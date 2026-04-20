package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import cg.epilote.desktop.data.CreateModuleDto
import cg.epilote.desktop.data.UpdateModuleDto
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import kotlinx.serialization.Serializable

@Serializable
data class ModuleDto(
    val id: String = "",
    val code: String = "",
    val nom: String = "",
    val categorieCode: String = "",
    val description: String = "",
    val isCore: Boolean = false,
    val requiredPlan: String = "gratuit",
    val isActive: Boolean = true,
    val ordre: Int = 0
)

@Composable
fun AdminModulesScreen(
    modules: List<ModuleDto>,
    categories: List<CategorieDto>,
    isLoading: Boolean,
    onCreateModule: (CreateModuleDto, (Boolean, String?) -> Unit) -> Unit,
    onUpdateModule: (String, UpdateModuleDto, (Boolean, String?) -> Unit) -> Unit,
    onToggleModuleStatus: (String, Boolean, (Boolean, String?) -> Unit) -> Unit,
    onRefresh: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedModuleForDetail by remember { mutableStateOf<ModuleDto?>(null) }
    var selectedModuleForEdit by remember { mutableStateOf<ModuleDto?>(null) }
    var selectedModuleForStatusAction by remember { mutableStateOf<ModuleDto?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("all") }
    var filterCategory by remember { mutableStateOf("all") }
    var filterPlan by remember { mutableStateOf("all") }
    var sortBy by remember { mutableStateOf("ordre") }
    var viewMode by remember { mutableStateOf("card") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var isStatusSubmitting by remember { mutableStateOf(false) }
    var statusError by remember { mutableStateOf<String?>(null) }
    var actionFeedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }
    val scrollState = rememberScrollState()
    val categoriesByCode = remember(categories) { categories.associateBy { it.code } }
    val categoryOptions = remember(categories) {
        listOf("all" to "Toutes les catégories") + categories
            .sortedWith(compareBy<CategorieDto> { it.ordre }.thenBy { it.nom.lowercase() })
            .map { it.code to it.nom }
    }
    val statusOptions = listOf("all" to "Tous les statuts", "active" to "Actifs", "inactive" to "Inactifs")
    val sortOptions = listOf(
        "ordre" to "Ordre",
        "name" to "Nom",
        "category" to "Catégorie",
        "plan" to "Plan requis"
    )

    val handleUpdateModule: (String, UpdateModuleDto, (Boolean, String?) -> Unit) -> Unit = { moduleId, dto, onResult ->
        isSubmitting = true
        submitError = null
        onUpdateModule(moduleId, dto) { ok, err ->
            isSubmitting = false
            if (ok) {
                actionFeedback = AdminFeedbackMessage("Module mis à jour avec succès")
            } else {
                submitError = err
            }
            onResult(ok, err)
        }
    }

    val handleToggleStatus: (String, Boolean, (Boolean, String?) -> Unit) -> Unit = { moduleId, isActive, onResult ->
        isStatusSubmitting = true
        statusError = null
        onToggleModuleStatus(moduleId, isActive) { ok, err ->
            isStatusSubmitting = false
            if (ok) {
                actionFeedback = AdminFeedbackMessage(if (isActive) "Module réactivé avec succès" else "Module suspendu avec succès")
            } else {
                statusError = err
            }
            onResult(ok, err)
        }
    }

    val filtered = modules
        .filter { module ->
            val matchesSearch = searchQuery.isBlank() ||
                module.nom.contains(searchQuery, ignoreCase = true) ||
                module.code.contains(searchQuery, ignoreCase = true) ||
                module.description.contains(searchQuery, ignoreCase = true)
            val matchesStatus = when (filterStatus) {
                "active" -> module.isActive
                "inactive" -> !module.isActive
                else -> true
            }
            val matchesCategory = filterCategory == "all" || module.categorieCode == filterCategory
            val matchesPlan = filterPlan == "all" || module.requiredPlan.equals(filterPlan, ignoreCase = true)
            matchesSearch && matchesStatus && matchesCategory && matchesPlan
        }
        .let { list ->
            when (sortBy) {
                "name" -> list.sortedBy { it.nom.lowercase() }
                "category" -> list.sortedWith(compareBy<ModuleDto> { categoriesByCode[it.categorieCode]?.nom ?: it.categorieCode }.thenBy { it.ordre })
                "plan" -> list.sortedWith(compareBy<ModuleDto> { it.requiredPlan }.thenBy { it.ordre })
                else -> list.sortedWith(compareBy<ModuleDto> { it.ordre }.thenBy { it.nom.lowercase() })
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column {
            Text("Modules", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Gestion complète des modules rattachés aux catégories de la plateforme", fontSize = 13.sp, color = EpiloteTextMuted)
        }

        actionFeedback?.let { feedback ->
            AdminFeedbackBanner(feedback = feedback, onDismiss = { actionFeedback = null })
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        }

        ModuleKpiRow(
            totalModules = modules.size,
            activeModules = modules.count { it.isActive },
            coreModules = modules.count { it.isCore },
            premiumModules = modules.count { it.requiredPlan != "gratuit" }
        )

        ModuleToolbar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            statusOptions = statusOptions,
            filterStatus = filterStatus,
            onFilterStatusChange = { filterStatus = it },
            categoryOptions = categoryOptions,
            filterCategory = filterCategory,
            onFilterCategoryChange = { filterCategory = it },
            filterPlan = filterPlan,
            onFilterPlanChange = { filterPlan = it },
            sortOptions = sortOptions,
            sortBy = sortBy,
            onSortChange = { sortBy = it },
            viewMode = viewMode,
            onViewModeChange = { viewMode = it },
            onRefresh = onRefresh,
            onCreateModule = {
                submitError = null
                showCreateDialog = true
            },
            totalResults = filtered.size
        )

        if (filtered.isEmpty() && !isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Extension, null, tint = EpiloteTextMuted, modifier = Modifier.size(48.dp))
                        Text("Aucun module", fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
                        Text(
                            if (searchQuery.isNotBlank() || filterStatus != "all" || filterCategory != "all" || filterPlan != "all") {
                                "Aucun module ne correspond aux filtres actuels"
                            } else {
                                "Les modules apparaîtront ici après chargement ou création"
                            },
                            fontSize = 12.sp,
                            color = EpiloteTextMuted
                        )
                    }
                }
            }
        }

        when (viewMode) {
            "table" -> ModuleTableView(
                modules = filtered,
                categoriesByCode = categoriesByCode,
                onViewDetail = { selectedModuleForDetail = it },
                onEdit = { selectedModuleForEdit = it },
                onToggleStatus = { selectedModuleForStatusAction = it }
            )

            else -> ModuleCardGrid(
                modules = filtered,
                categoriesByCode = categoriesByCode,
                onViewDetail = { selectedModuleForDetail = it },
                onEdit = { selectedModuleForEdit = it },
                onToggleStatus = { selectedModuleForStatusAction = it }
            )
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showCreateDialog) {
        ModuleFormDialog(
            module = null,
            categories = categories,
            isSubmitting = isSubmitting,
            errorMessage = submitError,
            onDismiss = {
                if (!isSubmitting) {
                    showCreateDialog = false
                    submitError = null
                }
            },
            onCreate = { dto ->
                isSubmitting = true
                submitError = null
                onCreateModule(dto) { ok, err ->
                    isSubmitting = false
                    if (ok) {
                        showCreateDialog = false
                        actionFeedback = AdminFeedbackMessage("Module créé avec succès")
                    } else {
                        submitError = err
                    }
                }
            },
            onUpdate = { _, _ -> }
        )
    }

    selectedModuleForEdit?.let { module ->
        ModuleFormDialog(
            module = module,
            categories = categories,
            isSubmitting = isSubmitting,
            errorMessage = submitError,
            onDismiss = { if (!isSubmitting) selectedModuleForEdit = null },
            onCreate = {},
            onUpdate = { moduleId, dto ->
                handleUpdateModule(moduleId, dto) { ok, _ ->
                    if (ok) {
                        selectedModuleForEdit = null
                    }
                }
            }
        )
    }

    selectedModuleForStatusAction?.let { module ->
        val activating = !module.isActive
        AdminConfirmationDialog(
            title = if (activating) "Réactiver le module" else "Suspendre le module",
            subtitle = "Changement de statut",
            message = if (activating) {
                "Voulez-vous réactiver « ${module.nom} » ? Il redeviendra disponible dans la configuration plateforme."
            } else {
                "Voulez-vous suspendre « ${module.nom} » ? Il restera enregistré mais sera marqué inactif."
            },
            confirmLabel = if (activating) "Réactiver" else "Suspendre",
            onDismiss = { if (!isStatusSubmitting) selectedModuleForStatusAction = null },
            onConfirm = {
                handleToggleStatus(module.code, activating) { ok, _ ->
                    if (ok) {
                        selectedModuleForStatusAction = null
                    }
                }
            },
            confirmContainerColor = if (activating) Color(0xFF059669) else Color(0xFFD97706),
            isSubmitting = isStatusSubmitting,
            errorMessage = statusError
        )
    }

    selectedModuleForDetail?.let { module ->
        ModuleDetailDialog(
            module = module,
            categoryLabel = categoriesByCode[module.categorieCode]?.nom ?: module.categorieCode,
            onDismiss = { selectedModuleForDetail = null },
            onEdit = { selectedModuleForEdit = module },
            onToggleStatus = { selectedModuleForStatusAction = module }
        )
    }
}
