package cg.epilote.backend.admin

import com.couchbase.client.core.error.CasMismatchException
import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.query.QueryParameters
import com.couchbase.client.kotlin.query.execute
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class AdminCommunicationRepository(
    private val bucket: Bucket
) {
    private val scope = runBlocking { bucket.defaultScope() }

    private companion object {
        const val ANNOUNCEMENTS_COLLECTION = "announcements"
        const val MESSAGES_COLLECTION = "messages"
        const val ANNOUNCEMENT_TYPE = "platform_announcement"
        const val MESSAGE_TYPE = "platform_message"
    }

    private val announcementsCol: Collection = runBlocking { scope.collection(ANNOUNCEMENTS_COLLECTION) }
    private val messagesCol: Collection = runBlocking { scope.collection(MESSAGES_COLLECTION) }
    private fun newId(prefix: String): String = "$prefix::${UUID.randomUUID()}"
    private fun now(): Long = System.currentTimeMillis()

    suspend fun createAnnouncement(req: CreateAnnouncementRequest, createdBy: String): AnnouncementResponse {
        val currentTime = now()
        val id = newId("ann")
        val doc = mapOf(
            "type" to ANNOUNCEMENT_TYPE,
            "titre" to req.titre.trim(),
            "contenu" to req.contenu.trim(),
            "cible" to req.cible.trim().ifBlank { "all" },
            "createdBy" to createdBy,
            "createdAt" to currentTime,
            "updatedAt" to currentTime
        )
        announcementsCol.upsert(id, doc)
        return AnnouncementResponse(
            id = id,
            titre = doc["titre"] as String,
            contenu = doc["contenu"] as String,
            cible = doc["cible"] as String,
            createdBy = createdBy,
            createdAt = currentTime
        )
    }

    suspend fun listAnnouncements(page: Int = 1, pageSize: Int = 50): List<AnnouncementResponse> {
        val safePage = page.coerceAtLeast(1)
        val safeSize = pageSize.coerceIn(1, 200)
        val offset = (safePage - 1) * safeSize
        val result = scope.query(
            statement = "SELECT META(a).id AS id, a.* FROM `$ANNOUNCEMENTS_COLLECTION` a WHERE a.type = \$docType ORDER BY a.createdAt DESC LIMIT \$lim OFFSET \$off",
            parameters = QueryParameters.named("docType" to ANNOUNCEMENT_TYPE, "lim" to safeSize, "off" to offset)
        ).execute()
        return result.rows.map { row ->
            val data = row.contentAs<Map<String, Any?>>()
            AnnouncementResponse(
                id = data["id"] as? String ?: "",
                titre = data["titre"] as? String ?: "",
                contenu = data["contenu"] as? String ?: "",
                cible = data["cible"] as? String ?: "all",
                createdBy = data["createdBy"] as? String ?: "",
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    suspend fun createMessage(req: CreateAdminMessageRequest, createdBy: String): AdminMessageResponse? {
        val normalizedTargetType = req.targetType.trim().lowercase()
        val normalizedGroupId = req.groupId?.trim()?.takeIf { it.isNotBlank() }
        val normalizedAdminId = req.adminId?.trim()?.takeIf { it.isNotBlank() }
        val targetIsValid = when (normalizedTargetType) {
            "all_groups", "all_admins" -> true
            "group" -> normalizedGroupId != null
            "admin" -> normalizedAdminId != null
            else -> false
        }
        if (!targetIsValid) return null

        val currentTime = now()
        val id = newId("msg")
        val threadKey = when (normalizedTargetType) {
            "group" -> "group::${normalizedGroupId}"
            "admin" -> "admin::${normalizedAdminId}"
            else -> normalizedTargetType
        }
        val doc = mapOf(
            "type" to MESSAGE_TYPE,
            "sujet" to req.sujet.trim(),
            "contenu" to req.contenu.trim(),
            "targetType" to normalizedTargetType,
            "groupId" to normalizedGroupId,
            "adminId" to normalizedAdminId,
            "threadKey" to threadKey,
            "status" to "sent",
            "readBy" to listOf(createdBy),
            "createdBy" to createdBy,
            "createdAt" to currentTime,
            "updatedAt" to currentTime
        )
        messagesCol.upsert(id, doc)
        return AdminMessageResponse(
            id = id,
            sujet = doc["sujet"] as String,
            contenu = doc["contenu"] as String,
            targetType = normalizedTargetType,
            groupId = normalizedGroupId,
            adminId = normalizedAdminId,
            threadKey = threadKey,
            status = "sent",
            readBy = listOf(createdBy),
            createdBy = createdBy,
            createdAt = currentTime
        )
    }

    suspend fun updateMessageStatus(messageId: String, status: String): AdminMessageResponse? {
        val normalizedStatus = status.trim().lowercase()
        if (normalizedStatus !in setOf("sent", "archived", "deleted")) return null

        repeat(3) {
            val getResult = runCatching {
                messagesCol.get(messageId)
            }.getOrNull() ?: return null
            val current = getResult.contentAs<MutableMap<String, Any?>>()
            if ((current["type"] as? String) != MESSAGE_TYPE) return null

            current["status"] = normalizedStatus
            current["updatedAt"] = now()
            try {
                messagesCol.replace(messageId, current, cas = getResult.cas)
            } catch (_: CasMismatchException) {
                return@repeat
            }

            @Suppress("UNCHECKED_CAST")
            return AdminMessageResponse(
                id = messageId,
                sujet = current["sujet"] as? String ?: "",
                contenu = current["contenu"] as? String ?: "",
                targetType = current["targetType"] as? String ?: "all_groups",
                groupId = current["groupId"] as? String,
                adminId = current["adminId"] as? String,
                threadKey = current["threadKey"] as? String ?: "",
                status = normalizedStatus,
                readBy = (current["readBy"] as? List<String>) ?: emptyList(),
                createdBy = current["createdBy"] as? String ?: "",
                createdAt = (current["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
        return null
    }

    suspend fun listMessages(page: Int = 1, pageSize: Int = 50): List<AdminMessageResponse> {
        val safePage = page.coerceAtLeast(1)
        val safeSize = pageSize.coerceIn(1, 200)
        val offset = (safePage - 1) * safeSize
        val result = scope.query(
            statement = "SELECT META(m).id AS id, m.* FROM `$MESSAGES_COLLECTION` m WHERE m.type = \$docType ORDER BY m.createdAt DESC LIMIT \$lim OFFSET \$off",
            parameters = QueryParameters.named("docType" to MESSAGE_TYPE, "lim" to safeSize, "off" to offset)
        ).execute()
        return result.rows.map { row ->
            val data = row.contentAs<Map<String, Any?>>()
            @Suppress("UNCHECKED_CAST")
            AdminMessageResponse(
                id = data["id"] as? String ?: "",
                sujet = data["sujet"] as? String ?: "",
                contenu = data["contenu"] as? String ?: "",
                targetType = data["targetType"] as? String ?: "all_groups",
                groupId = data["groupId"] as? String,
                adminId = data["adminId"] as? String,
                threadKey = data["threadKey"] as? String ?: "",
                status = data["status"] as? String ?: "sent",
                readBy = (data["readBy"] as? List<String>) ?: emptyList(),
                createdBy = data["createdBy"] as? String ?: "",
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    suspend fun createSubscriptionRequest(groupeId: String, groupeNom: String, requestType: String, requestMessage: String?, createdBy: String): Boolean {
        val allowedTypes = setOf("RENEWAL_REQUEST", "PLAN_CHANGE_REQUEST")
        val normalizedType = requestType.trim().uppercase()
        if (normalizedType !in allowedTypes) return false

        val typeLabel = if (normalizedType == "RENEWAL_REQUEST") "Renouvellement d'abonnement" else "Changement de plan"
        val sujet = "Demande de $typeLabel — $groupeNom"
        val contenu = buildString {
            append("Groupe : $groupeNom (ID: $groupeId)\n")
            append("Type de demande : $typeLabel\n")
            if (!requestMessage.isNullOrBlank()) {
                append("\nMessage de l'administrateur :\n${requestMessage.trim()}")
            }
        }
        val currentTime = now()
        val id = newId("sub_req")
        val doc = mapOf(
            "type" to MESSAGE_TYPE,
            "sujet" to sujet,
            "contenu" to contenu,
            "targetType" to "all_admins",
            "groupId" to groupeId,
            "adminId" to null,
            "threadKey" to "group::$groupeId",
            "status" to "sent",
            "readBy" to emptyList<String>(),
            "requestType" to normalizedType,
            "createdBy" to createdBy,
            "createdAt" to currentTime,
            "updatedAt" to currentTime
        )
        val docWithNom = doc.toMutableMap()
        docWithNom["groupeNom"] = groupeNom
        messagesCol.upsert(id, docWithNom)
        return true
    }

    suspend fun listSubscriptionRequests(statusFilter: String? = null, limit: Int = 200, offset: Int = 0): List<SubscriptionRequestInfo> {
        val safeLimit = limit.coerceIn(1, 500)
        val safeOffset = offset.coerceAtLeast(0)
        val stmt = buildString {
            append("SELECT META(m).id AS id, m.* FROM `$MESSAGES_COLLECTION` m ")
            append("WHERE m.`type` = \$docType ")
            append("AND m.requestType IN [\"RENEWAL_REQUEST\", \"PLAN_CHANGE_REQUEST\"] ")
            if (statusFilter != null) append("AND m.status = \$status ")
            append("ORDER BY m.createdAt DESC LIMIT \$lim OFFSET \$off")
        }
        val params = if (statusFilter != null)
            QueryParameters.named("docType" to MESSAGE_TYPE, "status" to statusFilter, "lim" to safeLimit, "off" to safeOffset)
        else
            QueryParameters.named("docType" to MESSAGE_TYPE, "lim" to safeLimit, "off" to safeOffset)
        return runCatching {
            scope.query(stmt, parameters = params).execute().rows.mapNotNull { row ->
                val d = row.contentAs<Map<String, Any?>>()
                val id = d["id"] as? String ?: return@mapNotNull null
                val rType = d["requestType"] as? String ?: return@mapNotNull null
                val typeLabel = if (rType == "RENEWAL_REQUEST") "Renouvellement d'abonnement" else "Changement de plan"
                val rawNom = d["groupeNom"] as? String
                    ?: (d["sujet"] as? String)?.substringAfter("— ")?.trim() ?: ""
                SubscriptionRequestInfo(
                    id            = id,
                    groupeId      = d["groupId"] as? String ?: "",
                    groupeNom     = rawNom,
                    requestType   = rType,
                    typeLabel     = typeLabel,
                    message       = d["contenu"] as? String,
                    status        = d["status"] as? String ?: "sent",
                    createdBy     = d["createdBy"] as? String ?: "",
                    createdAt     = (d["createdAt"] as? Number)?.toLong() ?: 0L,
                    resolvedBy    = d["resolvedBy"] as? String,
                    resolvedAt    = (d["resolvedAt"] as? Number)?.toLong(),
                    resolutionNotes = d["resolutionNotes"] as? String
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun resolveSubscriptionRequest(id: String, action: String, resolvedBy: String, notes: String?): Boolean {
        if (action !in setOf("approved", "rejected")) return false
        repeat(3) {
            val getResult = runCatching { messagesCol.get(id) }.getOrNull() ?: return false
            val doc = getResult.contentAs<MutableMap<String, Any?>>()
            if (doc["type"] != MESSAGE_TYPE) return false
            if (doc["requestType"] !in listOf("RENEWAL_REQUEST", "PLAN_CHANGE_REQUEST")) return false
            if (doc["status"] != "sent") return false
            val currentTime = now()
            doc["status"] = action
            doc["resolvedBy"] = resolvedBy
            doc["resolvedAt"] = currentTime
            if (!notes.isNullOrBlank()) doc["resolutionNotes"] = notes.trim()
            doc["updatedAt"] = currentTime
            return try {
                messagesCol.replace(id, doc, cas = getResult.cas)
                true
            } catch (_: CasMismatchException) {
                false.also { return@repeat }
            }
        }
        return false
    }

    suspend fun markMessageAsRead(messageId: String, userId: String): AdminMessageResponse? {
        repeat(3) {
            val getResult = runCatching {
                messagesCol.get(messageId)
            }.getOrNull() ?: return null
            val current = getResult.contentAs<MutableMap<String, Any?>>()
            if ((current["type"] as? String) != MESSAGE_TYPE) return null

            @Suppress("UNCHECKED_CAST")
            val readBy = ((current["readBy"] as? List<String>) ?: emptyList()).toMutableList()
            if (userId !in readBy) {
                readBy.add(userId)
                current["readBy"] = readBy
                current["updatedAt"] = now()
                try {
                    messagesCol.replace(messageId, current, cas = getResult.cas)
                } catch (_: CasMismatchException) {
                    return@repeat
                }
            }

            return AdminMessageResponse(
                id = messageId,
                sujet = current["sujet"] as? String ?: "",
                contenu = current["contenu"] as? String ?: "",
                targetType = current["targetType"] as? String ?: "all_groups",
                groupId = current["groupId"] as? String,
                adminId = current["adminId"] as? String,
                threadKey = current["threadKey"] as? String ?: "",
                status = current["status"] as? String ?: "sent",
                readBy = readBy,
                createdBy = current["createdBy"] as? String ?: "",
                createdAt = (current["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
        return null
    }
}
