package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.History
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
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.data.InvoiceApiDto
import cg.epilote.desktop.data.SubscriptionApiDto
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted

@Composable
internal fun AdminAuditScreen(
    groupes: List<GroupeDto>,
    admins: List<AdminUserDto>,
    isLoading: Boolean,
    client: DesktopAdminClient,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("all") }
    var selectedSource by remember { mutableStateOf("all") }
    var selectedStatus by remember { mutableStateOf("all") }
    var subscriptions by remember { mutableStateOf<List<SubscriptionApiDto>>(emptyList()) }
    var invoices by remember { mutableStateOf<List<InvoiceApiDto>>(emptyList()) }
    var announcements by remember { mutableStateOf<List<AnnouncementApiDto>>(emptyList()) }
    var messages by remember { mutableStateOf<List<AdminMessageApiDto>>(emptyList()) }
    var pageLoading by remember { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var selectedEntry by remember { mutableStateOf<AdminAuditEntry?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }

    suspend fun refreshAudit() {
        pageLoading = true
        pageError = null
        val issues = mutableListOf<String>()
        subscriptions = runCatching { client.listSubscriptions() }.getOrNull().also { if (it == null) issues += "abonnements" }.orEmpty()
        invoices = runCatching { client.listInvoices() }.getOrNull().also { if (it == null) issues += "factures" }.orEmpty()
        announcements = runCatching { client.listAnnouncements() }.getOrNull().also { if (it == null) issues += "annonces" }.orEmpty()
        messages = runCatching { client.listMessages() }.getOrNull().also { if (it == null) issues += "messagerie" }.orEmpty()
        pageError = issues.takeIf { it.isNotEmpty() }?.joinToString(prefix = "Chargement partiel : ", separator = ", ")
        pageLoading = false
    }

    LaunchedEffect(groupes, admins, reloadTick) {
        refreshAudit()
    }

    AdminAutoRefreshEffect(refreshKey = reloadTick) {
        refreshAudit()
    }

    val auditEntries = remember(groupes, admins, subscriptions, invoices, announcements, messages) {
        buildAdminAuditEntries(groupes, admins, subscriptions, invoices, announcements, messages)
    }
    val filteredEntries = remember(auditEntries, searchQuery, selectedRole, selectedSource, selectedStatus) {
        auditEntries.filter { entry ->
            val matchesSearch = searchQuery.isBlank() ||
                entry.actor.contains(searchQuery, true) ||
                entry.action.contains(searchQuery, true) ||
                entry.details.contains(searchQuery, true) ||
                entry.entity.contains(searchQuery, true)
            val matchesRole = selectedRole == "all" || entry.role == selectedRole
            val matchesSource = selectedSource == "all" || entry.sourceType == selectedSource
            val matchesStatus = selectedStatus == "all" || entry.status == selectedStatus
            matchesSearch && matchesRole && matchesSource && matchesStatus
        }
    }

    val warningCount = auditEntries.count { it.status == "warning" }
    val errorCount = auditEntries.count { it.status == "error" }
    val securityCount = auditEntries.count { it.sourceType == "security" || it.sourceType == "access" }
    val financeCount = auditEntries.count { it.sourceType == "invoice" || it.sourceType == "subscription" }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Journal d'audit", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "Historique consolidé des opérations administratives, événements de facturation, accès et communications plateforme.",
                fontSize = 13.sp,
                color = EpiloteTextMuted
            )
        }

        pageError?.let {
            AdminFeedbackBanner(feedback = AdminFeedbackMessage(it, isError = true), onDismiss = { pageError = null })
        }

        AuditKpiRow(
            totalEntries = auditEntries.size,
            warningCount = warningCount,
            securityCount = securityCount,
            financeCount = financeCount
        )

        AuditToolbar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            selectedRole = selectedRole,
            onRoleChange = { selectedRole = it },
            selectedSource = selectedSource,
            onSourceChange = { selectedSource = it },
            selectedStatus = selectedStatus,
            onStatusChange = { selectedStatus = it },
            totalResults = filteredEntries.size,
            onRefresh = {
                onRefresh()
                reloadTick++
            }
        )

        if (isLoading || pageLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        } else if (filteredEntries.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.History, null, tint = EpiloteTextMuted)
                        Text("Aucune entrée d'audit", fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
                        Text("Aucun événement ne correspond aux filtres actuellement appliqués.", fontSize = 12.sp, color = EpiloteTextMuted)
                    }
                }
            }
        } else {
            AuditEntriesPanel(entries = filteredEntries, onOpen = { selectedEntry = it })
        }
    }

    selectedEntry?.let { entry ->
        AuditEntryDetailDialog(entry = entry, onDismiss = { selectedEntry = null })
    }
}
