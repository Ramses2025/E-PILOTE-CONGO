package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
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
import cg.epilote.desktop.data.CreateAdminUserDto
import cg.epilote.desktop.data.UpdateAdminUserDto
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted

@Composable
fun AdminAdminsScreen(
    admins: List<AdminUserDto>,
    groupes: List<GroupeDto>,
    isLoading: Boolean,
    onCreateAdmin: (CreateAdminUserDto, (Boolean, String?) -> Unit) -> Unit,
    onUpdateAdmin: (String, UpdateAdminUserDto, (Boolean, String?) -> Unit) -> Unit,
    onDeleteAdmin: (String, (Boolean, String?) -> Unit) -> Unit,
    onToggleAdminStatus: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onRefresh: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedAdminForDetail by remember { mutableStateOf<AdminUserDto?>(null) }
    var selectedAdminForEdit by remember { mutableStateOf<AdminUserDto?>(null) }
    var selectedAdminForStatusAction by remember { mutableStateOf<AdminUserDto?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("all") }
    var filterRole by remember { mutableStateOf("all") }
    var filterGroupId by remember { mutableStateOf("all") }
    var sortBy by remember { mutableStateOf("recent") }
    var viewMode by remember { mutableStateOf("card") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var isDeleteSubmitting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var isStatusSubmitting by remember { mutableStateOf(false) }
    var statusError by remember { mutableStateOf<String?>(null) }
    var actionFeedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }
    val scrollState = rememberScrollState()
    val groupOptions = listOf("all" to "Tous les groupes") + groupes
        .sortedBy { it.nom.lowercase() }
        .map { it.id to it.nom }

    val handleUpdateAdmin: (String, UpdateAdminUserDto, (Boolean, String?) -> Unit) -> Unit = { userId, dto, onResult ->
        isSubmitting = true
        submitError = null
        onUpdateAdmin(userId, dto) { ok, err ->
            isSubmitting = false
            if (ok) {
                actionFeedback = AdminFeedbackMessage("Administrateur mis à jour avec succès")
                submitError = null
            } else {
                submitError = err
            }
            onResult(ok, err)
        }
    }

    val handleDeleteAdmin: (String, (Boolean, String?) -> Unit) -> Unit = { userId, onResult ->
        isDeleteSubmitting = true
        deleteError = null
        onDeleteAdmin(userId) { ok, err ->
            isDeleteSubmitting = false
            if (ok) {
                actionFeedback = AdminFeedbackMessage("Administrateur supprimé avec succès")
                deleteError = null
            } else {
                deleteError = err
            }
            onResult(ok, err)
        }
    }

    val handleToggleAdminStatus: (String, String, (Boolean, String?) -> Unit) -> Unit = { userId, status, onResult ->
        isStatusSubmitting = true
        statusError = null
        onToggleAdminStatus(userId, status) { ok, err ->
            isStatusSubmitting = false
            if (ok) {
                val label = if (status == "active") "réactivé" else "suspendu"
                actionFeedback = AdminFeedbackMessage("Administrateur $label avec succès")
                statusError = null
            } else {
                statusError = err
            }
            onResult(ok, err)
        }
    }

    val filtered = admins
        .filter { a ->
            val matchSearch = searchQuery.isBlank() ||
                "${a.firstName} ${a.lastName}".contains(searchQuery, ignoreCase = true) ||
                a.email.contains(searchQuery, ignoreCase = true) ||
                (a.phone?.contains(searchQuery, ignoreCase = true) == true)
            val matchStatus = filterStatus == "all" || a.status == filterStatus
            val matchRole = filterRole == "all" || a.role.uppercase() == filterRole.uppercase()
            val matchGroup = filterGroupId == "all" || a.groupId == filterGroupId
            matchSearch && matchStatus && matchRole && matchGroup
        }
        .let { list ->
            when (sortBy) {
                "recent" -> list.sortedByDescending { it.createdAt }
                "name" -> list.sortedBy { "${it.firstName} ${it.lastName}".lowercase() }
                "lastLogin" -> list.sortedByDescending { it.lastLoginAt ?: 0L }
                else -> list
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Header ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Administrateurs", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Gestion des comptes admin groupe et super admin", fontSize = 13.sp, color = EpiloteTextMuted)
            }
        }

        // ── Feedback ───────────────────────────────────────────
        actionFeedback?.let { feedback ->
            AdminFeedbackBanner(feedback = feedback, onDismiss = { actionFeedback = null })
        }

        // ── Loading ────────────────────────────────────────────
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        }

        // ── KPI Row ────────────────────────────────────────────
        AdminKpiRow(
            totalAdmins = admins.size,
            adminGroupeCount = admins.count { it.role.uppercase() == "ADMIN_GROUPE" },
            superAdminCount = admins.count { it.role.uppercase() == "SUPER_ADMIN" },
            activeCount = admins.count { it.status == "active" }
        )

        // ── Toolbar ────────────────────────────────────────────
        AdminToolbar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            filterStatus = filterStatus,
            onFilterStatusChange = { filterStatus = it },
            filterRole = filterRole,
            onFilterRoleChange = { filterRole = it },
            filterGroupId = filterGroupId,
            onFilterGroupChange = { filterGroupId = it },
            groupOptions = groupOptions,
            sortBy = sortBy,
            onSortChange = { sortBy = it },
            viewMode = viewMode,
            onViewModeChange = { viewMode = it },
            onRefresh = onRefresh,
            onCreateAdmin = { showCreateDialog = true },
            totalResults = filtered.size
        )

        // ── Empty state ───────────────────────────────────────
        if (filtered.isEmpty() && !isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.People, null, tint = EpiloteTextMuted, modifier = Modifier.height(48.dp))
                        Text("Aucun administrateur", fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
                        Text(
                            if (searchQuery.isNotBlank() || filterStatus != "all" || filterRole != "all" || filterGroupId != "all") {
                                "Aucun administrateur ne correspond aux filtres actuels"
                            } else {
                                "Les administrateurs apparaîtront ici après création"
                            },
                            fontSize = 12.sp,
                            color = EpiloteTextMuted
                        )
                    }
                }
            }
        }

        when (viewMode) {
            "table" -> AdminTableView(
                admins = filtered,
                groupes = groupes,
                onViewDetail = { selectedAdminForDetail = it },
                onEdit = { selectedAdminForEdit = it },
                onToggleStatus = { selectedAdminForStatusAction = it }
            )
            else -> AdminCardGrid(
                admins = filtered,
                groupes = groupes,
                onViewDetail = { selectedAdminForDetail = it }
            )
        }

        Spacer(Modifier.height(16.dp))
    }

    // ── Create Dialog ──────────────────────────────────────────
    if (showCreateDialog) {
        CreateAdminDialog(
            groupes = groupes,
            isSubmitting = isSubmitting,
            errorMessage = submitError,
            onDismiss = {
                showCreateDialog = false
                submitError = null
            },
            onCreate = { dto ->
                isSubmitting = true
                submitError = null
                onCreateAdmin(dto) { ok, err ->
                    isSubmitting = false
                    if (ok) {
                        showCreateDialog = false
                        actionFeedback = AdminFeedbackMessage("Administrateur créé avec succès")
                    } else {
                        submitError = err
                    }
                }
            }
        )
    }

    selectedAdminForEdit?.let { admin ->
        EditAdminDialog(
            admin = admin,
            groupes = groupes,
            isSubmitting = isSubmitting,
            errorMessage = submitError,
            onDismiss = { selectedAdminForEdit = null },
            onUpdate = { userId, dto, onResult ->
                handleUpdateAdmin(userId, dto) { ok, err ->
                    if (ok) {
                        selectedAdminForEdit = null
                    }
                    onResult(ok, err)
                }
            }
        )
    }

    selectedAdminForStatusAction?.let { admin ->
        val nextStatus = if (admin.status == "active") "suspended" else "active"
        val actionLabel = if (admin.status == "active") "Suspendre" else "Réactiver"
        AdminConfirmationDialog(
            title = "$actionLabel l'administrateur",
            subtitle = "Changement de statut",
            message = if (admin.status == "active") {
                "Voulez-vous suspendre le compte de ${admin.firstName} ${admin.lastName} ? Il ne pourra plus se connecter."
            } else {
                "Voulez-vous réactiver le compte de ${admin.firstName} ${admin.lastName} ? Il pourra à nouveau se connecter."
            },
            confirmLabel = actionLabel,
            onDismiss = { selectedAdminForStatusAction = null },
            onConfirm = {
                handleToggleAdminStatus(admin.id, nextStatus) { ok, _ ->
                    if (ok) {
                        selectedAdminForStatusAction = null
                    }
                }
            },
            confirmContainerColor = if (admin.status == "active") Color(0xFFD97706) else Color(0xFF059669),
            isSubmitting = isStatusSubmitting,
            errorMessage = statusError
        )
    }

    selectedAdminForDetail?.let { admin ->
        AdminDetailDialog(
            admin = admin,
            groupes = groupes,
            isSubmitting = isSubmitting,
            submitError = submitError,
            isDeleteSubmitting = isDeleteSubmitting,
            deleteError = deleteError,
            isStatusSubmitting = isStatusSubmitting,
            statusError = statusError,
            onDismiss = { selectedAdminForDetail = null },
            onEdit = handleUpdateAdmin,
            onDelete = handleDeleteAdmin,
            onToggleStatus = handleToggleAdminStatus
        )
    }
}
