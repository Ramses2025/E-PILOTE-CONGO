package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroupeDetailDialog(
    groupe: GroupeDto,
    admins: List<UserDto>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit,
    onAddAdmin: () -> Unit
) {
    val planLabel = resolvePlanLabel(groupe.planId)
    val planColor = resolvePlanColor(groupe.planId)
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy 'à' HH:mm", Locale.FRENCH) }
    val scrollState = rememberScrollState()

    val dialogState = rememberDialogState(size = DpSize(900.dp, 720.dp))
  
    DialogWindow(
        onCloseRequest = onDismiss, title = "Détail — ${groupe.nom}",
        state = dialogState,
        undecorated = true, resizable = false
    ) {
        WindowDraggableArea(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(14.dp), color = Color.White) {
                Column(Modifier.fillMaxSize()) {
                    // ── Header ──
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        Arrangement.SpaceBetween, Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            GroupeLogoAvatar(logoData = groupe.logo, modifier = Modifier.size(44.dp), shape = RoundedCornerShape(10.dp))
                            Column {
                                Text(groupe.nom, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D3557))
                                Text(groupe.slug, fontSize = 12.sp, color = EpiloteTextMuted)
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = planColor.copy(alpha = 0.12f)) {
                                Text(planLabel, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = planColor)
                            }
                            val (statusLabel, statusColor) = if (groupe.isActive) "Actif" to Color(0xFF059669) else "Inactif" to Color(0xFFDE350B)
                            Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.10f)) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
                                    Box(Modifier.size(6.dp).background(statusColor, RoundedCornerShape(999.dp)))
                                    Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                                }
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.cursorHand()) { Icon(Icons.Default.Close, "Fermer") }
                    }
                    HorizontalDivider(color = Color(0xFFE9EEF5))

                // ── Content ──
                Column(Modifier.weight(1f).verticalScroll(scrollState).padding(24.dp), Arrangement.spacedBy(20.dp)) {
                    // Stats row
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                        StatBox("Écoles", "${groupe.ecolesCount}", Icons.Default.School, Color(0xFF3B82F6), Modifier.weight(1f))
                        StatBox("Utilisateurs", "${groupe.usersCount}", Icons.Default.People, Color(0xFF7C3AED), Modifier.weight(1f))
                        StatBox("Créé le", if (groupe.createdAt > 0) dateFormat.format(Date(groupe.createdAt)) else "—", Icons.Default.CalendarMonth, Color(0xFF059669), Modifier.weight(1f))
                    }

                    // Info grid
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(16.dp)) {
                        InfoSection("Contact", Modifier.weight(1f), buildList {
                            groupe.email?.let { add("Email" to it) }
                            groupe.phone?.let { add("Téléphone" to it) }
                            groupe.website?.let { add("Site web" to it) }
                        })
                        InfoSection("Localisation", Modifier.weight(1f), buildList {
                            groupe.department?.let { add("Département" to it) }
                            groupe.city?.let { add("Ville" to it) }
                            groupe.address?.let { add("Adresse" to it) }
                            add("Pays" to groupe.country)
                        })
                    }

                    // Details
                    InfoSection("Détails", Modifier.fillMaxWidth(), buildList {
                        groupe.foundedYear?.let { add("Année de fondation" to it.toString()) }
                        add("Plan" to planLabel)
                        groupe.description?.let { add("Description" to it) }
                    })

                    // Admins
                    Column {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Administrateurs du groupe", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1D3557))
                            FilledTonalButton(onClick = onAddAdmin, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.cursorHand()) {
                                Icon(Icons.Default.PersonAdd, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Ajouter", fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (admins.isEmpty()) {
                            Text("Aucun administrateur défini.", fontSize = 13.sp, color = EpiloteTextMuted)
                        } else {
                            admins.forEach { admin ->
                                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF7FAFC)) {
                                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("${admin.firstName} ${admin.lastName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(admin.email.ifBlank { admin.username }, fontSize = 11.sp, color = EpiloteTextMuted)
                                        }
                                        Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFF6C5CE7).copy(alpha = 0.12f)) {
                                            Text("ADMIN", Modifier.padding(horizontal = 10.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6C5CE7))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }

                // ── Actions bar ──
                HorizontalDivider(color = Color(0xFFE9EEF5))
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onToggleStatus, shape = RoundedCornerShape(10.dp), modifier = Modifier.cursorHand()) {
                            Icon(if (groupe.isActive) Icons.Default.Block else Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (groupe.isActive) "Suspendre" else "Réactiver", fontSize = 13.sp)
                        }
                        OutlinedButton(onClick = {
                            onDismiss()
                            onDelete()
                        }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB3261E)), modifier = Modifier.cursorHand()) {
                            Icon(Icons.Default.DeleteOutline, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Supprimer", fontSize = 13.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss, modifier = Modifier.cursorHand()) { Text("Fermer") }
                        Button(onClick = { onDismiss(); onEdit() }, shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D3557)), modifier = Modifier.cursorHand()) {
                            Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Modifier", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

 }

@Composable
private fun StatBox(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.06f)) {
        Row(Modifier.padding(14.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)), Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1D3557))
                Text(label, fontSize = 11.sp, color = EpiloteTextMuted)
            }
        }
    }
}

@Composable
private fun InfoSection(title: String, modifier: Modifier, rows: List<Pair<String, String>>) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = Color(0xFFF8FAFD), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6ECF4))) {
        Column(Modifier.padding(16.dp), Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1D3557))
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(label, fontSize = 13.sp, color = EpiloteTextMuted)
                    Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                }
            }
        }
    }
}
