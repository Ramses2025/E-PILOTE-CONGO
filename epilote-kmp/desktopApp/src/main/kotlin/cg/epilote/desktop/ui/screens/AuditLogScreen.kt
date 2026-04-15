package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class AuditEntry(
    val id: String,
    val timestamp: Long,
    val user: String,
    val role: String,
    val action: String,
    val entity: String,
    val entityId: String,
    val details: String,
    val status: String = "success"
)

private val mockAuditEntries = listOf(
    AuditEntry("1", System.currentTimeMillis() - 60_000, "Super Administrateur", "SUPER_ADMIN", "Création groupe", "Groupe Scolaire", "grp-001", "Groupe 'Lycée Savorgnan de Brazza' créé à Brazzaville"),
    AuditEntry("2", System.currentTimeMillis() - 120_000, "Super Administrateur", "SUPER_ADMIN", "Modification plan", "Plan", "plan-002", "Plan 'Premium' mis à jour : 26 → 28 modules"),
    AuditEntry("3", System.currentTimeMillis() - 300_000, "Super Administrateur", "SUPER_ADMIN", "Émission facture", "Facture", "inv-012", "Facture #F-2026-012 émise : 150 000 XAF"),
    AuditEntry("4", System.currentTimeMillis() - 600_000, "Jean Makaya", "ADMIN_GROUPE", "Ajout école", "École", "sch-045", "École 'EP Les Aiglons' ajoutée au groupe Pointe-Noire"),
    AuditEntry("5", System.currentTimeMillis() - 900_000, "Super Administrateur", "SUPER_ADMIN", "Paiement facture", "Facture", "inv-010", "Facture #F-2026-010 marquée payée : 350 000 XAF"),
    AuditEntry("6", System.currentTimeMillis() - 1_800_000, "Super Administrateur", "SUPER_ADMIN", "Login", "Session", "sess-099", "Connexion réussie depuis 192.168.1.10"),
    AuditEntry("7", System.currentTimeMillis() - 3_600_000, "Marie Loutaya", "ADMIN_GROUPE", "Création utilisateur", "Utilisateur", "usr-078", "Directeur 'Paul Mbemba' créé pour EP Dolisie Centre"),
    AuditEntry("8", System.currentTimeMillis() - 7_200_000, "Super Administrateur", "SUPER_ADMIN", "Activation module", "Module", "mod-015", "Module 'Cahier de textes' activé pour le plan Gratuit"),
    AuditEntry("9", System.currentTimeMillis() - 14_400_000, "Super Administrateur", "SUPER_ADMIN", "Suspension abonnement", "Abonnement", "sub-007", "Abonnement groupe Owando suspendu (impayé)", "warning"),
    AuditEntry("10", System.currentTimeMillis() - 28_800_000, "Super Administrateur", "SUPER_ADMIN", "Annonce globale", "Annonce", "ann-003", "Annonce 'Maintenance 20 avril' publiée"),
    AuditEntry("11", System.currentTimeMillis() - 43_200_000, "Pierre Ngoma", "ADMIN_GROUPE", "Modification profil", "Profil", "prf-011", "Profil 'Enseignant' mis à jour : +module discipline"),
    AuditEntry("12", System.currentTimeMillis() - 86_400_000, "Super Administrateur", "SUPER_ADMIN", "Logout", "Session", "sess-098", "Déconnexion manuelle"),
)

private val actionTypes = listOf("Tous", "Création groupe", "Modification plan", "Émission facture", "Paiement facture", "Login", "Logout", "Création utilisateur", "Activation module", "Suspension abonnement", "Annonce globale")
private val roleFilters = listOf("Tous", "SUPER_ADMIN", "ADMIN_GROUPE", "USER")

