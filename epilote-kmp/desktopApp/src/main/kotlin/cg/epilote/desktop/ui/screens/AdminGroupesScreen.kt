package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.CreateGroupeDto
import cg.epilote.desktop.data.UpdateGroupeDto
import cg.epilote.desktop.ui.screens.superadmin.KpiCard
import cg.epilote.desktop.ui.theme.*
import cg.epilote.desktop.ui.theme.AnimatedCardEntrance
import cg.epilote.desktop.ui.theme.PulsingLoadingBar
import cg.epilote.desktop.ui.theme.cursorHand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@Composable
fun AdminGroupesScreen(
    groupes: List<GroupeDto>,
    adminGroupesByGroup: Map<String, List<UserDto>>,
    plans: List<PlanDto>,
    totalEcoles: Int,
    totalUtilisateurs: Int,
    isLoading: Boolean,
    onCreateGroupe: (CreateGroupeDto, (Boolean, String?) -> Unit) -> Unit,
    onUpdateGroupe: (groupId: String, UpdateGroupeDto, (Boolean, String?) -> Unit) -> Unit,
    onDeleteGroupe: (groupId: String, (Boolean, String?) -> Unit) -> Unit,
    onToggleGroupeStatus: (groupId: String, isActive: Boolean, (Boolean, String?) -> Unit) -> Unit,
    onCreateAdminGroupe: (groupId: String, password: String, nom: String, prenom: String, email: String, (Boolean, String?) -> Unit) -> Unit,
    onNavigateDetail: ((GroupeDto) -> Unit)? = null,
    onRefresh: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope = kotlinx.coroutines.GlobalScope
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedGroupeForEdit by remember { mutableStateOf<GroupeDto?>(null) }
    var selectedGroupeForAdmin by remember { mutableStateOf<GroupeDto?>(null) }
    var selectedGroupeForDetail by remember { mutableStateOf<GroupeDto?>(null) }
    var selectedGroupeForDelete by remember { mutableStateOf<GroupeDto?>(null) }
    var selectedGroupeForStatusAction by remember { mutableStateOf<GroupeDto?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("all") }
    var sortBy by remember { mutableStateOf("recent") }
    var viewMode by remember { mutableStateOf("card") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var isDeleteSubmitting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var isStatusSubmitting by remember { mutableStateOf(false) }
    var statusError by remember { mutableStateOf<String?>(null) }
    var isAdminSubmitting by remember { mutableStateOf(false) }
    var adminSubmitError by remember { mutableStateOf<String?>(null) }
    var actionFeedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }
    val scrollState = rememberScrollState()

    val groupesActifs = groupes.count { it.isActive }
    val filtered = groupes
        .filter { g ->
            val matchSearch = searchQuery.isBlank() ||
                g.nom.contains(searchQuery, ignoreCase = true) ||
                g.slug.contains(searchQuery, ignoreCase = true) ||
                (g.department?.contains(searchQuery, ignoreCase = true) == true) ||
                (g.city?.contains(searchQuery, ignoreCase = true) == true) ||
                (g.email?.contains(searchQuery, ignoreCase = true) == true)
            val matchStatus = when (filterStatus) {
                "actif" -> g.isActive
                "inactif" -> !g.isActive
                else -> true
            }
            matchSearch && matchStatus
        }
        .let { list ->
            when (sortBy) {
                "recent" -> list.sortedByDescending { it.createdAt }
                "nom" -> list.sortedBy { it.nom.lowercase() }
                "ecoles" -> list.sortedByDescending { it.ecolesCount }
                else -> list
            }
        }

    val byDepartment = groupes.groupBy { it.department?.ifBlank { null } ?: "Non renseigné" }
    val byPlan = groupes.groupBy { g ->
        when {
            g.planId.contains("institutionnel", true) -> "Institutionnel"
            g.planId.contains("pro", true) -> "Pro"
            g.planId.contains("premium", true) -> "Premium"
            g.planId.isNotBlank() -> "Gratuit"
            else -> "Non défini"
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4F8))) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            actionFeedback?.let {
                AdminFeedbackBanner(
                    feedback = it,
                    onDismiss = { actionFeedback = null }
                )
            }

            GroupesKpiRow(groupes.size, groupesActifs, totalEcoles, totalUtilisateurs)

            if (actionFeedback == null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val path = exportCsvToDesktop(
                                        baseName = "groupes",
                                        headers = listOf("Nom", "Slug", "Département", "Ville", "Email", "Écoles", "Utilisateurs", "Statut", "Plan", "Créé le"),
                                        rows = filtered.map { g ->
                                            listOf(
                                                g.nom, g.slug,
                                                g.department.orEmpty(), g.city.orEmpty(),
                                                g.email.orEmpty(),
                                                g.ecolesCount.toString(), g.usersCount.toString(),
                                                if (g.isActive) "Actif" else "Inactif",
                                                g.planId,
                                                java.time.Instant.ofEpochMilli(g.createdAt)
                                                    .atZone(java.time.ZoneId.systemDefault())
                                                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                            )
                                        }
                                    )
                                    if (path != null) actionFeedback = AdminFeedbackMessage("✓ Export CSV enregistré : $path")
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                        Text("  Exporter CSV", fontSize = 13.sp)
                    }
                }
            }

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 980.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        GroupeDepartmentDistribution(byDepartment, Modifier.fillMaxWidth())
                        GroupePlanDistributionChart(byPlan, Modifier.fillMaxWidth())
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp)) {
                        GroupeDepartmentDistribution(byDepartment, Modifier.weight(1f))
                        GroupePlanDistributionChart(byPlan, Modifier.weight(1f))
                    }
                }
            }

            GroupesToolbar(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                filterStatus = filterStatus,
                onFilterChange = { filterStatus = it },
                sortBy = sortBy,
                onSortChange = { sortBy = it },
                viewMode = viewMode,
                onViewModeChange = { viewMode = it },
                onRefresh = onRefresh,
                onNewGroupe = {
                    submitError = null
                    showCreateDialog = true
                },
                totalResults = filtered.size
            )

            if (isLoading && groupes.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), Arrangement.spacedBy(12.dp), Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = EpiloteGreen, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                    Text("Chargement des groupes…", fontSize = 13.sp, color = EpiloteTextMuted)
                    PulsingLoadingBar(Modifier.padding(horizontal = 40.dp))
                }
            }
            if (isLoading && groupes.isNotEmpty()) {
                PulsingLoadingBar()
            }

            if (filtered.isEmpty() && !isLoading) {
                EmptyGroupesState(searchQuery.isNotBlank())
            }

            when (viewMode) {
                "table" -> GroupeTableView(
                    groupes = filtered,
                    onEdit = { submitError = null; selectedGroupeForEdit = it },
                    onAddAdmin = { adminSubmitError = null; selectedGroupeForAdmin = it },
                    onViewDetail = { selectedGroupeForDetail = it }
                )
                else -> GroupeCardGrid(
                    groupes = filtered,
                    adminGroupesByGroup = adminGroupesByGroup,
                    onEdit = { submitError = null; selectedGroupeForEdit = it },
                    onAddAdmin = { adminSubmitError = null; selectedGroupeForAdmin = it },
                    onViewDetail = { selectedGroupeForDetail = it }
                )
            }
        }
    }

    // ── Create dialog ──
    if (showCreateDialog) {
        CreateGroupeDialog(
            onDismiss = { if (!isSubmitting) showCreateDialog = false },
            onSubmit = { dto ->
                isSubmitting = true
                submitError = null
                onCreateGroupe(dto) { success, error ->
                    isSubmitting = false
                    if (success) {
                        showCreateDialog = false
                        actionFeedback = AdminFeedbackMessage("Groupe créé avec succès.")
                    } else {
                        submitError = error
                    }
                }
            },
            availablePlans = plans,
            isSubmitting = isSubmitting,
            submitError = submitError
        )
    }

    // ── Edit dialog ──
    selectedGroupeForEdit?.let { groupe ->
        CreateGroupeDialog(
            onDismiss = { if (!isSubmitting) selectedGroupeForEdit = null },
            onSubmit = { dto ->
                isSubmitting = true
                submitError = null
                onUpdateGroupe(groupe.id, dto.toUpdateGroupeDto(dto.isActive)) { success, error ->
                    isSubmitting = false
                    if (success) {
                        selectedGroupeForEdit = null
                        selectedGroupeForDetail = null
                        actionFeedback = AdminFeedbackMessage("Groupe mis à jour avec succès.")
                    } else {
                        submitError = error
                    }
                }
            },
            availablePlans = plans,
            initialData = groupe.toGroupeFormInitialData(),
            title = "Modifier le groupe",
            subtitle = "Mettez à jour les informations, le plan et le logo du groupe scolaire",
            submitLabel = "Enregistrer les modifications",
            includeBlankOptionalFields = true,
            isSubmitting = isSubmitting,
            submitError = submitError
        )
    }

    // ── Detail dialog ──
    selectedGroupeForDetail?.let { groupe ->
        val admins = adminGroupesByGroup[groupe.id].orEmpty()
        GroupeDetailDialog(
            groupe = groupe,
            admins = admins,
            onDismiss = { selectedGroupeForDetail = null },
            onEdit = { submitError = null; selectedGroupeForEdit = groupe },
            onDelete = { deleteError = null; selectedGroupeForDelete = groupe },
            onToggleStatus = { statusError = null; selectedGroupeForStatusAction = groupe },
            onAddAdmin = { adminSubmitError = null; selectedGroupeForAdmin = groupe },
            onViewFullDetail = onNavigateDetail?.let { nav -> { selectedGroupeForDetail = null; nav(groupe) } }
        )
    }

    // ── Admin dialog ──
    selectedGroupeForAdmin?.let { groupe ->
        CreateAdminGroupeDialog(
            groupe = groupe,
            onDismiss = {
                if (!isAdminSubmitting) {
                    adminSubmitError = null
                    selectedGroupeForAdmin = null
                }
            },
            onCreate = { password, nom, prenom, email ->
                isAdminSubmitting = true
                adminSubmitError = null
                onCreateAdminGroupe(groupe.id, password, nom, prenom, email) { success, error ->
                    isAdminSubmitting = false
                    if (success) {
                        selectedGroupeForAdmin = null
                        selectedGroupeForDetail = null
                        actionFeedback = AdminFeedbackMessage("Administrateur du groupe créé avec succès.")
                    } else {
                        adminSubmitError = error
                    }
                }
            },
            isSubmitting = isAdminSubmitting,
            submitError = adminSubmitError
        )
    }

    selectedGroupeForStatusAction?.let { groupe ->
        AdminConfirmationDialog(
            title = if (groupe.isActive) "Suspendre le groupe" else "Réactiver le groupe",
            subtitle = if (groupe.isActive) "Cette action désactivera temporairement l'accès au groupe." else "Cette action rétablira l'accès au groupe.",
            message = if (groupe.isActive) "Voulez-vous vraiment suspendre « ${groupe.nom} » ? Les utilisateurs du groupe ne pourront plus accéder à leurs espaces tant que le groupe restera suspendu." else "Voulez-vous vraiment réactiver « ${groupe.nom} » ? Les utilisateurs du groupe retrouveront immédiatement l'accès à leurs espaces.",
            confirmLabel = if (groupe.isActive) "Suspendre" else "Réactiver",
            onDismiss = {
                if (!isStatusSubmitting) {
                    statusError = null
                    selectedGroupeForStatusAction = null
                }
            },
            onConfirm = {
                isStatusSubmitting = true
                statusError = null
                onToggleGroupeStatus(groupe.id, !groupe.isActive) { success, error ->
                    isStatusSubmitting = false
                    if (success) {
                        selectedGroupeForStatusAction = null
                        selectedGroupeForDetail = null
                        actionFeedback = AdminFeedbackMessage(if (groupe.isActive) "Groupe suspendu avec succès." else "Groupe réactivé avec succès.")
                    } else {
                        statusError = error
                    }
                }
            },
            confirmContainerColor = if (groupe.isActive) Color(0xFFB45309) else Color(0xFF15803D),
            isSubmitting = isStatusSubmitting,
            errorMessage = statusError
        )
    }
    // ── Delete confirmation ──
    selectedGroupeForDelete?.let { groupe ->
        AdminConfirmationDialog(
            title = "Supprimer le groupe",
            subtitle = "Cette action est irréversible et supprimera les données associées.",
            message = "Voulez-vous vraiment supprimer « ${groupe.nom} » ? Cette suppression retirera définitivement le groupe de la plateforme.",
            confirmLabel = "Supprimer définitivement",
            onDismiss = {
                if (!isDeleteSubmitting) {
                    deleteError = null
                    selectedGroupeForDelete = null
                }
            },
            onConfirm = {
                isDeleteSubmitting = true
                deleteError = null
                onDeleteGroupe(groupe.id) { success, error ->
                    isDeleteSubmitting = false
                    if (success) {
                        selectedGroupeForDelete = null
                        selectedGroupeForDetail = null
                        actionFeedback = AdminFeedbackMessage("Groupe supprimé avec succès.")
                    } else {
                        deleteError = error
                    }
                }
            },
            confirmContainerColor = Color(0xFFB3261E),
            isSubmitting = isDeleteSubmitting,
            errorMessage = deleteError
        )
    }
}
