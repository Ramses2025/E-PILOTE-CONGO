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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.DpSize
import cg.epilote.desktop.ui.theme.cursorHand
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
    "failure" to "Échec"
)

private val AuditActionOptions = listOf(
    "all"                         to "Toutes actions",
    "auth.login.success"          to "Connexion réussie",
    "auth.login.failure"          to "Échec connexion",
    "auth.password.changed"       to "Changement MDP",
    "auth.password.reset"         to "Réinit. MDP (admin)",
    "groupe.created"              to "Création groupe",
    "groupe.updated"              to "Modification groupe",
    "groupe.deleted"              to "Suppression groupe",
    "admin.created"               to "Création admin",
    "admin.updated"               to "Modification admin",
    "admin.deleted"               to "Suppression admin",
    "subscription.created"        to "Création abonnement",
    "subscription.status_changed" to "Statut abonnement",
    "subscription.renewed"        to "Renouvellement abonnement",
    "invoice.created"             to "Émission facture",
    "invoice.deleted"             to "Suppression facture",
    "invoice.status_changed"      to "Statut facture",
    "payment.recorded"            to "Paiement enregistré",
    "payment.deleted"             to "Suppression paiement",
    "platform.identity_updated"   to "Paramètres plateforme",
    "system.scheduler.expiry_run" to "Job expiration"
)

private val PageSizeOptions = listOf(
    "25" to "25 / page",
    "50" to "50 / page",
    "100" to "100 / page"
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
    selectedAction: String,
    onActionChange: (String) -> Unit,
    pageSize: Int,
    onPageSizeChange: (Int) -> Unit,
    totalResults: Long,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("Rechercher (acteur, IP, message…)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(onClick = onRefresh, modifier = Modifier.cursorHand()) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Actualiser")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuditDropdownPicker(label = "Catégorie", options = AuditCategories,    selectedKey = selectedCategory, onSelect = onCategoryChange)
                AuditDropdownPicker(label = "Résultat",  options = AuditOutcomes,      selectedKey = selectedOutcome,  onSelect = onOutcomeChange)
                AuditDropdownPicker(label = "Action",    options = AuditActionOptions, selectedKey = selectedAction,   onSelect = onActionChange)
                Spacer(modifier = Modifier.weight(1f))
                AuditDropdownPicker(
                    label   = "Lignes",
                    options = PageSizeOptions,
                    selectedKey = pageSize.toString(),
                    onSelect    = { onPageSizeChange(it.toIntOrNull() ?: 50) }
                )
                Text(text = "$totalResults événement(s)", fontSize = 12.sp, color = EpiloteTextMuted)
            }
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
        "success" -> Color(0xFF16A34A)
        "failure" -> Color(0xFFDC2626)
        "denied"  -> Color(0xFFE65100)
        else      -> EpiloteTextMuted
    }
    val categoryColor = when (entry.category.lowercase()) {
        "auth"         -> Color(0xFF2563EB)
        "groupe"       -> Color(0xFF7C3AED)
        "admin"        -> Color(0xFF0891B2)
        "subscription" -> Color(0xFF0D9488)
        "invoice"      -> Color(0xFF1D4ED8)
        "payment"      -> Color(0xFF15803D)
        "platform"     -> Color(0xFFB45309)
        else           -> Color(0xFF64748B)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.actionLabel.ifBlank { entry.action }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                AuditPillBadge(entry.category, categoryColor)
            }
            Text(
                text = entry.message ?: entry.action,
                fontSize = 12.sp,
                color = EpiloteTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            entry.ipAddress?.let { Text("IP $it", fontSize = 10.sp, color = Color(0xFF94A3B8)) }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formatDate(entry.timestamp), fontSize = 11.sp, color = EpiloteTextMuted)
            Text(entry.actorEmail ?: entry.actorId ?: "\u2014", fontSize = 11.sp, color = EpiloteTextMuted, maxLines = 1)
            AuditPillBadge(
                text = when (entry.outcome.lowercase()) {
                    "success" -> "Succès"
                    "failure" -> "Échec"
                    "denied"  -> "Refusé"
                    else      -> entry.outcome
                },
                color = outcomeColor
            )
        }
    }
}

@Composable
private fun AuditPillBadge(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
internal fun AuditServerEntryDetailDialog(
    entry: AuditEventApiDto,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    AdminDialogWindow(
        title    = entry.actionLabel.ifBlank { entry.action },
        subtitle = "${entry.category}  \u00b7  ${entry.outcome}",
        onDismiss = onDismiss,
        size = DpSize(760.dp, 580.dp),
        content = {
            Column(modifier = Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LineKv("Date",       formatDate(entry.timestamp))
                LineKv("Catégorie",  entry.category)
                LineKv("Action",     entry.action)
                LineKv("Résultat",   entry.outcome)
                LineKv("Acteur",     entry.actorEmail ?: entry.actorId ?: "\u2014")
                entry.actorRole?.let   { LineKv("Rôle",        it) }
                entry.targetType?.let  { LineKv("Cible",       "${entry.targetType} ${entry.targetId ?: ""}") }
                entry.targetLabel?.let { LineKv("Label cible",  it) }
                entry.ipAddress?.let   { LineKv("IP",           it) }
                entry.userAgent?.let   { LineKv("User-Agent",   it) }
                entry.message?.let     { LineKv("Message",      it) }
                if (entry.metadata.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Métadonnées", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    entry.metadata.forEach { (k, v) ->
                        val display = if (v is kotlinx.serialization.json.JsonPrimitive && v.isString) v.content else v.toString()
                        LineKv(k, display)
                    }
                }
            }
        },
        actions = {
            OutlinedButton(
                onClick = {
                    val actor  = entry.actorEmail ?: entry.actorId ?: ""
                    val ip      = entry.ipAddress ?: ""
                    val message = entry.message ?: ""
                    val json = "{\"id\":\"${entry.id}\",\"timestamp\":${entry.timestamp}" +
                        ",\"action\":\"${entry.action}\",\"category\":\"${entry.category}\"" +
                        ",\"outcome\":\"${entry.outcome}\",\"actor\":\"$actor\"" +
                        ",\"ip\":\"$ip\",\"message\":\"$message\"}"
                    clipboardManager.setText(AnnotatedString(json))
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.cursorHand()
            ) { Text("Copier JSON") }
            TextButton(onClick = onDismiss, modifier = Modifier.cursorHand()) { Text("Fermer") }
        }
    )
}

@Composable
private fun LineKv(key: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$key :", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.width(120.dp))
        Text(value, fontSize = 12.sp, color = EpiloteTextMuted, modifier = Modifier.weight(1f))
    }
}
