package cg.epilote.backend.admin

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
