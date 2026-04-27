package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import cg.epilote.desktop.data.CreateSubscriptionDto
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.data.SubscriptionApiDto
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal data class SubscriptionDto(
    val id: String,
    val groupeId: String,
    val groupeNom: String,
    val groupeSlug: String,
    val planId: String,
    val planNom: String,
    val prixXAF: Long,
    val statut: String,
    val dateDebut: Long,
    val dateFin: Long,
    val renouvellementAuto: Boolean,
    val createdAt: Long,
    val maxStudents: Int,
    val maxPersonnel: Int,
    val moduleCount: Int
)

@Composable
fun AdminSubscriptionsScreen(
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
    var filterPlanId by remember { mutableStateOf("all") }
    var sortBy by remember { mutableStateOf("recent") }
    var viewMode by remember { mutableStateOf("card") }
    var subscriptions by remember { mutableStateOf<List<SubscriptionDto>>(emptyList()) }
    var pageLoading by remember { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var actionFeedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }
    var selectedSubscription by remember { mutableStateOf<SubscriptionDto?>(null) }
    var formTarget by remember { mutableStateOf<SubscriptionDto?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }
    var confirmStatusChange by remember { mutableStateOf<Pair<SubscriptionDto, String>?>(null) }
    var recordPaymentTarget by remember { mutableStateOf<SubscriptionDto?>(null) }
    var historyTarget by remember { mutableStateOf<SubscriptionDto?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }

    val planOptions = remember(plans) {
        listOf("all" to "Tous les plans") + plans.sortedBy { it.prixXAF }.map { it.id to it.nom }
    }

    fun toUiDtos(apiItems: List<SubscriptionApiDto>): List<SubscriptionDto> = apiItems.mapNotNull { api ->
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
    }

    fun refreshSubscriptions() {
        scope.launch {
            pageLoading = true
            pageError = null
            val apiResult = runCatching { client.listSubscriptions() }.getOrNull().orEmpty()
            subscriptions = toUiDtos(apiResult)
            pageLoading = false
        }
    }

    LaunchedEffect(groupes, plans, reloadTick) {
        refreshSubscriptions()
    }

    val filtered = remember(subscriptions, searchQuery, filterStatus, filterPlanId, sortBy) {
        subscriptions
            .filter { sub ->
                val searchMatch = searchQuery.isBlank() ||
                    sub.groupeNom.contains(searchQuery, true) ||
                    sub.groupeSlug.contains(searchQuery, true) ||
                    sub.planNom.contains(searchQuery, true)
                val statusMatch = filterStatus == "all" || sub.statut == filterStatus
                val planMatch = filterPlanId == "all" || sub.planId == filterPlanId
                searchMatch && statusMatch && planMatch
            }
            .let { list ->
                when (sortBy) {
                    "group" -> list.sortedBy { it.groupeNom.lowercase() }
                    "amount" -> list.sortedByDescending { it.prixXAF }
                    else -> list.sortedByDescending { it.createdAt }
                }
            }
    }

    val activeSubscriptions = subscriptions.count { it.statut == "active" }
    val suspendedSubscriptions = subscriptions.count { it.statut == "suspended" }
    val monthlyRevenue = subscriptions.filter { it.statut == "active" }.sumOf { it.prixXAF }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Abonnements", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Pilotage des abonnements par groupe scolaire, dans le même esprit que Administrateurs.", fontSize = 13.sp, color = EpiloteTextMuted)
        }

        actionFeedback?.let { feedback ->
            AdminFeedbackBanner(feedback = feedback, onDismiss = { actionFeedback = null })
        }

        SubscriptionKpiRow(
            totalSubscriptions = subscriptions.size,
            activeSubscriptions = activeSubscriptions,
            suspendedSubscriptions = suspendedSubscriptions,
            monthlyRevenue = monthlyRevenue
        )

        SubscriptionToolbar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            filterStatus = filterStatus,
            onFilterStatusChange = { filterStatus = it },
            filterPlanId = filterPlanId,
            onFilterPlanIdChange = { filterPlanId = it },
            planOptions = planOptions,
            sortBy = sortBy,
            onSortChange = { sortBy = it },
            viewMode = viewMode,
            onViewModeChange = { viewMode = it },
            onRefresh = {
                onRefresh()
                reloadTick++
            },
            onCreateSubscription = {
                formTarget = null
                submitError = null
                showFormDialog = true
            },
            totalResults = filtered.size
        )

        if (isLoading || pageLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        } else if (filtered.isEmpty()) {
            Card(modifier = Modifier.fillMaxSize(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
                Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Icon(Icons.Default.Subscriptions, null, tint = EpiloteTextMuted)
                        Text("Aucun abonnement", fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
                        Text(pageError ?: "Les abonnements apparaîtront ici dès qu’un groupe dispose d’un plan.", fontSize = 12.sp, color = EpiloteTextMuted)
                    }
                }
            }
        } else if (viewMode == "table") {
            SubscriptionTableView(subscriptions = filtered, onViewDetail = { selectedSubscription = it })
        } else {
            SubscriptionCardGrid(subscriptions = filtered, onViewDetail = { selectedSubscription = it })
        }
    }

    if (showFormDialog) {
        SubscriptionFormDialog(
            groupes = groupes,
            plans = plans,
            existing = formTarget,
            isSubmitting = isSubmitting,
            errorMessage = submitError,
            onDismiss = { showFormDialog = false },
            onSubmit = { groupeId, planId, renouvellementAuto ->
                isSubmitting = true
                submitError = null
                scope.launch {
                    val result = runCatching { client.createSubscription(CreateSubscriptionDto(groupeId, planId, renouvellementAuto)) }.getOrNull()
                    isSubmitting = false
                    if (result != null) {
                        showFormDialog = false
                        actionFeedback = AdminFeedbackMessage("Abonnement mis à jour avec succès")
                        onRefresh()
                        reloadTick++
                    } else {
                        submitError = "Impossible d'enregistrer l'abonnement."
                    }
                }
            }
        )
    }

    selectedSubscription?.let { subscription ->
        SubscriptionDetailDialog(
            subscription = subscription,
            onDismiss = { selectedSubscription = null },
            onChangePlan = {
                formTarget = subscription
                selectedSubscription = null
                submitError = null
                showFormDialog = true
            },
            onChangeStatus = { nextStatus ->
                confirmStatusChange = subscription to nextStatus
            },
            onRecordPayment = {
                recordPaymentTarget = subscription
                selectedSubscription = null
            },
            onShowHistory = {
                historyTarget = subscription
                selectedSubscription = null
            }
        )
    }

    recordPaymentTarget?.let { target ->
        RecordPaymentDialog(
            subscription = target,
            client = client,
            scope = scope,
            onDismiss = { recordPaymentTarget = null },
            onSuccess = { receipt ->
                recordPaymentTarget = null
                actionFeedback = AdminFeedbackMessage(
                    "Paiement enregistré : ${formatMoneyXaf(receipt.montantXAF)} — accès activé jusqu'au " +
                        cg.epilote.desktop.ui.screens.superadmin.formatDate(receipt.accessEnd)
                )
                onRefresh()
                reloadTick++
            }
        )
    }

    historyTarget?.let { target ->
        PaymentHistoryDialog(
            subscription = target,
            client = client,
            scope = scope,
            onDismiss = { historyTarget = null }
        )
    }

    confirmStatusChange?.let { (subscription, nextStatus) ->
        AdminConfirmationDialog(
            title = if (nextStatus == "suspended") "Suspendre l'abonnement" else "Réactiver l'abonnement",
            subtitle = subscription.groupeNom,
            message = if (nextStatus == "suspended") {
                "Voulez-vous suspendre l'abonnement ${subscription.planNom} du groupe ${subscription.groupeNom} ?"
            } else {
                "Voulez-vous réactiver l'abonnement ${subscription.planNom} du groupe ${subscription.groupeNom} ?"
            },
            confirmLabel = if (nextStatus == "suspended") "Suspendre" else "Réactiver",
            onDismiss = { confirmStatusChange = null },
            onConfirm = {
                isSubmitting = true
                submitError = null
                scope.launch {
                    val result = runCatching { client.updateSubscriptionStatus(subscription.id, nextStatus) }.getOrNull()
                    isSubmitting = false
                    if (result != null) {
                        confirmStatusChange = null
                        selectedSubscription = null
                        actionFeedback = AdminFeedbackMessage("Statut d'abonnement mis à jour")
                        onRefresh()
                        reloadTick++
                    } else {
                        submitError = "Impossible de modifier le statut de l'abonnement."
                    }
                }
            },
            confirmContainerColor = if (nextStatus == "suspended") Color(0xFFD97706) else Color(0xFF059669),
            isSubmitting = isSubmitting,
            errorMessage = submitError
        )
    }
}
