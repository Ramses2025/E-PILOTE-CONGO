package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.screens.superadmin.KpiCard
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import cg.epilote.desktop.ui.theme.hoverScale

@Composable
internal fun AuditKpiRow(
    totalEntries: Int,
    warningCount: Int,
    securityCount: Int,
    financeCount: Int,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 980.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.History, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF2563EB), label = "Événements", value = "$totalEntries", trendLabel = "traces consolidées")
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Filled.FactCheck, iconBg = Color(0xFFFFEDD5), iconTint = Color(0xFFD97706), label = "Alertes", value = "$warningCount", trendLabel = "à surveiller")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Security, iconBg = Color(0xFFEDE9FE), iconTint = Color(0xFF6D28D9), label = "Sécurité", value = "$securityCount", trendLabel = "accès et contrôles")
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.AdminPanelSettings, iconBg = Color(0xFFFEE2E2), iconTint = Color(0xFFDC2626), label = "Finances", value = "$financeCount", trendLabel = "factures et abonnements")
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.History, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF2563EB), label = "Événements", value = "$totalEntries", trendLabel = "traces consolidées")
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Filled.FactCheck, iconBg = Color(0xFFFFEDD5), iconTint = Color(0xFFD97706), label = "Alertes", value = "$warningCount", trendLabel = "à surveiller")
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Security, iconBg = Color(0xFFEDE9FE), iconTint = Color(0xFF6D28D9), label = "Sécurité", value = "$securityCount", trendLabel = "accès et contrôles")
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.AdminPanelSettings, iconBg = Color(0xFFFEE2E2), iconTint = Color(0xFFDC2626), label = "Finances", value = "$financeCount", trendLabel = "factures et abonnements")
            }
        }
    }
}

