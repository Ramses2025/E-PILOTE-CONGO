package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
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
import cg.epilote.desktop.data.CreateCategorieDto
import cg.epilote.desktop.data.UpdateCategorieDto
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import kotlinx.serialization.Serializable

@Serializable
data class CategorieDto(
    val code: String = "",
    val nom: String = "",
    val isCore: Boolean = false,
    val ordre: Int = 0,
    val isActive: Boolean = true
)

@Composable
fun AdminCategoriesScreen(
    categories: List<CategorieDto>,
    modules: List<ModuleDto>,
    isLoading: Boolean,
    onCreateCategory: (CreateCategorieDto, (Boolean, String?) -> Unit) -> Unit,
    onUpdateCategory: (String, UpdateCategorieDto, (Boolean, String?) -> Unit) -> Unit,
    onToggleCategoryStatus: (String, Boolean, (Boolean, String?) -> Unit) -> Unit,
    onRefresh: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedCategoryForDetail by remember { mutableStateOf<CategorieDto?>(null) }
    var selectedCategoryForEdit by remember { mutableStateOf<CategorieDto?>(null) }
    var selectedCategoryForStatusAction by remember { mutableStateOf<CategorieDto?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("all") }
    var filterCore by remember { mutableStateOf("all") }
    var sortBy by remember { mutableStateOf("ordre") }
    var viewMode by remember { mutableStateOf("card") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var isStatusSubmitting by remember { mutableStateOf(false) }
    var statusError by remember { mutableStateOf<String?>(null) }
    var actionFeedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }
    val scrollState = rememberScrollState()
    val modulesByCategory = remember(modules) {
        modules.groupBy { it.categorieCode }
    }

    val handleUpdateCategory: (String, UpdateCategorieDto, (Boolean, String?) -> Unit) -> Unit = { code, dto, onResult ->
        isSubmitting = true
        submitError = null
        onUpdateCategory(code, dto) { ok, err ->
            isSubmitting = false
            if (ok) {
                actionFeedback = AdminFeedbackMessage("Catégorie mise à jour avec succès")
            } else {
                submitError = err
            }
            onResult(ok, err)
        }
    }

    val handleToggleCategoryStatus: (String, Boolean, (Boolean, String?) -> Unit) -> Unit = { code, isActive, onResult ->
        isStatusSubmitting = true
        statusError = null
        onToggleCategoryStatus(code, isActive) { ok, err ->
            isStatusSubmitting = false
            if (ok) {
                actionFeedback = AdminFeedbackMessage(if (isActive) "Catégorie réactivée avec succès" else "Catégorie suspendue avec succès")
            } else {
                statusError = err
            }
            onResult(ok, err)
        }
    }

    val filtered = categories
        .filter { category ->
            val matchesSearch = searchQuery.isBlank() ||
                category.nom.contains(searchQuery, ignoreCase = true) ||
                category.code.contains(searchQuery, ignoreCase = true)
            val matchesStatus = when (filterStatus) {
                "active" -> category.isActive
                "inactive" -> !category.isActive
                else -> true
            }
            val matchesCore = when (filterCore) {
                "core" -> category.isCore
                "custom" -> !category.isCore
                else -> true
            }
            matchesSearch && matchesStatus && matchesCore
        }
        .let { list ->
            when (sortBy) {
                "name" -> list.sortedBy { it.nom.lowercase() }
                "modules" -> list.sortedByDescending { modulesByCategory[it.code].orEmpty().size }
                else -> list.sortedBy { it.ordre }
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
            Text("Catégories", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Gestion des catégories dynamiques de la plateforme e-Pilote", fontSize = 13.sp, color = EpiloteTextMuted)
        }

        actionFeedback?.let { feedback ->
            AdminFeedbackBanner(feedback = feedback, onDismiss = { actionFeedback = null })
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        }

        CategoryKpiRow(
            totalCategories = categories.size,
            activeCategories = categories.count { it.isActive },
            coreCategories = categories.count { it.isCore },
            linkedModules = modules.count { it.categorieCode.isNotBlank() }
        )

        CategoryToolbar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            filterStatus = filterStatus,
            onFilterStatusChange = { filterStatus = it },
            filterCore = filterCore,
            onFilterCoreChange = { filterCore = it },
            sortBy = sortBy,
            onSortChange = { sortBy = it },
            viewMode = viewMode,
            onViewModeChange = { viewMode = it },
            onRefresh = onRefresh,
            onCreateCategory = {
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
                        Icon(Icons.Default.Category, null, tint = EpiloteTextMuted, modifier = Modifier.height(48.dp))
                        Text("Aucune catégorie", fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
                        Text(
                            if (searchQuery.isNotBlank() || filterStatus != "all" || filterCore != "all") {
                                "Aucune catégorie ne correspond aux filtres actuels"
                            } else {
                                "Les catégories apparaîtront ici après chargement ou création"
                            },
                            fontSize = 12.sp,
                            color = EpiloteTextMuted
                        )
                    }
                }
            }
        }

        when (viewMode) {
            "table" -> CategoryTableView(
                categories = filtered,
                modulesByCategory = modulesByCategory,
                onViewDetail = { selectedCategoryForDetail = it },
                onEdit = { selectedCategoryForEdit = it },
                onToggleStatus = { selectedCategoryForStatusAction = it }
            )

            else -> CategoryCardGrid(
                categories = filtered,
                modulesByCategory = modulesByCategory,
                onViewDetail = { selectedCategoryForDetail = it },
                onEdit = { selectedCategoryForEdit = it },
                onToggleStatus = { selectedCategoryForStatusAction = it }
            )
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showCreateDialog) {
        CategoryFormDialog(
            category = null,
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
                onCreateCategory(dto) { ok, err ->
                    isSubmitting = false
                    if (ok) {
                        showCreateDialog = false
                        actionFeedback = AdminFeedbackMessage("Catégorie créée avec succès")
                    } else {
                        submitError = err
                    }
                }
            },
            onUpdate = { _, _ -> }
        )
    }

    selectedCategoryForEdit?.let { category ->
        CategoryFormDialog(
            category = category,
            isSubmitting = isSubmitting,
            errorMessage = submitError,
            onDismiss = { selectedCategoryForEdit = null },
            onCreate = {},
            onUpdate = { code, dto ->
                handleUpdateCategory(code, dto) { ok, _ ->
                    if (ok) {
                        selectedCategoryForEdit = null
                    }
                }
            }
        )
    }

    selectedCategoryForStatusAction?.let { category ->
        val activating = !category.isActive
        AdminConfirmationDialog(
            title = if (activating) "Réactiver la catégorie" else "Suspendre la catégorie",
            subtitle = "Changement de statut",
            message = if (activating) {
                "Voulez-vous réactiver « ${category.nom} » ? Les modules liés redeviendront visibles dans la configuration plateforme."
            } else {
                "Voulez-vous suspendre « ${category.nom} » ? Les modules liés resteront présents mais la catégorie sera marquée inactive."
            },
            confirmLabel = if (activating) "Réactiver" else "Suspendre",
            onDismiss = { selectedCategoryForStatusAction = null },
            onConfirm = {
                handleToggleCategoryStatus(category.code, activating) { ok, _ ->
                    if (ok) {
                        selectedCategoryForStatusAction = null
                    }
                }
            },
            confirmContainerColor = if (activating) Color(0xFF059669) else Color(0xFFD97706),
            isSubmitting = isStatusSubmitting,
            errorMessage = statusError
        )
    }

    selectedCategoryForDetail?.let { category ->
        CategoryDetailDialog(
            category = category,
            modules = modulesByCategory[category.code].orEmpty(),
            onDismiss = { selectedCategoryForDetail = null },
            onEdit = { selectedCategoryForEdit = category },
            onToggleStatus = { selectedCategoryForStatusAction = category }
        )
    }
}
