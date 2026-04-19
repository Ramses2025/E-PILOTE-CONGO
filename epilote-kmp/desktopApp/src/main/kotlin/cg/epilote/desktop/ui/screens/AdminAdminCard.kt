package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand

internal fun adminRoleColor(role: String): Color = when (role.uppercase()) {
    "SUPER_ADMIN" -> Color(0xFFE63946)
    "ADMIN_GROUPE" -> Color(0xFF6C5CE7)
    else -> Color(0xFF94A3B8)
}

internal fun adminRoleLabel(role: String): String = when (role.uppercase()) {
    "SUPER_ADMIN" -> "Super Admin"
    "ADMIN_GROUPE" -> "Admin Groupe"
    else -> role
}

internal fun adminStatusLabel(status: String): String = when (status) {
    "active" -> "Actif"
    "suspended" -> "Suspendu"
    "locked" -> "Verrouillé"
    else -> status
}

internal fun adminStatusColor(status: String): Color = when (status) {
    "active" -> Color(0xFF059669)
    "suspended" -> Color(0xFFD97706)
    "locked" -> Color(0xFFDC2626)
    else -> Color(0xFF94A3B8)
}

@Composable
fun AdminAdminCard(
    admin: AdminUserDto,
    groupes: List<GroupeDto>,
    isSubmitting: Boolean,
    submitError: String?,
    isDeleteSubmitting: Boolean,
    deleteError: String?,
    isStatusSubmitting: Boolean,
    statusError: String?,
    onEdit: (String, UpdateAdminUserDto) -> Unit,
    onDelete: (String, (Boolean, String?) -> Unit) -> Unit,
    onToggleStatus: (String, String, (Boolean, String?) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }

    val roleColor = adminRoleColor(admin.role)
    val statusColor = adminStatusColor(admin.status)
    val groupName = admin.groupId?.let { gid -> groupes.find { it.id == gid }?.nom }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // ── Header ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).background(roleColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${admin.firstName.take(1)}${admin.lastName.take(1)}".uppercase(),
                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = roleColor
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${admin.firstName} ${admin.lastName}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1D3557))
                        Text(admin.email, fontSize = 12.sp, color = EpiloteTextMuted)
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = roleColor.copy(alpha = 0.12f)) {
                        Text(adminRoleLabel(admin.role), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = roleColor)
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.12f)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                            Text(adminStatusLabel(admin.status), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // ── Details ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                admin.phone?.let { phone ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = EpiloteTextMuted)
                        Text(phone, fontSize = 12.sp, color = Color(0xFF475569))
                    }
                }
                groupName?.let { gn ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, null, modifier = Modifier.size(14.dp), tint = EpiloteTextMuted)
                        Text(gn, fontSize = 12.sp, color = Color(0xFF475569))
                    }
                }
                if (admin.mustChangePassword) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEF3C7)) {
                        Text("Doit changer le mdp", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF92400E))
                    }
                }
            }

            // ── Actions ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { showEditDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand()
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Modifier", fontSize = 12.sp)
                }
                val statusLabel = if (admin.status == "active") "Suspendre" else "Réactiver"
                val statusIcon = if (admin.status == "active") Icons.Default.Block else Icons.Default.CheckCircle
                val nextStatus = if (admin.status == "active") "suspended" else "active"
                OutlinedButton(
                    onClick = { showStatusDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand()
                ) {
                    Icon(statusIcon, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(statusLabel, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand(),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Supprimer", fontSize = 12.sp)
                }
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────
    if (showEditDialog) {
        EditAdminDialog(
            admin = admin,
            groupes = groupes,
            isSubmitting = isSubmitting,
            errorMessage = submitError,
            onDismiss = { showEditDialog = false },
            onUpdate = onEdit
        )
    }

    if (showDeleteDialog) {
        AdminConfirmationDialog(
            title = "Supprimer l'administrateur",
            subtitle = "Cette action est irréversible",
            message = "Voulez-vous vraiment supprimer le compte de ${admin.firstName} ${admin.lastName} ? Toutes les données associées seront perdues.",
            confirmLabel = "Supprimer",
            onDismiss = { showDeleteDialog = false },
            onConfirm = { onDelete(admin.id) { ok, err -> if (ok) showDeleteDialog = false } },
            confirmContainerColor = Color(0xFFDC2626),
            isSubmitting = isDeleteSubmitting,
            errorMessage = deleteError
        )
    }

    if (showStatusDialog) {
        val nextStatus = if (admin.status == "active") "suspended" else "active"
        val actionLabel = if (admin.status == "active") "Suspendre" else "Réactiver"
        AdminConfirmationDialog(
            title = "$actionLabel l'administrateur",
            subtitle = "Changement de statut",
            message = if (admin.status == "active")
                "Voulez-vous suspendre le compte de ${admin.firstName} ${admin.lastName} ? Il ne pourra plus se connecter."
            else
                "Voulez-vous réactiver le compte de ${admin.firstName} ${admin.lastName} ? Il pourra à nouveau se connecter.",
            confirmLabel = actionLabel,
            onDismiss = { showStatusDialog = false },
            onConfirm = { onToggleStatus(admin.id, nextStatus) { ok, err -> if (ok) showStatusDialog = false } },
            confirmContainerColor = if (admin.status == "active") Color(0xFFD97706) else Color(0xFF059669),
            isSubmitting = isStatusSubmitting,
            errorMessage = statusError
        )
    }
}
