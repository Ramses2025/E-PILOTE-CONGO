package cg.epilote.desktop.ui.screens

import cg.epilote.desktop.data.AnnouncementApiDto
import cg.epilote.desktop.data.AdminMessageApiDto

enum class AdminMessagingMailbox(val key: String, val label: String) {
    INBOX("inbox", "Boîte de réception"),
    SENT("sent", "Boîte d'envoi"),
    ANNOUNCEMENTS("announcements", "Annonces officielles"),
    ARCHIVED("archived", "Archives"),
    TRASH("trash", "Corbeille");

    companion object {
        fun fromKey(key: String): AdminMessagingMailbox = entries.firstOrNull { it.key == key } ?: INBOX
    }
}

internal data class AdminMessageThread(
    val threadKey: String,
    val targetType: String,
    val targetLabel: String,
    val lastSubject: String,
    val lastPreview: String,
    val lastCreatedAt: Long,
    val messageCount: Int,
    val messages: List<AdminMessageApiDto>
)

internal fun buildMessageThreads(
    messages: List<AdminMessageApiDto>,
    groupes: List<GroupeDto>,
    admins: List<AdminUserDto>
): List<AdminMessageThread> {
    val groupsById = groupes.associateBy { it.id }
    val adminsById = admins.associateBy { it.id }
    return messages
        .groupBy { it.threadKey.ifBlank { defaultThreadKey(it) } }
        .map { (threadKey, threadMessages) ->
            val sortedMessages = threadMessages.sortedByDescending { it.createdAt }
            val latest = sortedMessages.first()
            AdminMessageThread(
                threadKey = threadKey,
                targetType = latest.targetType,
                targetLabel = messageTargetLabel(latest, groupsById, adminsById),
                lastSubject = latest.sujet,
                lastPreview = latest.contenu,
                lastCreatedAt = latest.createdAt,
                messageCount = threadMessages.size,
                messages = threadMessages.sortedBy { it.createdAt }
            )
        }
        .sortedByDescending { it.lastCreatedAt }
}

internal fun AdminMessageThread.latestMessage(): AdminMessageApiDto =
    messages.maxByOrNull { it.createdAt } ?: messages.last()

internal fun AdminMessageThread.currentStatus(): String = latestMessage().status.ifBlank { "sent" }

internal fun AdminMessageThread.isOwnedBy(userId: String): Boolean =
    latestMessage().createdBy == userId

internal fun AdminMessageThread.matchesMailbox(mailbox: AdminMessagingMailbox, userId: String): Boolean = when (mailbox) {
    AdminMessagingMailbox.INBOX -> currentStatus() == "sent" && !isOwnedBy(userId)
    AdminMessagingMailbox.SENT -> currentStatus() == "sent" && isOwnedBy(userId)
    AdminMessagingMailbox.ANNOUNCEMENTS -> false
    AdminMessagingMailbox.ARCHIVED -> currentStatus() == "archived"
    AdminMessagingMailbox.TRASH -> currentStatus() == "deleted"
}

internal fun filterAnnouncements(
    announcements: List<AnnouncementApiDto>,
    searchQuery: String,
    targetFilter: String
): List<AnnouncementApiDto> = announcements.filter { item ->
    val matchesSearch = searchQuery.isBlank() ||
        item.titre.contains(searchQuery, true) ||
        item.contenu.contains(searchQuery, true) ||
        item.createdBy.contains(searchQuery, true)
    val matchesTarget = targetFilter == "all" || item.cible == targetFilter
    matchesSearch && matchesTarget
}

internal fun filterThreads(
    threads: List<AdminMessageThread>,
    searchQuery: String,
    targetFilter: String,
    mailbox: AdminMessagingMailbox,
    currentUserId: String
): List<AdminMessageThread> = threads.filter { thread ->
    val matchesSearch = searchQuery.isBlank() ||
        thread.targetLabel.contains(searchQuery, true) ||
        thread.lastSubject.contains(searchQuery, true) ||
        thread.lastPreview.contains(searchQuery, true)
    val matchesTarget = targetFilter == "all" || thread.targetType == targetFilter
    matchesSearch && matchesTarget && thread.matchesMailbox(mailbox, currentUserId)
}

internal fun messageTargetLabel(
    message: AdminMessageApiDto,
    groupsById: Map<String, GroupeDto>,
    adminsById: Map<String, AdminUserDto>
): String = when (message.targetType) {
    "group" -> message.groupId?.let { groupId -> groupsById[groupId]?.nom ?: groupId } ?: "Groupe inconnu"
    "admin" -> message.adminId?.let { adminId -> adminsById[adminId]?.let(::adminDisplayName) ?: adminId } ?: "Administrateur inconnu"
    "all_admins" -> "Tous les administrateurs"
    else -> "Tous les groupes scolaires"
}

internal fun adminDisplayName(admin: AdminUserDto): String =
    listOf(admin.firstName, admin.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { admin.email.ifBlank { admin.username } }

internal fun defaultThreadKey(message: AdminMessageApiDto): String = when (message.targetType) {
    "group" -> "group::${message.groupId.orEmpty()}"
    "admin" -> "admin::${message.adminId.orEmpty()}"
    "all_admins" -> "all_admins"
    else -> "all_groups"
}
