package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.*
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.data.InvoiceApiDto
import cg.epilote.desktop.data.PaymentReceiptDto
import cg.epilote.desktop.data.SubscriptionApiDto
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.screens.superadmin.formatXAF
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AdminPaymentsScreen(
    groupes: List<GroupeDto>,
    plans: List<PlanDto>,
    rawSubscriptions: List<SubscriptionApiDto>,
    rawInvoices: List<InvoiceApiDto>,
    paymentReceipts: List<PaymentReceiptDto>,
    isLoading: Boolean,
    scope: CoroutineScope,
    client: DesktopAdminClient,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMethod by remember { mutableStateOf("all") }
    var filterGroupId by remember { mutableStateOf("all") }
    var recordPaymentTarget by remember { mutableStateOf<SubscriptionDto?>(null) }
    var linkedInvoiceForPayment by remember { mutableStateOf<InvoiceDto?>(null) }
    var selectedReceipt by remember { mutableStateOf<PaymentReceiptDto?>(null) }
    var confirmDeleteReceipt by remember { mutableStateOf<PaymentReceiptDto?>(null) }
    var actionFeedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val subscriptions = remember(rawSubscriptions, groupes, plans) {
        rawSubscriptions.mapNotNull { api ->
            val group = groupes.firstOrNull { it.id == api.groupeId } ?: return@mapNotNull null
            val plan = plans.firstOrNull { it.id == api.planId }
            SubscriptionDto(
                id = api.id.ifBlank { "sub::${api.groupeId}" },
                groupeId = api.groupeId,
                groupeNom = group.nom,
                groupeSlug = group.slug,
                planId = api.planId,
                planNom = plan?.nom ?: api.planId,
                prixXAF = plan?.prixXAF ?: 0L,
                statut = api.statut,
                dateDebut = api.dateDebut,
                dateFin = api.dateFin,
                renouvellementAuto = api.renouvellementAuto,
                createdAt = api.createdAt,
                maxStudents = plan?.maxStudents ?: 0,
                maxPersonnel = plan?.maxPersonnel ?: 0,
                moduleCount = plan?.modulesIncluded?.size ?: 0
            )
        }.sortedBy { it.groupeNom.lowercase() }
    }

    val groupOptions = remember(groupes) {
        listOf("all" to "Tous les groupes") + groupes.sortedBy { it.nom.lowercase() }.map { it.id to it.nom }
    }
    val subsById = remember(rawSubscriptions) { rawSubscriptions.associateBy { it.id } }
    val subsByGroup = remember(rawSubscriptions) { rawSubscriptions.associateBy { it.groupeId } }

    val invoices = remember(rawInvoices, groupes, rawSubscriptions, plans) {
        rawInvoices.map { api ->
            val group = groupes.firstOrNull { it.id == api.groupeId }
            val sub = subsById[api.subscriptionId] ?: subsByGroup[api.groupeId]
            val planId = sub?.planId?.ifBlank { group?.planId.orEmpty() } ?: group?.planId.orEmpty()
            val plan = plans.firstOrNull { it.id == planId }
            InvoiceDto(
                id = api.id,
                groupeId = api.groupeId,
                groupeNom = group?.nom ?: api.groupeId,
                groupeSlug = group?.slug ?: "",
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

    val unpaidInvoices = remember(invoices) {
        invoices.filter { it.statut in listOf("pending", "sent", "overdue", "draft") }
            .sortedByDescending { it.dateEmission }
    }

    val filtered = remember(paymentReceipts, searchQuery, filterMethod, filterGroupId) {
        paymentReceipts
            .filter { r ->
                val groupe = groupes.firstOrNull { it.id == r.groupeId }
                val nameMatch = searchQuery.isBlank() ||
                    groupe?.nom?.contains(searchQuery, true) == true ||
                    r.paymentMethodLabel.contains(searchQuery, true) ||
                    r.externalReference?.contains(searchQuery, true) == true ||
                    r.paidBy?.contains(searchQuery, true) == true
                val methodMatch = filterMethod == "all" || r.paymentMethod.equals(filterMethod, ignoreCase = true)
                val groupMatch = filterGroupId == "all" || r.groupeId == filterGroupId
                nameMatch && methodMatch && groupMatch
            }
            .sortedByDescending { it.receivedAt }
    }

    val totalEncaisse = remember(filtered) { filtered.sumOf { it.montantXAF } }
    val uniqueGroups = remember(filtered) { filtered.map { it.groupeId }.distinct().size }
    val avgAmount = remember(filtered) { if (filtered.isEmpty()) 0L else totalEncaisse / filtered.size }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Paiements & Réconciliation", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Historique de tous les paiements présentiels enregistrés (espèces, chèque, virement).",
                    fontSize = 13.sp,
                    color = EpiloteTextMuted
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val path = exportCsvToDesktop(
                                    baseName = "paiements",
                                    headers = listOf("Groupe", "Montant (XAF)", "Méthode", "Payeur", "Référence externe", "Accès début", "Accès fin", "Date réception"),
                                    rows = filtered.map { r ->
                                        listOf(
                                            groupes.firstOrNull { it.id == r.groupeId }?.nom ?: r.groupeId,
                                            r.montantXAF.toString(),
                                            r.paymentMethodLabel.ifBlank { r.paymentMethod },
                                            r.paidBy.orEmpty(),
                                            r.externalReference.orEmpty(),
                                            formatDate(r.accessStart),
                                            formatDate(r.accessEnd),
                                            formatDate(r.receivedAt)
                                        )
                                    }
                                )
                                if (path != null) actionFeedback = AdminFeedbackMessage("Export enregistré : $path")
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                    Text("  Exporter CSV", fontSize = 13.sp)
                }
                PaymentRecordButton(
                    subscriptions = subscriptions,
                    unpaidInvoices = unpaidInvoices,
                    onRecordPayment = { target, inv ->
                        linkedInvoiceForPayment = inv
                        recordPaymentTarget = target
                    }
                )
            }
        }

        actionFeedback?.let { feedback ->
            AdminFeedbackBanner(feedback = feedback, onDismiss = { actionFeedback = null })
        }

        PaymentKpiRow(
            totalReceipts = filtered.size,
            totalEncaisse = totalEncaisse,
            uniqueGroups = uniqueGroups,
            avgAmount = avgAmount
        )
        PaymentMonthlyChart(receipts = filtered)
        PaymentToolbar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            filterMethod = filterMethod,
            onFilterMethodChange = { filterMethod = it },
            filterGroupId = filterGroupId,
            onFilterGroupChange = { filterGroupId = it },
            groupOptions = groupOptions,
            onRefresh = onRefresh,
            totalResults = filtered.size
        )

        if (isLoading && paymentReceipts.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        } else if (filtered.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Payments, null, tint = EpiloteTextMuted, modifier = Modifier.size(32.dp))
                        Text("Aucun paiement enregistré", fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
                        Text("Les paiements présentiels apparaîtront ici.", fontSize = 12.sp, color = EpiloteTextMuted)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filtered.forEach { r ->
                    val groupe = groupes.firstOrNull { it.id == r.groupeId }
                    PaymentReceiptCard(
                        receipt = r,
                        groupeNom = groupe?.nom ?: r.groupeId,
                        onClick = { selectedReceipt = r }
                    )
                }
            }
        }
    }

    recordPaymentTarget?.let { target ->
        RecordPaymentDialog(
            subscription = target,
            client = client,
            scope = scope,
            onDismiss = {
                recordPaymentTarget = null
                linkedInvoiceForPayment = null
            },
            onSuccess = { receipt ->
                actionFeedback = AdminFeedbackMessage(
                    "Paiement de ${formatXAF(receipt.montantXAF)} enregistré pour " +
                        (groupes.firstOrNull { it.id == receipt.groupeId }?.nom ?: receipt.groupeId)
                )
                onRefresh()
            }
        )
    }

    selectedReceipt?.let { receipt ->
        val groupeNom = groupes.firstOrNull { it.id == receipt.groupeId }?.nom ?: receipt.groupeId
        val receiptSub = subsById[receipt.subscriptionId] ?: subsByGroup[receipt.groupeId]
        val receiptPlanId = receiptSub?.planId ?: ""
        val receiptPlanNom = plans.firstOrNull { it.id == receiptPlanId }?.nom ?: ""
        PaymentReceiptDetailDialog(
            receipt = receipt,
            groupeNom = groupeNom,
            onDismiss = { selectedReceipt = null },
            onDelete = {
                submitError = null
                confirmDeleteReceipt = receipt
            },
            planNom = receiptPlanNom
        )
    }

    confirmDeleteReceipt?.let { receipt ->
        AdminConfirmationDialog(
            title = "Supprimer le paiement",
            subtitle = groupes.firstOrNull { it.id == receipt.groupeId }?.nom ?: receipt.groupeId,
            message = "Cette action sert au nettoyage des données de test. Le reçu sera supprimé et sa facture liée sera retirée si elle a été générée par ce flux.",
            confirmLabel = "Supprimer définitivement",
            onDismiss = {
                if (!isSubmitting) {
                    submitError = null
                    confirmDeleteReceipt = null
                }
            },
            onConfirm = {
                isSubmitting = true
                scope.launch {
                    when (val result = client.deletePaymentReceipt(receipt.id)) {
                        is DesktopAdminClient.ApiCallResult.Success -> {
                            isSubmitting = false
                            confirmDeleteReceipt = null
                            selectedReceipt = null
                            actionFeedback = AdminFeedbackMessage("Paiement supprimé avec succès.")
                            onRefresh()
                        }
                        is DesktopAdminClient.ApiCallResult.Failure -> {
                            isSubmitting = false
                            submitError = result.message ?: "Impossible de supprimer le paiement."
                        }
                    }
                }
            },
            confirmContainerColor = Color(0xFFB3261E),
            isSubmitting = isSubmitting,
            errorMessage = submitError
        )
    }
}
