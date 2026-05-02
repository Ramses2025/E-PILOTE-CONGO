package cg.epilote.backend.admin

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

        val getResult = runCatching {
            messagesCol.get(messageId)
        }.getOrNull() ?: return null
        val current = getResult.contentAs<MutableMap<String, Any?>>()
        if ((current["type"] as? String) != MESSAGE_TYPE) return null

        val updatedAt = now()
        current["status"] = normalizedStatus
        current["updatedAt"] = updatedAt
        messagesCol.replace(messageId, current, cas = getResult.cas)

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

    suspend fun markMessageAsRead(messageId: String, userId: String): AdminMessageResponse? {
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
            messagesCol.replace(messageId, current, cas = getResult.cas)
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
}
