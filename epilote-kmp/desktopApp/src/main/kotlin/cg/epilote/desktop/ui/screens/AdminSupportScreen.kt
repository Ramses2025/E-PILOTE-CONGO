package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
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
fun AdminSupportScreen(
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
    var viewMode by remember { mutableStateOf("list") }
    var subscriptions by remember { mutableStateOf<List<SubscriptionApiDto>>(emptyList()) }
    var invoices by remember { mutableStateOf<List<InvoiceApiDto>>(emptyList()) }
    var pageLoading by remember { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var selectedItem by remember { mutableStateOf<AdminNotificationItem?>(null) }
    var isDocumentProcessing by remember { mutableStateOf(false) }
    var documentFeedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }

    suspend fun refreshSupport() {
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
        refreshSupport()
    }

    AdminAutoRefreshEffect(refreshKey = reloadTick) {
        refreshSupport()
    }

    val supportItems = remember(groupes, adminGroupesByGroup, admins, subscriptions, invoices) {
        buildOperationalNotifications(groupes, adminGroupesByGroup, admins, subscriptions, invoices)
    }
    val filteredItems = remember(supportItems, searchQuery, filterSeverity, filterSource) {
        supportItems.filter { item ->
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

    val criticalCount = supportItems.count { it.severity == "critical" }
    val billingCount = supportItems.count { it.sourceType == "invoice" || it.sourceType == "subscription" }
    val securityCount = supportItems.count { it.sourceType == "admin" }
    val groupsAtRiskCount = supportItems.count { it.sourceType == "group" }
    val groupsCovered = groupes.count { !adminGroupesByGroup[it.id].isNullOrEmpty() }
    val activeAdmins = admins.count { it.isActive && it.status.equals("active", true) }
    val criticalItems = remember(supportItems) { supportItems.filter { it.severity == "critical" }.take(5) }
    val billingItems = remember(supportItems) {
        supportItems.filter { it.sourceType == "invoice" || it.sourceType == "subscription" }.take(5)
    }
    val securityItems = remember(supportItems) { supportItems.filter { it.sourceType == "admin" }.take(5) }
    val coverageItems = remember(supportItems) { supportItems.filter { it.sourceType == "group" }.take(5) }

    fun handleInvoiceDocumentAction(item: AdminNotificationItem, action: (InvoiceDto, ByteArray) -> InvoiceDocumentActionResult) {
        val invoice = item.linkedInvoice ?: return
        scope.launch {
            isDocumentProcessing = true
            val pdfBytes = runCatching { client.downloadInvoicePdf(invoice.id) }.getOrNull()
            isDocumentProcessing = false
            val feedback = pdfBytes?.let { action(invoice, it) }
                ?: InvoiceDocumentActionResult("Impossible de télécharger le PDF lié à ce signalement.", isError = true)
            documentFeedback = AdminFeedbackMessage(feedback.message, isError = feedback.isError)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Support", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "Centre opérationnel du support super-admin : incidents de facturation, sécurité des accès et couverture des groupes.",
                fontSize = 13.sp,
                color = EpiloteTextMuted
            )
        }

        pageError?.let {
            AdminFeedbackBanner(feedback = AdminFeedbackMessage(it, isError = true), onDismiss = { pageError = null })
        }

        SupportKpiRow(
            criticalCount = criticalCount,
            billingCount = billingCount,
            securityCount = securityCount,
            groupsAtRiskCount = groupsAtRiskCount
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
            totalResults = filteredItems.size,
            selectedTab = "alerts",
            onTabChange = {},
            showTabs = false,
            showCreateAnnouncement = false
        )

        if (isLoading || pageLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        } else if (filteredItems.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.SupportAgent, null, tint = EpiloteTextMuted)
                        Text("Aucun signalement actif", fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
                        Text(
                            "Aucune anomalie prioritaire n'est visible sur les données actuellement chargées.",
                            fontSize = 12.sp,
                            color = EpiloteTextMuted
                        )
                    }
                }
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 1120.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SupportCoverageCard(groupes.size, groupsCovered, admins.size, activeAdmins)
                        SupportInsightPanel(
                            criticalItems = criticalItems,
                            billingItems = billingItems,
                            securityItems = securityItems,
                            coverageItems = coverageItems,
                            onOpen = { selectedItem = it }
                        )
                        if (viewMode == "card") {
                            NotificationCardGrid(items = filteredItems, onViewDetail = { selectedItem = it })
                        } else {
                            NotificationListView(items = filteredItems, onViewDetail = { selectedItem = it })
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1.7f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SupportCoverageCard(groupes.size, groupsCovered, admins.size, activeAdmins)
                            if (viewMode == "card") {
                                NotificationCardGrid(items = filteredItems, onViewDetail = { selectedItem = it })
                            } else {
                                NotificationListView(items = filteredItems, onViewDetail = { selectedItem = it })
                            }
                        }
                        SupportInsightPanel(
                            modifier = Modifier.weight(1f),
                            criticalItems = criticalItems,
                            billingItems = billingItems,
                            securityItems = securityItems,
                            coverageItems = coverageItems,
                            onOpen = { selectedItem = it }
                        )
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        NotificationDetailDialog(
            item = item,
            onDismiss = { selectedItem = null },
            onOpenPdf = item.linkedInvoice?.let { { handleInvoiceDocumentAction(item, ::openInvoicePdf) } },
            onExportPdf = item.linkedInvoice?.let { { handleInvoiceDocumentAction(item, ::exportInvoicePdf) } },
            onSharePdf = item.linkedInvoice?.let { { handleInvoiceDocumentAction(item, ::shareInvoicePdf) } },
            documentFeedback = documentFeedback,
            onDismissDocumentFeedback = { documentFeedback = null },
            isDocumentProcessing = isDocumentProcessing
        )
    }
}
