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
import androidx.compose.material.icons.filled.NotificationsActive
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
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.data.InvoiceApiDto
import cg.epilote.desktop.data.SubscriptionApiDto
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AdminNotificationsScreen(
    groupes: List<GroupeDto>,
    adminGroupesByGroup: Map<String, List<UserDto>>,
    admins: List<AdminUserDto>,
    isLoading: Boolean,
    scope: CoroutineScope,
    client: DesktopAdminClient,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    var searchQuery by remember { mutableStateOf("") }
    var filterSeverity by remember { mutableStateOf("all") }
    var filterSource by remember { mutableStateOf("all") }
    var viewMode by remember { mutableStateOf("card") }
    var subscriptions by remember { mutableStateOf<List<SubscriptionApiDto>>(emptyList()) }
    var invoices by remember { mutableStateOf<List<InvoiceApiDto>>(emptyList()) }
    var pageLoading by remember { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var selectedItem by remember { mutableStateOf<AdminNotificationItem?>(null) }
    var isDocumentProcessing by remember { mutableStateOf(false) }
    var documentFeedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }

    suspend fun refreshNotifications() {
        pageLoading = true
        pageError = null
        val issues = mutableListOf<String>()
        val apiSubscriptions = runCatching { client.listSubscriptions() }.getOrNull().also {
            if (it == null) issues += "abonnements"
        }.orEmpty()
        val apiInvoices = runCatching { client.listInvoices() }.getOrNull().also {
            if (it == null) issues += "factures"
        }.orEmpty()
        subscriptions = apiSubscriptions
        invoices = apiInvoices
        pageError = issues.takeIf { it.isNotEmpty() }?.joinToString(prefix = "Chargement partiel : ", separator = ", ")
        pageLoading = false
    }

    LaunchedEffect(groupes, admins, adminGroupesByGroup, reloadTick) {
        refreshNotifications()
    }

    AdminAutoRefreshEffect(refreshKey = reloadTick) {
        refreshNotifications()
    }

    val operationalItems = remember(groupes, adminGroupesByGroup, admins, subscriptions, invoices) {
        buildOperationalNotifications(groupes, adminGroupesByGroup, admins, subscriptions, invoices)
    }
    val visibleItems = remember(operationalItems, searchQuery, filterSeverity, filterSource) {
        operationalItems.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, true) ||
                item.message.contains(searchQuery, true) ||
                item.targetLabel.contains(searchQuery, true) ||
                item.relatedReference.contains(searchQuery, true)
            val matchesSeverity = filterSeverity == "all" || item.severity == filterSeverity
            val matchesSource = filterSource == "all" || item.sourceType == filterSource
            matchesSearch && matchesSeverity && matchesSource
        }
    }

    val totalAlerts = operationalItems.size
    val criticalAlerts = operationalItems.count { it.severity == "critical" }
    val actionRequired = operationalItems.count { it.severity == "critical" || it.severity == "warning" }

    fun handleInvoiceDocumentAction(item: AdminNotificationItem, action: (InvoiceDto, ByteArray) -> InvoiceDocumentActionResult) {
        val invoice = item.linkedInvoice ?: return
        scope.launch {
            isDocumentProcessing = true
            val pdfBytes = runCatching { client.downloadInvoicePdf(invoice.id) }.getOrNull()
            isDocumentProcessing = false
            val feedback = pdfBytes?.let { action(invoice, it) }
                ?: InvoiceDocumentActionResult("Impossible de télécharger le PDF lié à cette notification.", isError = true)
            documentFeedback = AdminFeedbackMessage(feedback.message, isError = feedback.isError)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Notifications", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Centre de supervision temps réel des alertes plateforme, relances et incidents d'administration.", fontSize = 13.sp, color = EpiloteTextMuted)
        }

        pageError?.let {
            AdminFeedbackBanner(feedback = AdminFeedbackMessage(it, isError = true), onDismiss = { pageError = null })
        }

        NotificationKpiRow(
            totalAlerts = totalAlerts,
            criticalAlerts = criticalAlerts,
            actionRequired = actionRequired,
            broadcastCount = 0
        )

        NotificationToolbar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            filterSeverity = filterSeverity,
            onFilterSeverityChange = { filterSeverity = it },
            filterSource = filterSource,
            onFilterSourceChange = { filterSource = it },
            viewMode = viewMode,
            onViewModeChange = { viewMode = it },
            onRefresh = {
                onRefresh()
                reloadTick++
            },
            onCreateAnnouncement = {},
            totalResults = visibleItems.size,
            selectedTab = "alerts",
            onTabChange = {},
            showTabs = false,
            showCreateAnnouncement = false
        )

        if (isLoading || pageLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        } else if (visibleItems.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.NotificationsActive, null, tint = EpiloteTextMuted)
                        Text("Aucune notification", fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
                        Text(
                            "Aucune alerte active n'est détectée sur la plateforme pour les filtres courants.",
                            fontSize = 12.sp,
                            color = EpiloteTextMuted
                        )
                    }
                }
            }
        } else if (viewMode == "list") {
            NotificationListView(items = visibleItems, onViewDetail = { selectedItem = it })
        } else {
            NotificationCardGrid(items = visibleItems, onViewDetail = { selectedItem = it })
        }
    }

    selectedItem?.let { item ->
        NotificationDetailDialog(
            item = item,
            onDismiss = { selectedItem = null },
            onOpenPdf = item.linkedInvoice?.let {
                { handleInvoiceDocumentAction(item, ::openInvoicePdf) }
            },
            onExportPdf = item.linkedInvoice?.let {
                { handleInvoiceDocumentAction(item, ::exportInvoicePdf) }
            },
            onSharePdf = item.linkedInvoice?.let {
                { handleInvoiceDocumentAction(item, ::shareInvoicePdf) }
            },
            documentFeedback = documentFeedback,
            onDismissDocumentFeedback = { documentFeedback = null },
            isDocumentProcessing = isDocumentProcessing
        )
    }
}
