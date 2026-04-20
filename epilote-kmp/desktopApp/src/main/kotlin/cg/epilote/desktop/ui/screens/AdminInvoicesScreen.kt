package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
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
import cg.epilote.desktop.data.CreateInvoiceDto
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.data.InvoiceApiDto
import cg.epilote.desktop.data.SubscriptionApiDto
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal data class InvoiceDto(
    val id: String,
    val groupeId: String,
    val groupeNom: String,
    val groupeSlug: String,
    val subscriptionId: String,
    val planId: String,
    val planNom: String,
    val montantXAF: Long,
    val statut: String,
    val dateEmission: Long,
    val dateEcheance: Long,
    val datePaiement: Long?,
    val reference: String,
    val notes: String
)

internal data class InvoiceOption(
    val groupeId: String,
    val groupeNom: String,
    val groupeSlug: String,
    val subscriptionId: String,
    val planId: String,
    val planNom: String,
    val suggestedAmountXAF: Long,
    val suggestedDueDate: Long,
    val subscriptionStatus: String
)

@Composable
fun AdminInvoicesScreen(
    groupes: List<GroupeDto>,
    plans: List<PlanDto>,
    isLoading: Boolean,
    scope: CoroutineScope,
    client: DesktopAdminClient,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("all") }
    var filterGroupId by remember { mutableStateOf("all") }
    var sortBy by remember { mutableStateOf("recent") }
    var viewMode by remember { mutableStateOf("card") }
    var invoices by remember { mutableStateOf<List<InvoiceDto>>(emptyList()) }
    var invoiceOptions by remember { mutableStateOf<List<InvoiceOption>>(emptyList()) }
    var pageLoading by remember { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var actionFeedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }
    var selectedInvoice by remember { mutableStateOf<InvoiceDto?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }
    var confirmStatusChange by remember { mutableStateOf<Pair<InvoiceDto, String>?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }

    val groupOptions = remember(groupes) {
        listOf("all" to "Tous les groupes") + groupes.sortedBy { it.nom.lowercase() }.map { it.id to it.nom }
    }

    fun toInvoiceOptions(apiSubs: List<SubscriptionApiDto>): List<InvoiceOption> {
        val currentTime = System.currentTimeMillis()
        return apiSubs.mapNotNull { sub ->
            if (sub.statut != "active" || sub.dateFin < currentTime) return@mapNotNull null
            val group = groupes.firstOrNull { it.id == sub.groupeId } ?: return@mapNotNull null
            val planId = sub.planId.ifBlank { group.planId }
            val plan = plans.firstOrNull { it.id == planId }
            InvoiceOption(
                groupeId = group.id,
                groupeNom = group.nom,
                groupeSlug = group.slug,
                subscriptionId = sub.id.ifBlank { "sub::${group.id}" },
                planId = planId,
                planNom = plan?.nom ?: planId.ifBlank { "Plan inconnu" },
                suggestedAmountXAF = plan?.prixXAF ?: 0L,
                suggestedDueDate = if (sub.dateFin > currentTime) sub.dateFin else currentTime + (7L * 86_400_000L),
                subscriptionStatus = sub.statut
            )
        }.sortedWith(compareBy<InvoiceOption> { it.groupeNom.lowercase() }.thenBy { it.planNom.lowercase() })
    }

    fun toInvoiceDtos(apiInvoices: List<InvoiceApiDto>, apiSubs: List<SubscriptionApiDto>): List<InvoiceDto> {
        val subsById = apiSubs.associateBy { it.id }
        val subsByGroup = apiSubs.associateBy { it.groupeId }
        return apiInvoices.map { api ->
            val group = groupes.firstOrNull { it.id == api.groupeId }
            val sub = subsById[api.subscriptionId] ?: subsByGroup[api.groupeId]
            val planId = sub?.planId?.ifBlank { group?.planId.orEmpty() } ?: group?.planId.orEmpty()
            val plan = plans.firstOrNull { it.id == planId }
            InvoiceDto(
                id = api.id,
                groupeId = api.groupeId,
                groupeNom = group?.nom ?: api.groupeId,
                groupeSlug = group?.slug.orEmpty(),
                subscriptionId = api.subscriptionId,
                planId = planId,
                planNom = plan?.nom ?: planId.ifBlank { "Plan inconnu" },
                montantXAF = api.montantXAF,
                statut = api.statut,
                dateEmission = api.dateEmission,
                dateEcheance = api.dateEcheance,
                datePaiement = api.datePaiement,
                reference = api.reference,
                notes = api.notes
            )
        }
    }

    fun refreshInvoices() {
        scope.launch {
            pageLoading = true
            pageError = null
            val apiSubs = runCatching { client.listSubscriptions() }.getOrNull()
            val apiInvoices = runCatching { client.listInvoices() }.getOrNull()
            if (apiSubs == null || apiInvoices == null) {
                pageError = "Impossible de charger complètement les données de facturation plateforme."
            }
            val safeSubs = apiSubs.orEmpty()
            invoiceOptions = toInvoiceOptions(safeSubs)
            invoices = toInvoiceDtos(apiInvoices.orEmpty(), safeSubs)
                .sortedByDescending { maxOf(it.dateEmission, it.datePaiement ?: 0L) }
            pageLoading = false
        }
    }

    LaunchedEffect(groupes, plans, reloadTick) {
        refreshInvoices()
    }

    val filtered = remember(invoices, searchQuery, filterStatus, filterGroupId, sortBy) {
        invoices
            .filter { invoice ->
                val matchesSearch = searchQuery.isBlank() ||
                    invoice.groupeNom.contains(searchQuery, true) ||
                    invoice.groupeSlug.contains(searchQuery, true) ||
                    invoice.reference.contains(searchQuery, true) ||
                    invoice.planNom.contains(searchQuery, true)
                val matchesStatus = filterStatus == "all" || invoice.statut == filterStatus
                val matchesGroup = filterGroupId == "all" || invoice.groupeId == filterGroupId
                matchesSearch && matchesStatus && matchesGroup
            }
            .let { list ->
                when (sortBy) {
                    "amount" -> list.sortedByDescending { it.montantXAF }
                    "dueDate" -> list.sortedByDescending { it.dateEcheance }
                    "group" -> list.sortedBy { it.groupeNom.lowercase() }
                    else -> list.sortedByDescending { it.dateEmission }
                }
            }
    }

    val paidInvoices = invoices.count { it.statut == "paid" }
    val overdueInvoices = invoices.count { it.statut == "overdue" }
    val outstandingAmount = invoices.filter { it.statut != "paid" && it.statut != "cancelled" }.sumOf { it.montantXAF }
    val totalAmount = invoices.sumOf { it.montantXAF }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Factures", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Émission, suivi et encaissement des factures plateforme par groupe scolaire.", fontSize = 13.sp, color = EpiloteTextMuted)
        }

        actionFeedback?.let { feedback ->
            AdminFeedbackBanner(feedback = feedback, onDismiss = { actionFeedback = null })
        }

        InvoiceKpiRow(
            totalInvoices = invoices.size,
            paidInvoices = paidInvoices,
            overdueInvoices = overdueInvoices,
            outstandingAmount = outstandingAmount,
            totalAmount = totalAmount
        )

        InvoiceToolbar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            filterStatus = filterStatus,
            onFilterStatusChange = { filterStatus = it },
            filterGroupId = filterGroupId,
            onFilterGroupChange = { filterGroupId = it },
            groupOptions = groupOptions,
            sortBy = sortBy,
            onSortChange = { sortBy = it },
            viewMode = viewMode,
            onViewModeChange = { viewMode = it },
            onRefresh = {
                onRefresh()
                reloadTick++
            },
            onCreateInvoice = {
                submitError = null
                showFormDialog = true
            },
            totalResults = filtered.size
        )

        if (isLoading || pageLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        } else if (filtered.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Receipt, null, tint = EpiloteTextMuted)
                        Text("Aucune facture", fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
                        Text(
                            pageError ?: if (invoiceOptions.isEmpty()) {
                                "Aucun abonnement actif de groupe scolaire n'est disponible pour émettre une facture plateforme."
                            } else {
                                "Les factures apparaîtront ici dès leur émission."
                            },
                            fontSize = 12.sp,
                            color = EpiloteTextMuted
                        )
                    }
                }
            }
        } else if (viewMode == "table") {
            InvoiceTableView(invoices = filtered, onViewDetail = { selectedInvoice = it })
        } else {
            InvoiceCardGrid(invoices = filtered, onViewDetail = { selectedInvoice = it })
        }
    }

    if (showFormDialog) {
        InvoiceFormDialog(
            invoiceOptions = invoiceOptions,
            isSubmitting = isSubmitting,
            errorMessage = submitError,
            onDismiss = { showFormDialog = false },
            onSubmit = { option, amount, dueDate, notes ->
                isSubmitting = true
                submitError = null
                scope.launch {
                    val result = runCatching {
                        client.createInvoice(
                            CreateInvoiceDto(
                                groupeId = option.groupeId,
                                subscriptionId = option.subscriptionId,
                                montantXAF = amount,
                                dateEcheance = dueDate,
                                notes = notes
                            )
                        )
                    }.getOrNull()
                    isSubmitting = false
                    if (result != null) {
                        showFormDialog = false
                        actionFeedback = AdminFeedbackMessage("Facture émise avec succès")
                        reloadTick++
                    } else {
                        submitError = "Impossible d'émettre la facture."
                    }
                }
            }
        )
    }

    selectedInvoice?.let { invoice ->
        InvoiceDetailDialog(
            invoice = invoice,
            onDismiss = { selectedInvoice = null },
            onStatusChange = { nextStatus ->
                confirmStatusChange = invoice to nextStatus
            }
        )
    }

    confirmStatusChange?.let { (invoice, nextStatus) ->
        AdminConfirmationDialog(
            title = invoiceStatusActionTitle(nextStatus),
            subtitle = invoice.reference.ifBlank { invoice.groupeNom },
            message = invoiceStatusActionMessage(invoice, nextStatus),
            confirmLabel = invoiceStatusActionTitle(nextStatus),
            onDismiss = { confirmStatusChange = null },
            onConfirm = {
                isSubmitting = true
                submitError = null
                scope.launch {
                    val result = runCatching {
                        client.updateInvoiceStatus(
                            invoice.id,
                            nextStatus,
                            if (nextStatus == "paid") System.currentTimeMillis() else null
                        )
                    }.getOrNull()
                    isSubmitting = false
                    if (result != null) {
                        confirmStatusChange = null
                        selectedInvoice = null
                        actionFeedback = AdminFeedbackMessage("Statut de facture mis à jour")
                        reloadTick++
                    } else {
                        submitError = "Impossible de modifier le statut de la facture."
                    }
                }
            },
            confirmContainerColor = invoiceStatusActionColor(nextStatus),
            isSubmitting = isSubmitting,
            errorMessage = submitError
        )
    }
}
