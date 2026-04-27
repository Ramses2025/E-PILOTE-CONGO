package cg.epilote.backend.admin

import com.couchbase.client.core.error.CasMismatchException
import com.couchbase.client.core.error.DocumentNotFoundException
import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.query.execute
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Repository
class AdminSubscriptionRepository(
    private val bucket: Bucket,
    private val planRepo: AdminPlanRepository
) {
    private val scope by lazy { runBlocking { bucket.defaultScope() } }

    private companion object {
        const val GROUPS_COLLECTION = "school_groups"
        const val LEGACY_GROUP_TYPE = "groupe"
        const val GROUP_TYPE = "school_group"
        const val ONE_YEAR_MS = 365L * 86_400_000L
    }

    private fun col(name: String): Collection = runBlocking { scope.collection(name) }

    private fun now(): Long = System.currentTimeMillis()

    private fun parseTimestamp(raw: Any?): Long = when (raw) {
        is Number -> raw.toLong()
        is String -> runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
            ?: runCatching {
                LocalDate.parse(raw)
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
            ?: raw.toLongOrNull()
            ?: 0L
        else -> 0L
    }

    private fun formatDateIso(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.of("UTC"))
        .toLocalDate()
        .toString()

    @Suppress("UNCHECKED_CAST")
    private fun mutableMap(any: Any?): MutableMap<String, Any?> =
        (any as? Map<*, *>)
            ?.entries
            ?.associate { it.key.toString() to it.value }
            ?.toMutableMap()
            ?: mutableMapOf()

    private fun buildSubscriptionResponse(groupId: String, groupDoc: Map<String, Any?>): SubscriptionResponse? {
        val planId = groupDoc["planId"] as? String ?: return null
        if (planId.isBlank()) return null

        val subscription = mutableMap(groupDoc["subscription"])
        val createdAt = parseTimestamp(subscription["createdAt"] ?: groupDoc["createdAt"] ?: groupDoc["updatedAt"]).takeIf { it > 0 }
            ?: now()
        val dateDebut = parseTimestamp(subscription["dateDebut"] ?: subscription["startDate"]).takeIf { it > 0 } ?: createdAt
        val dateFin = parseTimestamp(subscription["dateFin"] ?: subscription["endDate"]).takeIf { it > 0 } ?: (dateDebut + ONE_YEAR_MS)
        val statut = ((subscription["statut"] ?: subscription["status"]) as? String)?.lowercase()?.ifBlank { null }
            ?: if ((groupDoc["isActive"] as? Boolean) != false) "active" else "suspended"
        val renouvellementAuto = subscription["renouvellementAuto"] as? Boolean
            ?: subscription["autoRenew"] as? Boolean
            ?: false
        val id = subscription["id"] as? String ?: "sub::$groupId"

        return SubscriptionResponse(
            id = id,
            groupeId = groupId,
            planId = planId,
            statut = statut,
            dateDebut = dateDebut,
            dateFin = dateFin,
            renouvellementAuto = renouvellementAuto,
            createdAt = createdAt
        )
    }

    private suspend fun listGroupDocs(): List<Pair<String, MutableMap<String, Any?>>> {
        val result = scope.query(
            "SELECT META().id AS id, * FROM `school_groups` WHERE `type` IN ['school_group', 'groupe']"
        ).execute()
        return result.rows.mapNotNull { row ->
            val doc = row.contentAs<Map<String, Any?>>()
            val id = doc["id"] as? String ?: return@mapNotNull null
            val inner = mutableMap(doc[GROUPS_COLLECTION] ?: doc)
            id to inner
        }
    }

    suspend fun listSubscriptions(): List<SubscriptionResponse> {
        return listGroupDocs()
            .mapNotNull { (groupId, doc) -> buildSubscriptionResponse(groupId, doc) }
            .sortedByDescending { it.createdAt }
    }

    suspend fun countSubscriptions(): Long = listSubscriptions().size.toLong()

    suspend fun countActiveSubscriptions(): Long {
        val currentTime = now()
        return listSubscriptions().count { it.statut == "active" && it.dateFin >= currentTime }.toLong()
    }

    suspend fun subscriptionsByStatus(): Map<String, Long> =
        listSubscriptions().groupingBy { it.statut }.eachCount().mapValues { it.value.toLong() }

    private suspend fun resolveGroupIdForSubscription(subscriptionId: String): String? {
        val directGroupId = subscriptionId.substringAfter("sub::", "")
        if (directGroupId.isNotBlank()) {
            val groupDoc = runCatching { col(GROUPS_COLLECTION).get(directGroupId).contentAs<MutableMap<String, Any?>>() }.getOrNull()
            if (groupDoc != null) {
                val subscription = mutableMap(groupDoc["subscription"])
                val currentId = subscription["id"] as? String ?: "sub::$directGroupId"
                if (currentId == subscriptionId || subscriptionId == directGroupId) {
                    return directGroupId
                }
            }
        }

        return listGroupDocs().firstNotNullOfOrNull { (groupId, doc) ->
            val subscription = mutableMap(doc["subscription"])
            val currentId = subscription["id"] as? String ?: "sub::$groupId"
            groupId.takeIf { currentId == subscriptionId || subscriptionId == groupId }
        }
    }

    suspend fun getSubscriptionById(id: String): SubscriptionResponse? {
        val groupId = resolveGroupIdForSubscription(id) ?: return null
        val groupDoc = runCatching { col(GROUPS_COLLECTION).get(groupId).contentAs<MutableMap<String, Any?>>() }.getOrNull() ?: return null
        return buildSubscriptionResponse(groupId, groupDoc)?.takeIf { it.id == id || id == groupId }
    }

    suspend fun getActiveSubscriptionByGroupe(groupeId: String): SubscriptionResponse? {
        val groupDoc = runCatching { col(GROUPS_COLLECTION).get(groupeId).contentAs<MutableMap<String, Any?>>() }.getOrNull() ?: return null
        val sub = buildSubscriptionResponse(groupeId, groupDoc) ?: return null
        return sub.takeIf { it.statut == "active" && it.dateFin >= now() }
    }

    suspend fun createSubscription(req: CreateSubscriptionRequest): SubscriptionResponse? {
        val groupDoc = runCatching { col(GROUPS_COLLECTION).get(req.groupeId).contentAs<MutableMap<String, Any?>>() }.getOrNull() ?: return null
        val plan = planRepo.getPlanById(req.planId) ?: return null
        val currentTime = now()
        val subscription = mutableMap(groupDoc["subscription"])
        val createdAt = parseTimestamp(subscription["createdAt"]).takeIf { it > 0 } ?: currentTime

        groupDoc["planId"] = plan.id
        groupDoc["subscription"] = mutableMapOf(
            "id" to (subscription["id"] as? String ?: "sub::${req.groupeId}"),
            "statut" to "active",
            "status" to "active",
            "dateDebut" to currentTime,
            "startDate" to formatDateIso(currentTime),
            "dateFin" to (currentTime + ONE_YEAR_MS),
            "endDate" to formatDateIso(currentTime + ONE_YEAR_MS),
            "renouvellementAuto" to req.renouvellementAuto,
            "autoRenew" to req.renouvellementAuto,
            "createdAt" to createdAt,
            "updatedAt" to currentTime
        )
        groupDoc["updatedAt"] = currentTime
        col(GROUPS_COLLECTION).upsert(req.groupeId, groupDoc)
        return buildSubscriptionResponse(req.groupeId, groupDoc)
    }

    /**
     * Active ou renouvelle un abonnement : fixe `dateDebut = maintenant` et
     * `dateFin = maintenant + durationMonths`. Met le statut à `active`. Utilisé quand
     * le Super Admin enregistre un paiement présentiel.
     */
    suspend fun activateOrRenew(subId: String, durationMonths: Int = 12): SubscriptionResponse? {
        val currentTime = now()
        val endTime = Instant.ofEpochMilli(currentTime)
            .atZone(ZoneId.of("UTC"))
            .plusMonths(durationMonths.toLong())
            .toInstant()
            .toEpochMilli()
        return applyPaidPeriod(subId, currentTime, endTime)
    }

    /**
     * Calcule la période d'accès pour un paiement présentiel sans persister.
     * Permet de pré-calculer `dateDebut` / `dateFin` côté contrôleur, créer la
     * facture et le reçu, puis n'appliquer la mutation sub qu'en dernier
     * via [applyPaidPeriod]. Empêche la double-extension d'abonnement en cas
     * d'échec mid-flow (cf. recordPayment).
     */
    fun computeRenewalPeriod(durationMonths: Int): Pair<Long, Long> {
        val currentTime = now()
        val endTime = Instant.ofEpochMilli(currentTime)
            .atZone(ZoneId.of("UTC"))
            .plusMonths(durationMonths.toLong())
            .toInstant()
            .toEpochMilli()
        return currentTime to endTime
    }

    /**
     * Applique une période d'accès pré-calculée à l'abonnement (étape finale
     * d'un recordPayment, après création de facture et reçu). Sépare la
     * computation de la persistance pour permettre une compensation
     * structurée en cas d'échec amont.
     */
    suspend fun applyPaidPeriod(subId: String, dateDebut: Long, dateFin: Long): SubscriptionResponse? {
        val groupId = resolveGroupIdForSubscription(subId) ?: return null
        val groupDoc = runCatching { col(GROUPS_COLLECTION).get(groupId).contentAs<MutableMap<String, Any?>>() }.getOrNull() ?: return null
        val planId = groupDoc["planId"] as? String ?: return null
        if (planId.isBlank()) return null

        val subscription = mutableMap(groupDoc["subscription"])
        val updatedAt = now()
        val createdAt = parseTimestamp(subscription["createdAt"] ?: groupDoc["createdAt"]).takeIf { it > 0 } ?: updatedAt

        groupDoc["subscription"] = mutableMapOf(
            "id" to (subscription["id"] as? String ?: "sub::$groupId"),
            "statut" to "active",
            "status" to "active",
            "dateDebut" to dateDebut,
            "startDate" to formatDateIso(dateDebut),
            "dateFin" to dateFin,
            "endDate" to formatDateIso(dateFin),
            "renouvellementAuto" to (subscription["renouvellementAuto"] as? Boolean ?: subscription["autoRenew"] as? Boolean ?: false),
            "autoRenew" to (subscription["renouvellementAuto"] as? Boolean ?: subscription["autoRenew"] as? Boolean ?: false),
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
        groupDoc["updatedAt"] = updatedAt
        col(GROUPS_COLLECTION).upsert(groupId, groupDoc)
        return buildSubscriptionResponse(groupId, groupDoc)
    }

    /**
     * Passe tous les abonnements arrivés à échéance en `suspended` (sauf ceux déjà
     * `cancelled`/`suspended`). Retourne la liste des identifiants de groupes suspendus.
     *
     * Invoqué automatiquement par [cg.epilote.backend.admin.SubscriptionExpiryScheduler]
     * chaque jour à 02:00 UTC (`@Scheduled(cron = ...)`).
     * Référence Spring Scheduling (Kotlin) :
     * https://docs.spring.io/spring-framework/reference/integration/scheduling.html
     */
    suspend fun suspendExpiredSubscriptions(): List<String> {
        val currentTime = now()
        // 1) N1QL sert UNIQUEMENT à repérer les candidats expirés. Les résultats peuvent
        //    être legèrement stale (eventual consistency entre l'index et le KV store).
        val candidateGroupIds = listGroupDocs().mapNotNull { (groupId, doc) ->
            val sub = buildSubscriptionResponse(groupId, doc) ?: return@mapNotNull null
            if (sub.statut == "active" && sub.dateFin in 1 until currentTime) groupId else null
        }

        // 2) Pour chaque candidat, relecture KV `get` (doc + CAS le plus récent) + re-check
        //    de l'expiration, puis `replace` avec le CAS lu (écriture optimiste). Si un autre
        //    writer (`activateOrRenew`, `updateSubscriptionStatus`) a modifié le doc entre le
        //    `get` et le `replace`, `CasMismatchException` est levée et on n'écrase PAS le
        //    renouvellement. Pattern officiel Couchbase Kotlin SDK :
        //    https://docs.couchbase.com/kotlin-sdk/current/howtos/kv-operations.html#cas
        val suspended = mutableListOf<String>()
        candidateGroupIds.forEach { groupId ->
            val fresh = runCatching { col(GROUPS_COLLECTION).get(groupId) }.getOrNull()
                ?: return@forEach
            val freshDoc = runCatching { fresh.contentAs<MutableMap<String, Any?>>() }.getOrNull()
                ?: return@forEach

            val sub = buildSubscriptionResponse(groupId, freshDoc) ?: return@forEach
            // Re-check : si l'abonnement a été renouvelé entre-temps (ou annulé), ne pas toucher.
            if (sub.statut != "active" || sub.dateFin <= 0 || sub.dateFin >= currentTime) return@forEach

            val subMap = mutableMap(freshDoc["subscription"])
            subMap["statut"] = "suspended"
            subMap["status"] = "suspended"
            subMap["updatedAt"] = currentTime
            freshDoc["subscription"] = subMap
            freshDoc["updatedAt"] = currentTime
            try {
                col(GROUPS_COLLECTION).replace(groupId, freshDoc, cas = fresh.cas)
                suspended += groupId
            } catch (_: CasMismatchException) {
                // Un autre writer (paiement / renouvellement) a modifié le doc entre le
                // `get` et le `replace` — on ne suspend pas ce cycle, prochain run du
                // scheduler réévaluera.
            } catch (_: DocumentNotFoundException) {
                // Doc supprimé entre-temps, rien à faire.
            }
        }
        return suspended
    }

    suspend fun updateSubscriptionStatus(id: String, statut: String): SubscriptionResponse? {
        val normalizedStatus = statut.trim().lowercase()
        val groupId = resolveGroupIdForSubscription(id) ?: return null
        val groupDoc = runCatching { col(GROUPS_COLLECTION).get(groupId).contentAs<MutableMap<String, Any?>>() }.getOrNull() ?: return null
        val planId = groupDoc["planId"] as? String ?: return null
        if (planId.isBlank()) return null

        val currentTime = now()
        val subscription = mutableMap(groupDoc["subscription"])
        val createdAt = parseTimestamp(subscription["createdAt"] ?: groupDoc["createdAt"]).takeIf { it > 0 } ?: currentTime
        val dateDebut = parseTimestamp(subscription["dateDebut"] ?: subscription["startDate"]).takeIf { it > 0 } ?: createdAt
        val dateFin = parseTimestamp(subscription["dateFin"] ?: subscription["endDate"]).takeIf { it > 0 } ?: (dateDebut + ONE_YEAR_MS)

        groupDoc["subscription"] = mutableMapOf(
            "id" to (subscription["id"] as? String ?: "sub::$groupId"),
            "statut" to normalizedStatus,
            "status" to normalizedStatus,
            "dateDebut" to dateDebut,
            "startDate" to formatDateIso(dateDebut),
            "dateFin" to dateFin,
            "endDate" to formatDateIso(dateFin),
            "renouvellementAuto" to (subscription["renouvellementAuto"] as? Boolean ?: subscription["autoRenew"] as? Boolean ?: false),
            "autoRenew" to (subscription["renouvellementAuto"] as? Boolean ?: subscription["autoRenew"] as? Boolean ?: false),
            "createdAt" to createdAt,
            "updatedAt" to currentTime
        )
        groupDoc["updatedAt"] = currentTime
        col(GROUPS_COLLECTION).upsert(groupId, groupDoc)
        return buildSubscriptionResponse(groupId, groupDoc)
    }
}
