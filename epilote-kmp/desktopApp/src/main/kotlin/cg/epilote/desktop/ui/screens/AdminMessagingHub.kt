package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.AdminMessageApiDto
import cg.epilote.desktop.data.AnnouncementApiDto
import cg.epilote.desktop.data.CreateAdminMessageDto
import cg.epilote.desktop.data.CreateAnnouncementDto
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.shared.domain.model.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
internal fun AdminMessagingHub(
    session: UserSession,
    groupes: List<GroupeDto>,
    admins: List<AdminUserDto>,
    isLoading: Boolean,
    scope: CoroutineScope,
    client: DesktopAdminClient,
    onRefresh: () -> Unit,
    initialMailbox: AdminMessagingMailbox,
    sseReloadTick: StateFlow<Int>? = null
) {
    val scrollState = rememberScrollState()
    var mailbox by remember(initialMailbox) { mutableStateOf(initialMailbox) }
    var searchQuery by remember { mutableStateOf("") }
    var targetFilter by remember { mutableStateOf("all") }
    var messages by remember { mutableStateOf<List<AdminMessageApiDto>>(emptyList()) }
    var announcements by remember { mutableStateOf<List<AnnouncementApiDto>>(emptyList()) }
    var pageLoading by remember { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var actionFeedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }
    var selectedThread by remember { mutableStateOf<AdminMessageThread?>(null) }
    var selectedAnnouncement by remember { mutableStateOf<AnnouncementApiDto?>(null) }
    var showMessageDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var composeTargetType by remember { mutableStateOf("all_groups") }
    var composeGroupId by remember { mutableStateOf<String?>(null) }
    var composeAdminId by remember { mutableStateOf<String?>(null) }
    var composeSubject by remember { mutableStateOf("") }
    var composeContent by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var pendingThreadAction by remember { mutableStateOf<Pair<AdminMessageThread, String>?>(null) }
    var isThreadActionSubmitting by remember { mutableStateOf(false) }
    var threadActionError by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }

    val targetOptions = listOf(
        "all" to "Toutes les cibles",
        "all_groups" to "Diffusions groupes",
        "group" to "Un groupe scolaire",
        "all_admins" to "Diffusions admins",
        "admin" to "Un administrateur",
        "groupes" to "Annonces groupes",
        "admins" to "Annonces administrateurs",
        "billing" to "Annonces facturation"
    )

    suspend fun refreshCommunications() {
        pageLoading = true
        pageError = null
        val issues = mutableListOf<String>()
        val apiMessages = runCatching { client.listMessages() }.getOrNull().also {
            if (it == null) issues += "messagerie"
        }.orEmpty()
        val apiAnnouncements = runCatching { client.listAnnouncements() }.getOrNull().also {
            if (it == null) issues += "annonces"
        }.orEmpty()
        messages = apiMessages
        announcements = apiAnnouncements
        pageError = issues.takeIf { it.isNotEmpty() }?.joinToString(prefix = "Chargement partiel : ", separator = ", ")
        pageLoading = false
    }

    fun openNewMessage() {
        composeTargetType = "all_groups"
        composeGroupId = null
        composeAdminId = null
        composeSubject = ""
        composeContent = ""
        submitError = null
        showMessageDialog = true
    }

    fun openReply(thread: AdminMessageThread) {
        val latest = thread.latestMessage()
        composeTargetType = thread.targetType
        composeGroupId = latest.groupId
        composeAdminId = latest.adminId
        composeSubject = if (latest.sujet.startsWith("Re:", ignoreCase = true)) latest.sujet else "Re: ${latest.sujet}"
        composeContent = "\n\n---\n${latest.contenu}"
        submitError = null
        showMessageDialog = true
    }

    suspend fun updateThreadStatus(thread: AdminMessageThread, status: String): Boolean {
        val results = thread.messages.map { message ->
            runCatching { client.updateMessageStatus(message.id, status) }.getOrNull()
        }
        return results.all { it != null }
    }

    LaunchedEffect(sseReloadTick) {
        sseReloadTick?.drop(1)?.collect { reloadTick++ }
    }

    LaunchedEffect(reloadTick) {
        refreshCommunications()
    }

    AdminAutoRefreshEffect(refreshKey = reloadTick) {
        refreshCommunications()
    }

    val threads = remember(messages, groupes, admins) {
        buildMessageThreads(messages, groupes, admins)
    }
    val filteredThreads = remember(threads, searchQuery, targetFilter, mailbox, session.userId) {
        filterThreads(threads, searchQuery, targetFilter, mailbox, session.userId)
    }
    val filteredAnnouncements = remember(announcements, searchQuery, targetFilter) {
        filterAnnouncements(announcements, searchQuery, targetFilter)
    }
    val inboxAnnouncements = remember(filteredAnnouncements, mailbox) {
        if (mailbox == AdminMessagingMailbox.INBOX || mailbox == AdminMessagingMailbox.ANNOUNCEMENTS) filteredAnnouncements else emptyList()
    }

    val inboxCount = announcements.size + threads.count { it.matchesMailbox(AdminMessagingMailbox.INBOX, session.userId) }
    val sentCount = threads.count { it.matchesMailbox(AdminMessagingMailbox.SENT, session.userId) }
    val trashCount = threads.count { it.matchesMailbox(AdminMessagingMailbox.TRASH, session.userId) }
    val totalCommunications = announcements.size + threads.size
    val visibleCount = when (mailbox) {
        AdminMessagingMailbox.ANNOUNCEMENTS -> filteredAnnouncements.size
        AdminMessagingMailbox.INBOX -> filteredThreads.size + inboxAnnouncements.size
        else -> filteredThreads.size
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Messagerie", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Hub unique des communications super-admin : réception, envois, annonces officielles, archivage et corbeille.", fontSize = 13.sp, color = EpiloteTextMuted)
        }

        actionFeedback?.let { feedback ->
            AdminFeedbackBanner(feedback = feedback, onDismiss = { actionFeedback = null })
        }

        pageError?.let {
            AdminFeedbackBanner(feedback = AdminFeedbackMessage(it, isError = true), onDismiss = { pageError = null })
        }

        AdminMessagingKpiRow(
            totalCommunications = totalCommunications,
            inboxCount = inboxCount,
            sentCount = sentCount,
            trashCount = trashCount
        )

        AdminMessagingToolbar(
            mailbox = mailbox,
            onMailboxChange = { mailbox = it },
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            targetFilter = targetFilter,
            onTargetFilterChange = { targetFilter = it },
            targetOptions = targetOptions,
            totalResults = visibleCount,
            onRefresh = {
                onRefresh()
                reloadTick++
            },
            onComposeMessage = { openNewMessage() },
            onComposeAnnouncement = {
                submitError = null
                showAnnouncementDialog = true
            }
        )

        if (isLoading || pageLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        } else if (visibleCount == 0) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (mailbox == AdminMessagingMailbox.ANNOUNCEMENTS) Icons.Default.Campaign else Icons.Default.Forum, null, tint = EpiloteTextMuted)
                        Text(messageMailboxEmptyTitle(mailbox), fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
                        Text(messageMailboxEmptyMessage(mailbox), fontSize = 12.sp, color = EpiloteTextMuted)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (mailbox == AdminMessagingMailbox.INBOX || mailbox == AdminMessagingMailbox.ANNOUNCEMENTS) {
                    inboxAnnouncements.forEach { announcement ->
                        AdminMessagingAnnouncementCard(announcement = announcement, onOpen = { selectedAnnouncement = announcement })
                    }
                }
                if (mailbox != AdminMessagingMailbox.ANNOUNCEMENTS) {
                    filteredThreads.forEach { thread ->
                        AdminMessagingThreadCard(
                            thread = thread,
                            onOpen = { selectedThread = thread },
                            onReply = { openReply(thread) },
                            onArchive = if (mailbox != AdminMessagingMailbox.ARCHIVED && mailbox != AdminMessagingMailbox.TRASH) {
                                { pendingThreadAction = thread to "archived" }
                            } else null,
                            onDelete = if (mailbox != AdminMessagingMailbox.TRASH) {
                                { pendingThreadAction = thread to "deleted" }
                            } else null,
                            onRestore = if (mailbox == AdminMessagingMailbox.TRASH || mailbox == AdminMessagingMailbox.ARCHIVED) {
                                { pendingThreadAction = thread to "sent" }
                            } else null
                        )
                    }
                }
            }
        }
    }

    if (showMessageDialog) {
        AdminMessageComposeDialog(
            groupes = groupes,
            admins = admins,
            initialTargetType = composeTargetType,
            initialGroupId = composeGroupId,
            initialAdminId = composeAdminId,
            initialSubject = composeSubject,
            initialContent = composeContent,
            isSubmitting = isSubmitting,
            errorMessage = submitError,
            onDismiss = { showMessageDialog = false },
            onSubmit = { subject, content, targetType, groupId, adminId ->
                isSubmitting = true
                submitError = null
                scope.launch {
                    val result = runCatching { client.createMessage(CreateAdminMessageDto(subject, content, targetType, groupId, adminId)) }.getOrNull()
                    isSubmitting = false
                    if (result != null) {
                        showMessageDialog = false
                        actionFeedback = AdminFeedbackMessage("Message envoyé avec succès")
                        reloadTick++
                    } else {
                        submitError = "Impossible d'envoyer le message."
                    }
                }
            }
        )
    }

    if (showAnnouncementDialog) {
        AnnouncementComposeDialog(
            isSubmitting = isSubmitting,
            errorMessage = submitError,
            onDismiss = { showAnnouncementDialog = false },
            onSubmit = { title, content, target ->
                isSubmitting = true
                submitError = null
                scope.launch {
                    val result = runCatching { client.createAnnouncement(CreateAnnouncementDto(title, content, target)) }.getOrNull()
                    isSubmitting = false
                    if (result != null) {
                        showAnnouncementDialog = false
                        actionFeedback = AdminFeedbackMessage("Annonce publiée avec succès")
                        mailbox = AdminMessagingMailbox.ANNOUNCEMENTS
                        reloadTick++
                    } else {
                        submitError = "Impossible de publier l'annonce."
                    }
                }
            }
        )
    }

    pendingThreadAction?.let { (thread, nextStatus) ->
        AdminConfirmationDialog(
            title = messageThreadActionTitle(nextStatus),
            subtitle = thread.targetLabel,
            message = messageThreadActionMessage(nextStatus),
            confirmLabel = messageThreadActionTitle(nextStatus),
            onDismiss = {
                pendingThreadAction = null
                threadActionError = null
            },
            onConfirm = {
                isThreadActionSubmitting = true
                threadActionError = null
                scope.launch {
                    val success = updateThreadStatus(thread, nextStatus)
                    isThreadActionSubmitting = false
                    if (success) {
                        pendingThreadAction = null
                        actionFeedback = AdminFeedbackMessage(messageThreadActionSuccess(nextStatus))
                        reloadTick++
                    } else {
                        threadActionError = "Impossible de mettre à jour ce fil de discussion."
                    }
                }
            },
            confirmContainerColor = messageThreadActionColor(nextStatus),
            isSubmitting = isThreadActionSubmitting,
            errorMessage = threadActionError
        )
    }

    selectedThread?.let { thread ->
        AdminMessageThreadDialog(
            thread = thread,
            onDismiss = { selectedThread = null },
            onReply = {
                selectedThread = null
                openReply(thread)
            }
        )
    }

    selectedAnnouncement?.let { announcement ->
        AnnouncementDetailDialog(item = announcement, onDismiss = { selectedAnnouncement = null })
    }
}

