package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.CreatePlanDto
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.data.UpdatePlanDto
import cg.epilote.desktop.createPlanAndRefresh
import cg.epilote.desktop.togglePlanStatusAndRefresh
import cg.epilote.desktop.updatePlanAndRefresh
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.Serializable

@Serializable
data class PlanDto(
    val id: String = "",
    val nom: String = "",
    val type: String = "gratuit",
    val prixXAF: Long = 0,
    val currency: String = "XAF",
    val maxStudents: Int = 0,
    val maxPersonnel: Int = 0,
    val modulesIncluded: List<String> = emptyList(),
    val isActive: Boolean = true
)

@Composable
fun AdminPlansScreen(
    plans: List<PlanDto>,
    modules: List<ModuleDto>,
    isLoading: Boolean,
    scope: CoroutineScope,
    client: DesktopAdminClient,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("all") }
    var sortBy by remember { mutableStateOf("price") }
    var viewMode by remember { mutableStateOf("card") }
    var selectedPlan by remember { mutableStateOf<PlanDto?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var showFormDialog by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val filteredPlans = remember(plans, searchQuery, filterStatus, sortBy) {
        var result = plans
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            result = result.filter { it.nom.lowercase().contains(q) || it.type.lowercase().contains(q) || it.id.lowercase().contains(q) }
        }
        result = when (filterStatus) {
            "active" -> result.filter { it.isActive }
            "inactive" -> result.filter { !it.isActive }
            else -> result
        }
        when (sortBy) {
            "name" -> result = result.sortedBy { it.nom.lowercase() }
            "students" -> result = result.sortedByDescending { it.maxStudents }
            else -> result = result.sortedBy { it.prixXAF }
        }
        result
    }

    val totalPlans = plans.size
    val activePlans = plans.count { it.isActive }
    val allModules = plans.flatMap { it.modulesIncluded }.toSet().size
    val avgPrice = if (plans.isNotEmpty()) plans.map { it.prixXAF }.average().toLong() else 0L

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        PlanKpiRow(totalPlans = totalPlans, activePlans = activePlans, totalModulesAcrossPlans = allModules, avgPrice = avgPrice)

        PlanToolbar(
            searchQuery = searchQuery, onSearchChange = { searchQuery = it },
            filterStatus = filterStatus, onFilterStatusChange = { filterStatus = it },
            sortBy = sortBy, onSortChange = { sortBy = it },
            viewMode = viewMode, onViewModeChange = { viewMode = it },
            onRefresh = onRefresh,
            onCreatePlan = { selectedPlan = null; formError = null; showFormDialog = true },
            totalResults = filteredPlans.size
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        } else if (filteredPlans.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aucun plan trouvé", fontSize = 16.sp, color = EpiloteTextMuted)
                    Text("Essayez de modifier vos filtres ou créez un nouveau plan.", fontSize = 13.sp, color = EpiloteTextMuted)
                }
            }
        } else if (viewMode == "card") {
            PlanCardGrid(
                plans = filteredPlans, modules = modules,
                onViewDetail = { selectedPlan = it; showDetailDialog = true },
                onEdit = { selectedPlan = it; formError = null; showFormDialog = true },
                onToggleStatus = { plan -> selectedPlan = plan; showConfirmation = true }
            )
        } else {
            PlanTableView(
                plans = filteredPlans,
                onViewDetail = { selectedPlan = it; showDetailDialog = true },
                onEdit = { selectedPlan = it; formError = null; showFormDialog = true },
                onToggleStatus = { plan -> selectedPlan = plan; showConfirmation = true }
            )
        }
    }

    if (showDetailDialog && selectedPlan != null) {
        PlanDetailDialog(
            plan = selectedPlan!!, modules = modules,
            onDismiss = { showDetailDialog = false },
            onEdit = { showDetailDialog = false; formError = null; showFormDialog = true },
            onToggleStatus = { showDetailDialog = false; showConfirmation = true }
        )
    }

    if (showFormDialog) {
        PlanFormDialog(
            existing = selectedPlan, modules = modules,
            isSubmitting = isSubmitting, errorMessage = formError,
            onSubmit = { nom, type, prix, students, personnel, modIncluded, active ->
                isSubmitting = true; formError = null
                if (selectedPlan != null) {
                    scope.updatePlanAndRefresh(client, selectedPlan!!.id,
                        UpdatePlanDto(nom = nom, prixXAF = prix, maxStudents = students, maxPersonnel = personnel, modulesIncluded = modIncluded, isActive = active),
                        onRefresh, { ok, err ->
                            isSubmitting = false
                            if (ok) showFormDialog = false else formError = err
                        })
                } else {
                    scope.createPlanAndRefresh(client,
                        CreatePlanDto(nom = nom, type = type, prixXAF = prix, maxStudents = students, maxPersonnel = personnel, modulesIncluded = modIncluded, isActive = active),
                        onRefresh, { ok, err ->
                            isSubmitting = false
                            if (ok) showFormDialog = false else formError = err
                        })
                }
            },
            onDismiss = { showFormDialog = false }
        )
    }

    if (showConfirmation && selectedPlan != null) {
        val plan = selectedPlan!!
        AdminConfirmationDialog(
            title = if (plan.isActive) "Suspendre le plan" else "Réactiver le plan",
            subtitle = plan.nom,
            message = if (plan.isActive) "Voulez-vous vraiment suspendre le plan ${plan.nom} ? Les groupes associés ne pourront plus accéder aux modules premium."
            else "Voulez-vous réactiver le plan ${plan.nom} ? Les groupes associés retrouveront l'accès à leurs modules.",
            confirmLabel = if (plan.isActive) "Suspendre" else "Réactiver",
            onDismiss = { showConfirmation = false },
            onConfirm = {
                scope.togglePlanStatusAndRefresh(client, plan.id, !plan.isActive, onRefresh) { ok, _ ->
                    if (ok) showConfirmation = false
                }
            },
            confirmContainerColor = if (plan.isActive) Color(0xFFB91C1C) else Color(0xFF059669)
        )
    }
}