@Composable
internal fun AuditToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedRole: String,
    onRoleChange: (String) -> Unit,
    selectedSource: String,
    onSourceChange: (String) -> Unit,
    selectedStatus: String,
    onStatusChange: (String) -> Unit,
    totalResults: Int,
    onRefresh: () -> Unit
) {
    var roleExpanded by remember { mutableStateOf(false) }
    var sourceExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
    val roleOptions = listOf("all" to "Tous les rôles", "SUPER_ADMIN" to "Super-admin", "ADMIN_GROUPE" to "Admin groupe", "SYSTEM" to "Système")
    val sourceOptions = listOf(
        "all" to "Toutes les sources",
        "invoice" to "Facturation",
        "subscription" to "Abonnements",
        "group" to "Groupes",
        "admin" to "Administrateurs",
        "security" to "Sécurité",
        "access" to "Accès",
        "announcement" to "Annonces",
        "message" to "Messagerie"
    )
    val statusOptions = listOf("all" to "Tous les statuts", "success" to "OK", "warning" to "Alerte", "error" to "Erreur", "info" to "Info")

    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Rechercher un événement") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(14.dp)
                )
                AuditDropdown(label = roleOptions.firstOrNull { it.first == selectedRole }?.second ?: "Rôle", expanded = roleExpanded, onExpandedChange = { roleExpanded = it }) {
                    roleOptions.forEach { (key, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = {
                            onRoleChange(key)
                            roleExpanded = false
                        })
                    }
                }
                AuditDropdown(label = sourceOptions.firstOrNull { it.first == selectedSource }?.second ?: "Source", expanded = sourceExpanded, onExpandedChange = { sourceExpanded = it }) {
                    sourceOptions.forEach { (key, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = {
                            onSourceChange(key)
                            sourceExpanded = false
                        })
                    }
                }
                AuditDropdown(label = statusOptions.firstOrNull { it.first == selectedStatus }?.second ?: "Statut", expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                    statusOptions.forEach { (key, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = {
                            onStatusChange(key)
                            statusExpanded = false
                        })
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("$totalResults entrée(s)", fontSize = 12.sp, color = EpiloteTextMuted)
                OutlinedButton(onClick = onRefresh, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Text("Actualiser", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun AuditDropdown(
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        OutlinedButton(onClick = { onExpandedChange(true) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(12.dp)) {
            Text(label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }, content = { content() })
    }
}

@Composable
internal fun AuditEntriesPanel(entries: List<AdminAuditEntry>, onOpen: (AdminAuditEntry) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 1040.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                entries.forEach { entry ->
                    AuditEntryCard(entry = entry, onOpen = { onOpen(entry) })
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AuditHeader("Date", Modifier.weight(1.1f))
                        AuditHeader("Acteur", Modifier.weight(1.2f))
                        AuditHeader("Action", Modifier.weight(1.1f))
                        AuditHeader("Source", Modifier.weight(0.9f))
                        AuditHeader("Détails", Modifier.weight(2.2f))
                        AuditHeader("Statut", Modifier.weight(0.7f))
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Column {
                        entries.forEachIndexed { index, entry ->
                            Surface(onClick = { onOpen(entry) }, modifier = Modifier.fillMaxWidth().cursorHand().hoverScale(1.002f), color = Color.Transparent) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(if (index % 2 == 0) Color.White else Color(0xFFFAFBFC)).padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(formatDate(entry.timestamp), modifier = Modifier.weight(1.1f), fontSize = 11.sp, color = Color(0xFF334155))
                                    Column(modifier = Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(entry.actor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(entry.role, fontSize = 10.sp, color = EpiloteTextMuted, maxLines = 1)
                                    }
                                    Text(entry.action, modifier = Modifier.weight(1.1f), fontSize = 11.sp, color = Color(0xFF334155), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    AuditBadge(auditSourceLabel(entry.sourceType), notificationSourceColor(entry.sourceType), Modifier.weight(0.9f))
                                    Text(entry.details, modifier = Modifier.weight(2.2f), fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    AuditBadge(auditStatusLabel(entry.status), auditStatusColor(entry.status), Modifier.weight(0.7f))
                                }
                            }
                            if (index < entries.lastIndex) HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditHeader(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B), maxLines = 1)
}

@Composable
private fun AuditBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.10f)) {
            Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
private fun AuditEntryCard(entry: AdminAuditEntry, onOpen: () -> Unit) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().cursorHand().hoverScale(1.005f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(entry.action, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text(entry.actor, fontSize = 12.sp, color = Color(0xFF1D3557), fontWeight = FontWeight.Medium)
                    Text(formatDate(entry.timestamp), fontSize = 11.sp, color = EpiloteTextMuted)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AuditBadge(auditStatusLabel(entry.status), auditStatusColor(entry.status))
                    AuditBadge(auditSourceLabel(entry.sourceType), notificationSourceColor(entry.sourceType))
                }
            }
            Text(entry.details, fontSize = 12.sp, color = Color(0xFF475569), maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(entry.entity, fontSize = 11.sp, color = EpiloteTextMuted)
                Text(entry.entityId, fontSize = 11.sp, color = Color(0xFF334155), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF1D3557), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
internal fun AuditEntryDetailDialog(entry: AdminAuditEntry, onDismiss: () -> Unit) {
    val scrollState = rememberScrollState()
    AdminDialogWindow(
        title = entry.action,
        subtitle = entry.entity,
        onDismiss = onDismiss,
        size = DpSize(700.dp, 520.dp),
        content = {
            Column(modifier = Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AuditDetailRow("Date", formatDate(entry.timestamp))
                AuditDetailRow("Acteur", entry.actor)
                AuditDetailRow("Rôle", entry.role)
                AuditDetailRow("Source", auditSourceLabel(entry.sourceType))
                AuditDetailRow("Statut", auditStatusLabel(entry.status))
                AuditDetailRow("Entité", entry.entity)
                AuditDetailRow("Identifiant", entry.entityId)
                Text("Détails", fontSize = 12.sp, color = EpiloteTextMuted)
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF8FAFC)) {
                    Text(entry.details, modifier = Modifier.fillMaxWidth().padding(14.dp), fontSize = 13.sp, color = Color(0xFF334155))
                }
            }
        },
        actions = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                Text("Fermer")
            }
        }
    )
}

@Composable
private fun AuditDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = EpiloteTextMuted)
        Text(value, fontSize = 13.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
    }
}
