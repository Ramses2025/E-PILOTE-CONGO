package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.*
import cg.epilote.desktop.ui.theme.cursorHand
import cg.epilote.desktop.ui.theme.hoverScale
import cg.epilote.desktop.ui.theme.AnimatedCardEntrance

// ── Groupe Card ────────────────────────────────────────────────

@Composable
fun GroupeCard(groupe: GroupeDto, admins: List<UserDto>, onEdit: () -> Unit, onAddAdmin: () -> Unit, onViewDetail: () -> Unit = {}) {
    val planLabel = when {
        groupe.planId.contains("institutionnel", true) -> "INSTITUTIONNEL"
        groupe.planId.contains("pro", true) -> "PRO"
        groupe.planId.contains("premium", true) -> "PREMIUM"
        else -> "GRATUIT"
    }
    val planColor = when (planLabel) {
        "INSTITUTIONNEL" -> Color(0xFFE76F51)
        "PRO" -> Color(0xFF6C5CE7)
        "PREMIUM" -> Color(0xFFE9C46A)
        else -> Color(0xFF2A9D8F)
    }
    val accentBg = planColor.copy(alpha = 0.04f)

    Card(
        onClick = onViewDetail,
        modifier = Modifier.fillMaxWidth().cursorHand().hoverScale(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = planColor.copy(alpha = 0.03f).let { tint ->
            // Blend white with a very subtle plan tint
            Color(
                red   = (Color.White.red + tint.red * 0.03f).coerceAtMost(1f),
                green = (Color.White.green + tint.green * 0.03f).coerceAtMost(1f),
                blue  = (Color.White.blue + tint.blue * 0.03f).coerceAtMost(1f),
                alpha = 1f
            )
        }),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, planColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Accent top bar ──
            Box(Modifier.fillMaxWidth().height(4.dp).background(planColor))

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // ── Header: logo + name + badges ──
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp), Alignment.CenterVertically) {
                    GroupeLogoAvatar(
                        logoData = groupe.logo,
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            groupe.nom,
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(6.dp), color = planColor.copy(alpha = 0.12f)) {
                                Text(planLabel, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = planColor)
                            }
                            if (!groupe.isActive) {
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDE350B).copy(alpha = 0.12f)) {
                                    Text("INACTIF", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDE350B))
                                }
                            }
                        }
                        Text(
                            buildString {
                                groupe.slug.takeIf { it.isNotBlank() }?.let { append(it) }
                                groupe.department?.takeIf { it.isNotBlank() }?.let { if (isNotEmpty()) append(" · "); append(it) }
                                groupe.city?.takeIf { it.isNotBlank() }?.let { if (isNotEmpty()) append(", "); append(it) }
                            }.ifBlank { "Localisation non renseignée" },
                            fontSize = 11.sp, color = EpiloteTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // ── Stats row ──
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(accentBg).padding(horizontal = 12.dp, vertical = 8.dp),
                    Arrangement.SpaceEvenly, Alignment.CenterVertically
                ) {
                    StatPill("${groupe.ecolesCount}", "écoles", planColor)
                    StatPill("${groupe.usersCount}", "utilisateurs", planColor)
                    StatPill(if (groupe.isActive) "Actif" else "Inactif", "statut", if (groupe.isActive) Color(0xFF059669) else Color(0xFFDE350B))
                }

                // ── Contact row ──
                if (!groupe.email.isNullOrBlank() || !groupe.phone.isNullOrBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        groupe.email?.takeIf { it.isNotBlank() }?.let {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, null, tint = EpiloteTextMuted, modifier = Modifier.size(13.dp))
                                Text(it, fontSize = 11.sp, color = EpiloteTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        groupe.phone?.takeIf { it.isNotBlank() }?.let {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, null, tint = EpiloteTextMuted, modifier = Modifier.size(13.dp))
                                Text(it, fontSize = 11.sp, color = EpiloteTextMuted)
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE9EEF5))

                // ── Admins ──
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Admins du groupe", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF1D3557))
                    if (admins.isEmpty()) {
                        Text("Aucun admin défini.", fontSize = 11.sp, color = EpiloteTextMuted)
                    } else {
                        admins.forEach { admin ->
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF7FAFC)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text("${admin.firstName} ${admin.lastName}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text(admin.email.ifBlank { admin.username }, fontSize = 10.sp, color = EpiloteTextMuted)
                                    }
                                    Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFF6C5CE7).copy(alpha = 0.12f)) {
                                        Text("ADMIN", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6C5CE7))
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Action buttons ──
                Row(Modifier.fillMaxWidth(), Arrangement.End, Alignment.CenterVertically) {
                    FilledTonalButton(onClick = onEdit, shape = RoundedCornerShape(8.dp), modifier = Modifier.cursorHand(),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = planColor.copy(alpha = 0.10f), contentColor = planColor)
                    ) {
                        Icon(Icons.Default.Edit, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Modifier", fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = onAddAdmin, shape = RoundedCornerShape(8.dp), modifier = Modifier.cursorHand(),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = planColor.copy(alpha = 0.10f), contentColor = planColor)
                    ) {
                        Icon(Icons.Default.PersonAdd, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Admin", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
        Text(label, fontSize = 10.sp, color = EpiloteTextMuted)
    }
}

// ── Admin Groupe Dialog ────────────────────────────────────────

@Composable
fun CreateAdminGroupeDialog(
    groupe: GroupeDto,
    onDismiss: () -> Unit,
    onCreate: (password: String, nom: String, prenom: String, email: String) -> Unit,
    isSubmitting: Boolean = false,
    submitError: String? = null
) {
    var password by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val isValid = password.length >= 8 && nom.isNotBlank() && prenom.isNotBlank() && email.isNotBlank() && email.contains("@")

    AdminDialogWindow(
        title = "Nouvel Admin Groupe",
        subtitle = "Créez l'administrateur principal pour ${groupe.nom}",
        onDismiss = onDismiss,
        size = DpSize(620.dp, 460.dp),
        content = {
            Text("Groupe : ${groupe.nom}", fontSize = 12.sp, color = EpiloteTextMuted)
            OutlinedTextField(prenom, { prenom = it }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
            OutlinedTextField(nom, { nom = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
            OutlinedTextField(password, { password = it }, label = { Text("Mot de passe") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
            Text(
                text = if (isValid) "Le compte administrateur peut être créé." else "Tous les champs sont requis et le mot de passe doit contenir au moins 8 caractères.",
                fontSize = 12.sp,
                color = if (isValid) EpiloteTextMuted else Color(0xFFB3261E)
            )
            submitError?.let {
                AdminFeedbackBanner(
                    feedback = AdminFeedbackMessage(it, isError = true),
                    onDismiss = {}
                )
            }
        },
        actions = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting, modifier = Modifier.cursorHand()) {
                Text("Annuler")
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { if (isValid) onCreate(password, nom, prenom, email) },
                enabled = isValid && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.cursorHand()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isSubmitting) "Création…" else "Créer")
            }
        }
    )
}

// ── Responsive Card Grid ───────────────────────────────────────

@Composable
fun GroupeCardGrid(
    groupes: List<GroupeDto>,
    adminGroupesByGroup: Map<String, List<UserDto>>,
    onEdit: (GroupeDto) -> Unit,
    onAddAdmin: (GroupeDto) -> Unit,
    onViewDetail: (GroupeDto) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cols = when {
            maxWidth > 1400.dp -> 4
            maxWidth > 1050.dp -> 3
            maxWidth > 700.dp -> 2
            else -> 1
        }
        val cardWidth = (maxWidth - 16.dp * (cols - 1)) / cols

        val rows = groupes.chunked(cols)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rows.forEachIndexed { rowIndex, rowGroupes ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowGroupes.forEachIndexed { colIndex, groupe ->
                        val globalIndex = rowIndex * cols + colIndex
                        Box(modifier = Modifier.width(cardWidth)) {
                            AnimatedCardEntrance(index = globalIndex) {
                                val admins = adminGroupesByGroup[groupe.id].orEmpty()
                                GroupeCard(
                                    groupe = groupe,
                                    admins = admins,
                                    onEdit = { onEdit(groupe) },
                                    onAddAdmin = { onAddAdmin(groupe) },
                                    onViewDetail = { onViewDetail(groupe) }
                                )
                            }
                        }
                    }
                    // Fill remaining columns with empty boxes for alignment
                    repeat(cols - rowGroupes.size) {
                        Spacer(Modifier.width(cardWidth))
                    }
                }
            }
        }
    }
}
