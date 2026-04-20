package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.UpdateAdminUserDto
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand

@Composable
fun AdminDetailDialog(
    admin: AdminUserDto,
    groupes: List<GroupeDto>,
    isSubmitting: Boolean,
    submitError: String?,
    isDeleteSubmitting: Boolean,
    deleteError: String?,
    isStatusSubmitting: Boolean,
    statusError: String?,
    onDismiss: () -> Unit,
    onEdit: (String, UpdateAdminUserDto, (Boolean, String?) -> Unit) -> Unit,
    onDelete: (String, (Boolean, String?) -> Unit) -> Unit,
    onToggleStatus: (String, String, (Boolean, String?) -> Unit) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val roleColor = adminRoleColor(admin.role)
    val statusColor = adminStatusColor(admin.status)
    val groupName = resolveAdminGroupName(admin, groupes)
    val nextStatus = if (admin.status == "active") "suspended" else "active"
    val statusActionLabel = if (admin.status == "active") "Suspendre" else "Réactiver"

    AdminDialogWindow(
        title = "Détail administrateur",
        subtitle = "${admin.firstName} ${admin.lastName} — ${adminRoleLabel(admin.role)}",
        onDismiss = onDismiss,
        size = DpSize(900.dp, 720.dp),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdminAvatar(
                        avatarData = admin.avatar,
                        firstName = admin.firstName,
                        lastName = admin.lastName,
                        modifier = Modifier.size(72.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${admin.firstName} ${admin.lastName}".trim(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D3557)
                        )
                        Text(admin.email.ifBlank { admin.username }, fontSize = 13.sp, color = EpiloteTextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(8.dp), color = roleColor.copy(alpha = 0.12f)) {
                                Text(
                                    text = adminRoleLabel(admin.role),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = roleColor
                                )
                            }
                            Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.10f)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(6.dp).background(statusColor, RoundedCornerShape(999.dp)))
                                    Text(adminStatusLabel(admin.status), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                                }
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminStatBox("Créé le", formatDate(admin.createdAt), Icons.Default.CalendarMonth, Color(0xFF3B82F6), Modifier.weight(1f))
                    AdminStatBox("Dernière connexion", admin.lastLoginAt?.let(::formatDate) ?: "Jamais", Icons.AutoMirrored.Filled.Login, Color(0xFF059669), Modifier.weight(1f))
                    AdminStatBox("Tentatives", admin.loginAttempts.toString(), Icons.Default.Security, Color(0xFF7C3AED), Modifier.weight(1f))
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AdminInfoSection(
                        title = "Contact",
                        modifier = Modifier.weight(1f),
                        rows = buildList {
                            add("Email" to admin.email)
                            admin.phone?.takeIf { it.isNotBlank() }?.let { add("Téléphone" to it) }
                            admin.address?.takeIf { it.isNotBlank() }?.let { add("Adresse" to it) }
                        }
                    )
                    AdminInfoSection(
                        title = "Identité",
                        modifier = Modifier.weight(1f),
                        rows = buildList {
                            admin.gender?.takeIf { it.isNotBlank() }?.let { add("Genre" to it) }
                            admin.dateOfBirth?.takeIf { it.isNotBlank() }?.let { add("Date de naissance" to it) }
                            admin.birthPlace?.takeIf { it.isNotBlank() }?.let { add("Lieu de naissance" to it) }
                        }
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AdminInfoSection(
                        title = "Affectation",
                        modifier = Modifier.weight(1f),
                        rows = buildList {
                            add("Rôle" to adminRoleLabel(admin.role))
                            add("Groupe" to (groupName ?: "Non affecté"))
                            admin.schoolId?.takeIf { it.isNotBlank() }?.let { add("École" to it) }
                        }
                    )
                    AdminInfoSection(
                        title = "Sécurité",
                        modifier = Modifier.weight(1f),
                        rows = buildList {
                            add("Statut" to adminStatusLabel(admin.status))
                            add("Compte actif" to if (admin.isActive) "Oui" else "Non")
                            add("Changement mdp requis" to if (admin.mustChangePassword) "Oui" else "Non")
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFD)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Actions rapides", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1D3557))
                        Text(
                            "Vous pouvez modifier, suspendre/réactiver ou supprimer ce compte depuis cette fenêtre.",
                            fontSize = 12.sp,
                            color = EpiloteTextMuted
                        )
                    }
                }
            }
        },
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showStatusDialog = true }, shape = RoundedCornerShape(10.dp), modifier = Modifier.cursorHand()) {
                    Icon(if (admin.status == "active") Icons.Default.Block else Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(statusActionLabel, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB3261E)),
                    modifier = Modifier.cursorHand()
                ) {
                    Icon(Icons.Default.DeleteOutline, null, Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Supprimer", fontSize = 13.sp)
                }
                TextButton(onClick = onDismiss, modifier = Modifier.cursorHand()) {
                    Text("Fermer")
                }
                Button(
                    onClick = { showEditDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D3557)),
                    modifier = Modifier.cursorHand()
                ) {
                    Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Modifier", fontSize = 13.sp)
                }
            }
        }
    )

    if (showEditDialog) {
        EditAdminDialog(
            admin = admin,
            groupes = groupes,
            isSubmitting = isSubmitting,
            errorMessage = submitError,
            onDismiss = { showEditDialog = false },
            onUpdate = { userId, dto, onResult ->
                onEdit(userId, dto) { ok, err ->
                    if (ok) {
                        showEditDialog = false
                        onDismiss()
                    }
                    onResult(ok, err)
                }
            }
        )
    }

    if (showDeleteDialog) {
        AdminConfirmationDialog(
            title = "Supprimer l'administrateur",
            subtitle = "Cette action est irréversible",
            message = "Voulez-vous vraiment supprimer le compte de ${admin.firstName} ${admin.lastName} ?",
            confirmLabel = "Supprimer",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                onDelete(admin.id) { ok, _ ->
                    if (ok) {
                        showDeleteDialog = false
                        onDismiss()
                    }
                }
            },
            confirmContainerColor = Color(0xFFDC2626),
            isSubmitting = isDeleteSubmitting,
            errorMessage = deleteError
        )
    }

    if (showStatusDialog) {
        AdminConfirmationDialog(
            title = "$statusActionLabel l'administrateur",
            subtitle = "Changement de statut",
            message = if (admin.status == "active") {
                "Voulez-vous suspendre le compte de ${admin.firstName} ${admin.lastName} ? Il ne pourra plus se connecter."
            } else {
                "Voulez-vous réactiver le compte de ${admin.firstName} ${admin.lastName} ? Il pourra à nouveau se connecter."
            },
            confirmLabel = statusActionLabel,
            onDismiss = { showStatusDialog = false },
            onConfirm = {
                onToggleStatus(admin.id, nextStatus) { ok, _ ->
                    if (ok) {
                        showStatusDialog = false
                        onDismiss()
                    }
                }
            },
            confirmContainerColor = if (admin.status == "active") Color(0xFFD97706) else Color(0xFF059669),
            isSubmitting = isStatusSubmitting,
            errorMessage = statusError
        )
    }
}

@Composable
private fun AdminStatBox(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.06f)) {
        Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)), Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1D3557))
                Text(label, fontSize = 11.sp, color = EpiloteTextMuted)
            }
        }
    }
}

@Composable
private fun AdminInfoSection(title: String, modifier: Modifier, rows: List<Pair<String, String>>) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFD),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6ECF4))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1D3557))
            if (rows.isEmpty()) {
                Text("Aucune information", fontSize = 12.sp, color = EpiloteTextMuted)
            } else {
                rows.forEachIndexed { index, (label, value) ->
                    if (index > 0) {
                        HorizontalDivider(color = Color(0xFFE9EEF5))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(label, fontSize = 12.sp, color = EpiloteTextMuted)
                        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                    }
                }
            }
        }
    }
}
