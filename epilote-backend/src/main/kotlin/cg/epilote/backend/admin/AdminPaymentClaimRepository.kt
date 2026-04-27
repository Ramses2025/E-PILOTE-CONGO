package cg.epilote.backend.admin

import com.couchbase.client.core.error.DocumentExistsException
import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Repository

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
 * Cycle de vie d'un claim :
 *   1. `claim(key)` :
 *      - première fois → renvoie [ClaimOutcome.Acquired], le caller exécute la logique
 *      - retry concurrent → renvoie [ClaimOutcome.AlreadyDone] si un précédent appel
 *        a complété (avec `receiptId` caché), ou [ClaimOutcome.InProgress] si la
 *        première exécution est encore en cours, ou [ClaimOutcome.PreviouslyFailed]
 *        si un précédent appel s'est terminé en échec — auquel cas on accepte une
 *        nouvelle tentative (relâche le claim et ré-acquiert)
 *   2. `markDone(key, receiptId)` après succès → cache la réponse
 *   3. `markFailed(key, error)` après échec → permet un retry humain plus tard
 *
 * **Pourquoi pas un check N1QL** : la réplication des index N1QL est *eventually
 * consistent* (un retry survenant 100 ms après le premier appel pouvait ne pas voir
 * le doc du premier). KV est strongly-consistent : la réponse de `insert()` est
 * définitive dès qu'elle revient.
 */
@Repository
class AdminPaymentClaimRepository(private val bucket: Bucket) {

    private val scope by lazy { runBlocking { bucket.defaultScope() } }

    private companion object {
        const val COLLECTION = "payment_receipts"
        const val DOC_TYPE = "payment_idempotency"
        const val DOC_PREFIX = "payment_claim::"
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_DONE = "done"
        const val STATUS_FAILED = "failed"
    }

    private fun col(): Collection = runBlocking { scope.collection(COLLECTION) }

    private fun docId(idempotencyKey: String): String = "$DOC_PREFIX$idempotencyKey"

    /**
     * Tente d'acquérir un claim pour la clé donnée. Strongly-consistent.
     */
    suspend fun claim(idempotencyKey: String): ClaimOutcome {
        val id = docId(idempotencyKey)
        val now = System.currentTimeMillis()
        val newClaim = mapOf(
            "type" to DOC_TYPE,
            "idempotencyKey" to idempotencyKey,
            "status" to STATUS_IN_PROGRESS,
            "receiptId" to null,
            "errorMessage" to null,
            "createdAt" to now,
            "updatedAt" to now
        )

        return try {
            // KV insert : strongly-consistent côté cluster.
            col().insert(id, newClaim)
            ClaimOutcome.Acquired
        } catch (e: DocumentExistsException) {
            // Un autre appel a déjà posé un claim pour cette clé. On lit son état
            // pour décider quoi faire.
            val existing = runCatching { col().get(id).contentAs<Map<String, Any?>>() }
                .getOrNull()
                ?: return ClaimOutcome.InProgress
            val status = existing["status"] as? String ?: STATUS_IN_PROGRESS
            val receiptId = existing["receiptId"] as? String
            val errorMessage = existing["errorMessage"] as? String

            when (status) {
                STATUS_DONE -> ClaimOutcome.AlreadyDone(receiptId)
                STATUS_FAILED -> {
                    // Un précédent essai a échoué : on libère et réessaye.
                    runCatching { col().remove(id) }
                    try {
                        col().insert(id, newClaim)
                        ClaimOutcome.Acquired
                    } catch (_: DocumentExistsException) {
                        // Race avec un autre retry : on retombe en InProgress.
                        ClaimOutcome.InProgress
                    }
                }
                else -> ClaimOutcome.InProgress
            }.let {
                if (it is ClaimOutcome.PreviouslyFailed) it.copy(errorMessage = errorMessage) else it
            }
        }
    }

    /**
     * Marque le claim comme terminé avec succès et cache la réponse pour les retries.
     */
    suspend fun markDone(idempotencyKey: String, receiptId: String) {
        val id = docId(idempotencyKey)
        val now = System.currentTimeMillis()
        val existing = runCatching { col().get(id).contentAs<MutableMap<String, Any?>>() }
            .getOrNull() ?: return
        existing["status"] = STATUS_DONE
        existing["receiptId"] = receiptId
        existing["updatedAt"] = now
        runCatching { col().upsert(id, existing) }
    }

    /**
     * Marque le claim comme échoué — un retry pourra reprendre depuis zéro.
     */
    suspend fun markFailed(idempotencyKey: String, errorMessage: String?) {
        val id = docId(idempotencyKey)
        val now = System.currentTimeMillis()
        val existing = runCatching { col().get(id).contentAs<MutableMap<String, Any?>>() }
            .getOrNull() ?: return
        existing["status"] = STATUS_FAILED
        existing["errorMessage"] = errorMessage
        existing["updatedAt"] = now
        runCatching { col().upsert(id, existing) }
    }
}

sealed class ClaimOutcome {
    /** Première fois qu'on voit cette clé : exécuter la logique métier. */
    data object Acquired : ClaimOutcome()

    /** Un appel précédent a déjà réussi : retourner le receiptId caché. */
    data class AlreadyDone(val receiptId: String?) : ClaimOutcome()

    /** Un appel concurrent est en cours : retourner 409 Conflict. */
    data object InProgress : ClaimOutcome()

    /** Le précédent essai a échoué (rare — interception interne, normalement transformé en Acquired). */
    data class PreviouslyFailed(val errorMessage: String?) : ClaimOutcome()
}