private fun messageMailboxEmptyTitle(mailbox: AdminMessagingMailbox): String = when (mailbox) {
    AdminMessagingMailbox.INBOX -> "Boîte de réception vide"
    AdminMessagingMailbox.SENT -> "Aucun envoi"
    AdminMessagingMailbox.ANNOUNCEMENTS -> "Aucune annonce officielle"
    AdminMessagingMailbox.ARCHIVED -> "Aucune archive"
    AdminMessagingMailbox.TRASH -> "Corbeille vide"
}

private fun messageMailboxEmptyMessage(mailbox: AdminMessagingMailbox): String = when (mailbox) {
    AdminMessagingMailbox.INBOX -> "Les messages reçus et annonces officielles apparaîtront ici."
    AdminMessagingMailbox.SENT -> "Les messages envoyés par le super-admin apparaîtront ici."
    AdminMessagingMailbox.ANNOUNCEMENTS -> "Les annonces officielles publiées sur la plateforme apparaîtront ici."
    AdminMessagingMailbox.ARCHIVED -> "Les fils archivés seront conservés ici."
    AdminMessagingMailbox.TRASH -> "Les fils supprimés pourront être restaurés depuis cette corbeille."
}

private fun messageThreadActionTitle(status: String): String = when (status) {
    "archived" -> "Archiver le fil"
    "deleted" -> "Supprimer le fil"
    else -> "Restaurer le fil"
}

private fun messageThreadActionMessage(status: String): String = when (status) {
    "archived" -> "Le fil sera déplacé dans les archives et restera consultable."
    "deleted" -> "Le fil sera déplacé dans la corbeille. Vous pourrez encore le restaurer plus tard."
    else -> "Le fil sera rétabli dans la boîte d'envoi active."
}

private fun messageThreadActionSuccess(status: String): String = when (status) {
    "archived" -> "Fil archivé avec succès"
    "deleted" -> "Fil déplacé dans la corbeille"
    else -> "Fil restauré avec succès"
}

private fun messageThreadActionColor(status: String): Color = when (status) {
    "deleted" -> Color(0xFFD62828)
    "archived" -> Color(0xFF475569)
    else -> Color(0xFF1D3557)
}
