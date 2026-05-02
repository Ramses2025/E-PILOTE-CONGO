package cg.epilote.backend.admin

import com.couchbase.client.core.error.CasMismatchException
import com.couchbase.client.core.error.DocumentExistsException
import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Idempotence transactionnelle des paiements présentiels (`recordPayment`).
 *
 * Le repository pose un *claim doc* déterministe (`payment_claim::<idempotencyKey>`)
 * dans la collection `payment_receipts` **avant** toute mutation métier. L'opération
 * Couchbase KV `insert()` est strongly-consistent côté cluster : si deux requêtes
 * concurrentes utilisent la même clé, une seule réussit, l'autre reçoit
 * [DocumentExistsException] et peut alors lire le claim existant pour retourner
 * la réponse précédemment cachée.
 *
 * Référence officielle Couchbase Kotlin SDK :
 *   https://docs.couchbase.com/kotlin-sdk/current/howtos/kv-operations.html#insert
 *
 * Cycle de vie d'un claim (sémantique Stripe — clé d'idempotence terminale) :
 *   1. `claim(key)` :
 *      - première fois → renvoie [ClaimOutcome.Acquired], le caller exécute la logique
 *      - retry concurrent → renvoie [ClaimOutcome.AlreadyDone] si un précédent appel
 *        a complété (avec `receiptId` caché), ou [ClaimOutcome.InProgress] si la
 *        première exécution est encore en cours, ou [ClaimOutcome.PreviouslyFailed]
 *        si un précédent appel s'est terminé en échec — auquel cas le client doit
 *        générer une **nouvelle clé** pour retenter (on NE re-exécute PAS la logique
 *        métier, ce qui éliminerait l'idempotence et causerait des doubles débits /
 *        doubles extensions d'abonnement si la première exécution avait partiellement
 *        muté l'état avant l'erreur).
 *   2. `markDone(key, receiptId)` après succès → cache la réponse
 *   3. `markFailed(key, error)` après échec → l'erreur cachée est rejouée tel quel
 *      à toute tentative future avec la même clé
 *
 * Référence : https://docs.stripe.com/api/idempotent_requests
 *  > "All POST requests accept idempotency keys. ... If the same idempotency key is
 *  > used after the request has fully completed, the same response is returned."
 *
 * **Pourquoi pas un check N1QL** : la réplication des index N1QL est *eventually
 * consistent* (un retry survenant 100 ms après le premier appel pouvait ne pas voir
 * le doc du premier). KV est strongly-consistent : la réponse de `insert()` est
 * définitive dès qu'elle revient.
 */
@Repository
class AdminPaymentClaimRepository(private val bucket: Bucket) {

    private val scope = runBlocking { bucket.defaultScope() }

    private companion object {
        const val COLLECTION = "payment_receipts"
        const val DOC_TYPE = "payment_idempotency"
        const val DOC_PREFIX = "payment_claim::"
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_DONE = "done"
        const val STATUS_FAILED = "failed"
        /**
         * Au-delà de cette durée, un claim resté `in_progress` est considéré comme
         * orphelin (markDone/markFailed n'a pas pu s'exécuter — crash, network split,
         * etc.). On le libère pour permettre une retry.
         *
         * Couvre largement la latence d'un POST `recordPayment` (en pratique <2 s).
         */
        const val STALE_IN_PROGRESS_MS = 5 * 60 * 1000L
    }

    private val col: Collection = runBlocking { scope.collection(COLLECTION) }

    private fun docId(idempotencyKey: String): String = "$DOC_PREFIX$idempotencyKey"

    /**
     * Tente d'acquérir un claim pour la clé donnée. Strongly-consistent.
     */
    suspend fun claim(idempotencyKey: String): ClaimOutcome {
        val id = docId(idempotencyKey)
        val now = System.currentTimeMillis()
        val claimToken = UUID.randomUUID().toString()
        val newClaim = mapOf(
            "type" to DOC_TYPE,
            "idempotencyKey" to idempotencyKey,
            "status" to STATUS_IN_PROGRESS,
            "claimToken" to claimToken,
            "receiptId" to null,
            "errorMessage" to null,
            "createdAt" to now,
            "updatedAt" to now
        )

        return try {
            // KV insert : strongly-consistent côté cluster.
            col.insert(id, newClaim)
            ClaimOutcome.Acquired(claimToken)
        } catch (e: DocumentExistsException) {
            // Un autre appel a déjà posé un claim pour cette clé. On lit son état
            // (avec son CAS) pour décider quoi faire.
            val getResult = runCatching { col.get(id) }.getOrNull()
                ?: return ClaimOutcome.InProgress
            val existing = runCatching { getResult.contentAs<Map<String, Any?>>() }
                .getOrNull()
                ?: return ClaimOutcome.InProgress
            val status = existing["status"] as? String ?: STATUS_IN_PROGRESS
            val receiptId = existing["receiptId"] as? String
            val errorMessage = existing["errorMessage"] as? String
            val updatedAt = (existing["updatedAt"] as? Number)?.toLong() ?: 0L

            when (status) {
                STATUS_DONE -> ClaimOutcome.AlreadyDone(receiptId)
                STATUS_FAILED -> ClaimOutcome.PreviouslyFailed(errorMessage)
                STATUS_IN_PROGRESS -> {
                    // Staleness : si le claim est `in_progress` depuis plus de
                    // STALE_IN_PROGRESS_MS, le caller précédent a probablement
                    // crashé entre l'insert et le markDone/markFailed. On réacquiert
                    // **avec CAS** plutôt que remove+insert (atomicité garantie
                    // par le SDK Couchbase : un seul caller concurrent réussira).
                    // https://docs.couchbase.com/kotlin-sdk/current/howtos/kv-operations.html#cas
                    val ageMs = System.currentTimeMillis() - updatedAt
                    if (ageMs > STALE_IN_PROGRESS_MS) {
                        reacquireWithCas(id, getResult.cas, newClaim, claimToken)
                    } else {
                        ClaimOutcome.InProgress
                    }
                }
                else -> ClaimOutcome.InProgress
            }
        }
    }

