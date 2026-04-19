package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import cg.epilote.desktop.ui.theme.hoverScale

@Composable
fun GroupeTableView(
    groupes: List<GroupeDto>,
    onEdit: (GroupeDto) -> Unit,
    onAddAdmin: (GroupeDto) -> Unit,
    onViewDetail: (GroupeDto) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCell("Groupe", Modifier.weight(2.5f))
                HeaderCell("Plan", Modifier.weight(1.2f))
                HeaderCell("Localisation", Modifier.weight(1.5f))
                HeaderCell("Contact", Modifier.weight(1.5f))
                HeaderCell("Écoles", Modifier.weight(0.7f))
                HeaderCell("Statut", Modifier.weight(1f))
                HeaderCell("Actions", Modifier.weight(1.5f))
            }
            HorizontalDivider(color = Color(0xFFE2E8F0))

            if (groupes.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    Text("Aucun groupe à afficher", color = EpiloteTextMuted)
                }
            }

            groupes.forEachIndexed { index, groupe ->
                TableRow(groupe, onEdit = { onEdit(groupe) }, onAddAdmin = { onAddAdmin(groupe) }, onViewDetail = { onViewDetail(groupe) })
                if (index < groupes.lastIndex) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(text, modifier, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B), maxLines = 1)
}

@Composable
private fun TableRow(groupe: GroupeDto, onEdit: () -> Unit, onAddAdmin: () -> Unit, onViewDetail: () -> Unit) {
    val planLabel = resolvePlanLabel(groupe.planId)
    val planColor = resolvePlanColor(groupe.planId)

    Surface(
        onClick = onViewDetail,
        modifier = Modifier.fillMaxWidth().cursorHand().hoverScale(1.005f),
        color = Color.Transparent
    ) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(2.5f), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
            GroupeLogoAvatar(logoData = groupe.logo, modifier = Modifier.size(36.dp), shape = RoundedCornerShape(8.dp))
            Column {
                Text(groupe.nom, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(groupe.slug, fontSize = 11.sp, color = EpiloteTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Box(Modifier.weight(1.2f)) {
            Surface(shape = RoundedCornerShape(6.dp), color = planColor.copy(alpha = 0.12f)) {
                Text(planLabel, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = planColor)
            }
        }

        Text(
            text = buildString {
                groupe.department?.takeIf { it.isNotBlank() }?.let { append(it) }
                groupe.city?.takeIf { it.isNotBlank() }?.let { if (isNotEmpty()) append(", "); append(it) }
            }.ifBlank { "—" },
            modifier = Modifier.weight(1.5f), fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1, overflow = TextOverflow.Ellipsis
        )

        Text(
            text = groupe.email?.takeIf { it.isNotBlank() } ?: "—",
            modifier = Modifier.weight(1.5f), fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1, overflow = TextOverflow.Ellipsis
        )

        Text("${groupe.ecolesCount}", Modifier.weight(0.7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D3557))

        Box(Modifier.weight(1f)) {
            val (label, color) = if (groupe.isActive) "Actif" to Color(0xFF059669) else "Inactif" to Color(0xFFDE350B)
            Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.10f)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(color, RoundedCornerShape(999.dp)))
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
                }
            }
        }

        Row(Modifier.weight(1.5f), Arrangement.spacedBy(4.dp)) {
            FilledTonalButton(onClick = { onEdit() }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp), modifier = Modifier.cursorHand()) {
                Icon(Icons.Default.Edit, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Modifier", fontSize = 11.sp)
            }
            FilledTonalButton(onClick = { onAddAdmin() }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp), modifier = Modifier.cursorHand()) {
                Icon(Icons.Default.PersonAdd, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Admin", fontSize = 11.sp)
            }
        }
    }
    }
}

internal fun resolvePlanLabel(planId: String): String = when {
    planId.contains("institutionnel", true) -> "Institutionnel"
    planId.contains("pro", true) -> "Pro"
    planId.contains("premium", true) -> "Premium"
    else -> "Gratuit"
}

internal fun resolvePlanColor(planId: String): Color = when {
    planId.contains("institutionnel", true) -> Color(0xFFE76F51)
    planId.contains("pro", true) -> Color(0xFF6C5CE7)
    planId.contains("premium", true) -> Color(0xFFE9C46A)
    else -> Color(0xFF2A9D8F)
}