@Composable
fun AuditLogScreen() {
    var selectedRole by remember { mutableStateOf("Tous") }
    var selectedAction by remember { mutableStateOf("Tous") }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = mockAuditEntries.filter { entry ->
        val roleOk = selectedRole == "Tous" || entry.role == selectedRole
        val actionOk = selectedAction == "Tous" || entry.action == selectedAction
        val searchOk = searchQuery.isBlank() ||
                entry.user.contains(searchQuery, ignoreCase = true) ||
                entry.details.contains(searchQuery, ignoreCase = true) ||
                entry.entity.contains(searchQuery, ignoreCase = true)
        roleOk && actionOk && searchOk
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Journal d'activité", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("Traçabilité complète des actions sur la plateforme", fontSize = 13.sp, color = EpiloteTextMuted)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF2A9D8F).copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, null, tint = Color(0xFF2A9D8F), modifier = Modifier.size(16.dp))
                    Text("${filtered.size} entrées", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2A9D8F))
                }
            }
        }

        // ── Filters ─────────────────────────────────────────
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FilterList, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher…", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) }
                )

                FilterChipRow("Rôle", roleFilters, selectedRole) { selectedRole = it }
                FilterChipRow("Action", actionTypes.take(5), selectedAction) { selectedAction = it }
            }
        }

        // ── Table ───────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column {
                // Table header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableHeader("Date / Heure", Modifier.weight(1.2f))
                    TableHeader("Utilisateur", Modifier.weight(1.2f))
                    TableHeader("Rôle", Modifier.weight(0.8f))
                    TableHeader("Action", Modifier.weight(1f))
                    TableHeader("Entité", Modifier.weight(0.8f))
                    TableHeader("Détails", Modifier.weight(2f))
                    TableHeader("Statut", Modifier.weight(0.6f))
                }

                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                // Table rows
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    filtered.forEachIndexed { index, entry ->
                        AuditRow(entry, index % 2 == 0)
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF64748B),
        maxLines = 1
    )
}

@Composable
private fun AuditRow(entry: AuditEntry, isEven: Boolean) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }
    val bg = if (isEven) Color.White else Color(0xFFFAFBFC)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            dateFormat.format(Date(entry.timestamp)),
            modifier = Modifier.weight(1.2f),
            fontSize = 11.sp, color = Color(0xFF334155)
        )
        Text(
            entry.user,
            modifier = Modifier.weight(1.2f),
            fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        RoleBadge(entry.role, Modifier.weight(0.8f))
        Text(
            entry.action,
            modifier = Modifier.weight(1f),
            fontSize = 11.sp, color = Color(0xFF334155),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(
            entry.entity,
            modifier = Modifier.weight(0.8f),
            fontSize = 11.sp, color = Color(0xFF64748B),
            maxLines = 1
        )
        Text(
            entry.details,
            modifier = Modifier.weight(2f),
            fontSize = 11.sp, color = Color(0xFF64748B),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        StatusBadgeAudit(entry.status, Modifier.weight(0.6f))
    }
}

@Composable
private fun RoleBadge(role: String, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (role) {
        "SUPER_ADMIN"  -> Triple(Color(0xFFE3F7EF), Color(0xFF2A9D8F), "Super Admin")
        "ADMIN_GROUPE" -> Triple(Color(0xFFE8E3FF), Color(0xFF6C5CE7), "Admin Groupe")
        else           -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "Utilisateur")
    }
    Box(modifier = modifier) {
        Surface(shape = RoundedCornerShape(4.dp), color = bg) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = fg,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

@Composable
private fun StatusBadgeAudit(status: String, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (status) {
        "success" -> Triple(Color(0xFFE3F7EF), Color(0xFF2A9D8F), "OK")
        "warning" -> Triple(Color(0xFFFFF7ED), Color(0xFFD97706), "Alerte")
        "error"   -> Triple(Color(0xFFFEF2F2), Color(0xFFDE350B), "Erreur")
        else      -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "—")
    }
    Box(modifier = modifier) {
        Surface(shape = RoundedCornerShape(4.dp), color = bg) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = fg,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

@Composable
private fun FilterChipRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
        options.forEach { option ->
            val isActive = selected == option
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isActive) Color(0xFF2A9D8F).copy(alpha = 0.12f) else Color(0xFFF1F5F9),
                modifier = Modifier.clickable { onSelect(option) }
            ) {
                Text(
                    if (option.length > 15) option.take(12) + "…" else option,
                    fontSize = 10.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isActive) Color(0xFF2A9D8F) else Color(0xFF64748B),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