    /**
     * Réacquiert un claim stale en remplaçant **atomiquement** son contenu via
     * CAS (compare-and-swap). Si deux callers concurrents observent le même claim
     * stale et tentent de réacquérir, exactement un voit son `replace` réussir ;
     * l'autre reçoit [CasMismatchException] et retombe en `InProgress`. Cela
     * élimine la TOCTOU race d'un précédent `remove + insert` non-atomique.
     *
     * Pattern officiel Couchbase Kotlin SDK :
     *   https://docs.couchbase.com/kotlin-sdk/current/howtos/kv-operations.html#cas
     */
    private suspend fun reacquireWithCas(
        id: String,
        cas: Long,
        newClaim: Map<String, Any?>,
        claimToken: String
    ): ClaimOutcome {
        return try {
            col.replace(id, newClaim, cas = cas)
            ClaimOutcome.Acquired(claimToken)
        } catch (_: CasMismatchException) {
            // Un autre retry vient de gagner la course de réacquisition.
            ClaimOutcome.InProgress
        }
    }

    /**
     * Marque le claim comme terminé avec succès et cache la réponse pour les retries.
     *
     * Vérifie que le `claimToken` du doc actuel correspond à celui obtenu lors de
     * l'acquisition : si un autre caller a réacquis le claim entre-temps (cas
     * stale-in-progress), on n'écrase PAS son état avec le nôtre. Pattern
     * officiel Couchbase Kotlin SDK (replace + CAS) :
     *   https://docs.couchbase.com/kotlin-sdk/current/howtos/kv-operations.html#cas
     */
    suspend fun markDone(idempotencyKey: String, claimToken: String, receiptId: String) {
        updateClaimIfOwner(idempotencyKey, claimToken) { existing ->
            existing["status"] = STATUS_DONE
            existing["receiptId"] = receiptId
        }
    }

    /**
     * Marque le claim comme échoué — l'erreur cachée sera rejouée à toute
     * tentative future avec la même clé (sémantique Stripe terminale).
     *
     * Comme [markDone], protégé par CAS + vérification du `claimToken` pour ne pas
     * écraser un claim qu'un autre caller aurait réacquis après staleness.
     */
    suspend fun markFailed(idempotencyKey: String, claimToken: String, errorMessage: String?) {
        updateClaimIfOwner(idempotencyKey, claimToken) { existing ->
            existing["status"] = STATUS_FAILED
            existing["errorMessage"] = errorMessage
        }
    }

    /**
     * Helper : applique [mutate] au doc claim **uniquement** si son `claimToken`
     * correspond à celui passé en paramètre, et le réécrit avec CAS-replace pour
     * garantir l'atomicité. Si un autre writer a touché le doc entre `get` et
     * `replace` ([CasMismatchException]) ou si le `claimToken` ne correspond
     * plus, on abandonne silencieusement (un autre caller possède désormais
     * le claim et c'est sa réponse qui doit être cachée).
     */
    private suspend fun updateClaimIfOwner(
        idempotencyKey: String,
        claimToken: String,
        mutate: (MutableMap<String, Any?>) -> Unit
    ) {
        val id = docId(idempotencyKey)
        val getResult = runCatching { col.get(id) }.getOrNull() ?: return
        val existing = runCatching { getResult.contentAs<MutableMap<String, Any?>>() }
            .getOrNull() ?: return
        val currentToken = existing["claimToken"] as? String
        if (currentToken != claimToken) {
            // Un autre caller a réacquis le claim après staleness — ne pas écraser.
            return
        }
        mutate(existing)
        existing["updatedAt"] = System.currentTimeMillis()
        runCatching { col.replace(id, existing, cas = getResult.cas) }
    }
}

sealed class ClaimOutcome {
    /**
     * Première fois qu'on voit cette clé : exécuter la logique métier. Le
     * [claimToken] doit être passé à [AdminPaymentClaimRepository.markDone] /
     * [AdminPaymentClaimRepository.markFailed] pour que ces appels puissent
     * vérifier qu'aucun autre caller n'a réacquis le claim entre-temps.
     */
    data class Acquired(val claimToken: String) : ClaimOutcome()

    /** Un appel précédent a déjà réussi : retourner le receiptId caché. */
    data class AlreadyDone(val receiptId: String?) : ClaimOutcome()

    /** Un appel concurrent est en cours : retourner 409 Conflict. */
    data object InProgress : ClaimOutcome()

    /**
     * Le précédent essai avec cette clé d'idempotence a échoué. Sémantique Stripe :
     * la clé est **terminale** — toute tentative future avec la même clé renvoie la
     * même erreur, sans réexécution métier. Le client doit générer une nouvelle clé
     * pour retenter, garantissant qu'aucune mutation partielle n'est dupliquée.
     */
    data class PreviouslyFailed(val errorMessage: String?) : ClaimOutcome()
}
