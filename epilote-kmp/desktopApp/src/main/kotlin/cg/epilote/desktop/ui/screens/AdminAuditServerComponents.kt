package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.AuditEventApiDto
import cg.epilote.desktop.ui.screens.superadmin.KpiCard
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.EpiloteTextMuted

/**
 * Composants UI dédiés à la version « vrai backend » du journal d'audit
 * (cloud-only, paginé, filtré côté serveur). Voir [AdminAuditScreen].
 */

private val AuditCategories = listOf(
    "all" to "Toutes catégories",
    "auth" to "Authentification",
    "groupe" to "Groupes scolaires",
    "admin" to "Administrateurs",
    "subscription" to "Abonnements",
    "invoice" to "Factures",
    "payment" to "Paiements",
    "platform" to "Plateforme",
    "system" to "Système"
)

private val AuditOutcomes = listOf(
    "all" to "Tous résultats",
    "success" to "Succès",
    "failure" to "Échec",
    "denied" to "Refusé"
)

@Composable
internal fun AuditServerKpiRow(
    totalEntries: Long,
    shownOnPage: Int,
    authCount: Int,
    failureCount: Int,
    financeCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Receipt,
            iconBg = Color(0xFFE0F2FE),
            iconTint = Color(0xFF0EA5E9),
            label = "Événements totaux",
            value = totalEntries.toString(),
            trendLabel = "$shownOnPage sur cette page"
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Security,
            iconBg = Color(0xFFDCFCE7),
            iconTint = Color(0xFF16A34A),
            label = "Authentification",
            value = authCount.toString(),
            trendLabel = "Connexions et MDP"
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Warning,
            iconBg = Color(0xFFFEE2E2),
            iconTint = Color(0xFFDC2626),
            label = "Échecs",
            value = failureCount.toString(),
            trendLabel = "À surveiller"
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.AdminPanelSettings,
            iconBg = Color(0xFFFEF3C7),
            iconTint = Color(0xFFD97706),
            label = "Facturation",
            value = financeCount.toString(),
            trendLabel = "Factures + paiements"
        )
    }
}

@Composable
internal fun AuditServerToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    selectedOutcome: String,
    onOutcomeChange: (String) -> Unit,
    totalResults: Long,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("Rechercher (acteur, action, message…)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                AuditDropdownPicker(
                    label = "Catégorie",
                    options = AuditCategories,
                    selectedKey = selectedCategory,
                    onSelect = onCategoryChange
                )
                AuditDropdownPicker(
                    label = "Résultat",
                    options = AuditOutcomes,
                    selectedKey = selectedOutcome,
                    onSelect = onOutcomeChange
                )
                OutlinedButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Actualiser")
                }
            }
            Text(
                text = "$totalResults événement(s) correspondant(s) — pagination 50 / page.",
                fontSize = 12.sp,
                color = EpiloteTextMuted
            )
        }
    }
}

@Composable
private fun AuditDropdownPicker(
    label: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedKey }?.second ?: label
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedLabel)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, lbl) ->
                DropdownMenuItem(
                    text = { Text(lbl) },
                    onClick = {
                        onSelect(key)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun AuditServerEntriesPanel(
    entries: List<AuditEventApiDto>,
    onOpen: (AuditEventApiDto) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            entries.forEachIndexed { idx, entry ->
                AuditServerEntryRow(entry = entry, onClick = { onOpen(entry) })
                if (idx < entries.lastIndex) {
                    HorizontalDivider(color = Color(0x14000000))
                }
            }
        }
    }
}

@Composable
private fun AuditServerEntryRow(entry: AuditEventApiDto, onClick: () -> Unit) {
    val outcomeColor = when (entry.outcome.lowercase()) {
        "success" -> Color(0xFF1B5E20)
        "failure" -> Color(0xFFB71C1C)
        "denied" -> Color(0xFFE65100)
        else -> EpiloteTextMuted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(outcomeColor, RoundedCornerShape(4.dp))
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.actionLabel.ifBlank { entry.action }, fontWeight = FontWeight.SemiBold)
                Text("[${entry.category}]", fontSize = 12.sp, color = EpiloteTextMuted)
            }
            Text(
                text = entry.message ?: entry.action,
                fontSize = 12.sp,
                color = EpiloteTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatDate(entry.timestamp), fontSize = 12.sp, color = EpiloteTextMuted)
            Text(entry.actorEmail ?: entry.actorId ?: "—", fontSize = 11.sp, color = EpiloteTextMuted)
        }
    }
}

@Composable
internal fun AuditServerEntryDetailDialog(
    entry: AuditEventApiDto,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
        title = { Text(entry.actionLabel.ifBlank { entry.action }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LineKv("Date", formatDate(entry.timestamp))
                LineKv("Catégorie", entry.category)
                LineKv("Résultat", entry.outcome)
                LineKv("Acteur", entry.actorEmail ?: entry.actorId ?: "—")
                entry.actorRole?.let { LineKv("Rôle", it) }
                entry.targetType?.let { LineKv("Cible", "${entry.targetType} ${entry.targetId ?: ""}") }
                entry.ipAddress?.let { LineKv("IP", it) }
                entry.userAgent?.let { LineKv("User-Agent", it) }
                entry.message?.let { LineKv("Message", it) }
                if (entry.metadata.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Métadonnées", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    entry.metadata.forEach { (k, v) ->
                        LineKv(k, v.toString())
                    }
                }
            }
        }
    )
}

@Composable
private fun LineKv(key: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$key :", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.width(100.dp))
        Text(value, fontSize = 12.sp, color = EpiloteTextMuted)
    }
}
