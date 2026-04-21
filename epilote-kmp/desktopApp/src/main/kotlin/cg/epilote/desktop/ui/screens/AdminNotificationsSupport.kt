package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CreditCardOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.screens.superadmin.KpiCard
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.AnimatedCardEntrance
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import cg.epilote.desktop.ui.theme.hoverScale

internal fun notificationSeverityLabel(severity: String): String = when (severity.lowercase()) {
    "critical" -> "Critique"
    "warning" -> "Attention"
    "success" -> "Traité"
    else -> "Information"
}

internal fun notificationSeverityColor(severity: String): Color = when (severity.lowercase()) {
    "critical" -> Color(0xFFDC2626)
    "warning" -> Color(0xFFD97706)
    "success" -> Color(0xFF059669)
    else -> Color(0xFF2563EB)
}

internal fun notificationSourceLabel(sourceType: String): String = when (sourceType.lowercase()) {
    "invoice" -> "Facturation"
    "subscription" -> "Abonnement"
    "group" -> "Groupe"
    "admin" -> "Administrateurs"
    "announcement" -> "Diffusion"
    else -> "Plateforme"
}

internal fun notificationSourceColor(sourceType: String): Color = when (sourceType.lowercase()) {
    "invoice" -> Color(0xFF7C3AED)
    "subscription" -> Color(0xFF2563EB)
    "group" -> Color(0xFF059669)
    "admin" -> Color(0xFFDB2777)
    "announcement" -> Color(0xFFEA580C)
    else -> Color(0xFF475569)
}

internal fun notificationIcon(sourceType: String, severity: String): ImageVector = when (sourceType.lowercase()) {
    "invoice" -> if (severity == "critical") Icons.Default.CreditCardOff else Icons.Default.AddAlert
    "subscription" -> Icons.Default.Widgets
    "group" -> Icons.Default.Groups
    "admin" -> Icons.Default.ManageAccounts
    "announcement" -> Icons.Default.Campaign
    else -> Icons.Default.NotificationsActive
}

@Composable
internal fun NotificationKpiRow(
    totalAlerts: Int,
    criticalAlerts: Int,
    actionRequired: Int,
    broadcastCount: Int,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 980.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.NotificationsActive, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF2563EB), label = "Notifications", value = "$totalAlerts", trendLabel = "$criticalAlerts critiques")
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.WarningAmber, iconBg = Color(0xFFFEE2E2), iconTint = Color(0xFFDC2626), label = "Actions requises", value = "$actionRequired", trendLabel = "à traiter")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Campaign, iconBg = Color(0xFFFFEDD5), iconTint = Color(0xFFEA580C), label = "Diffusions", value = "$broadcastCount", trendLabel = "annonces publiées")
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Groups, iconBg = Color(0xFFD1FAE5), iconTint = Color(0xFF059669), label = "Lecture", value = if (actionRequired == 0) "OK" else "Vigilance", trendLabel = if (actionRequired == 0) "aucune urgence" else "prioriser")
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.NotificationsActive, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF2563EB), label = "Notifications", value = "$totalAlerts", trendLabel = "$criticalAlerts critiques")
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.WarningAmber, iconBg = Color(0xFFFEE2E2), iconTint = Color(0xFFDC2626), label = "Actions requises", value = "$actionRequired", trendLabel = "à traiter")
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Campaign, iconBg = Color(0xFFFFEDD5), iconTint = Color(0xFFEA580C), label = "Diffusions", value = "$broadcastCount", trendLabel = "annonces publiées")
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Groups, iconBg = Color(0xFFD1FAE5), iconTint = Color(0xFF059669), label = "Lecture", value = if (actionRequired == 0) "OK" else "Vigilance", trendLabel = if (actionRequired == 0) "aucune urgence" else "prioriser")
            }
        }
    }
}

@Composable
internal fun NotificationToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterSeverity: String,
    onFilterSeverityChange: (String) -> Unit,
    filterSource: String,
    onFilterSourceChange: (String) -> Unit,
    viewMode: String,
    onViewModeChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreateAnnouncement: () -> Unit,
    totalResults: Int,
    selectedTab: String,
    onTabChange: (String) -> Unit,
    showTabs: Boolean = true,
    showCreateAnnouncement: Boolean = true
) {
    var severityMenuExpanded by remember { mutableStateOf(false) }
    var sourceMenuExpanded by remember { mutableStateOf(false) }
    val severityOptions = listOf(
        "all" to "Toutes les priorités",
        "critical" to "Critiques",
        "warning" to "Attention",
        "info" to "Informations",
        "success" to "Traitées"
    )
    val sourceOptions = listOf(
        "all" to "Toutes les sources",
        "invoice" to "Facturation",
        "subscription" to "Abonnements",
        "group" to "Groupes",
        "admin" to "Administrateurs",
        "announcement" to "Diffusions"
    )

    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (showTabs) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = selectedTab == "alerts",
                        onClick = { onTabChange("alerts") },
                        label = { Text("Centre d'alertes") },
                        leadingIcon = if (selectedTab == "alerts") ({ Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(16.dp)) }) else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDBEAFE),
                            selectedLabelColor = Color(0xFF1D4ED8)
                        )
                    )
                    FilterChip(
                        selected = selectedTab == "broadcasts",
                        onClick = { onTabChange("broadcasts") },
                        label = { Text("Diffusions") },
                        leadingIcon = if (selectedTab == "broadcasts") ({ Icon(Icons.Default.Campaign, null, modifier = Modifier.size(16.dp)) }) else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFEDD5),
                            selectedLabelColor = Color(0xFFEA580C)
                        )
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(if (showTabs && selectedTab == "broadcasts") "Rechercher une diffusion" else "Rechercher une notification") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(14.dp)
                )
                Column {
                    OutlinedButton(onClick = { severityMenuExpanded = true }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(12.dp)) {
                        Text(severityOptions.firstOrNull { it.first == filterSeverity }?.second ?: "Priorité")
                    }
                    DropdownMenu(expanded = severityMenuExpanded, onDismissRequest = { severityMenuExpanded = false }) {
                        severityOptions.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                onFilterSeverityChange(key)
                                severityMenuExpanded = false
                            })
                        }
                    }
                }
                Column {
                    OutlinedButton(onClick = { sourceMenuExpanded = true }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(12.dp)) {
                        Text(sourceOptions.firstOrNull { it.first == filterSource }?.second ?: "Source")
                    }
                    DropdownMenu(expanded = sourceMenuExpanded, onDismissRequest = { sourceMenuExpanded = false }) {
                        sourceOptions.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                onFilterSourceChange(key)
                                sourceMenuExpanded = false
                            })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onViewModeChange("card") }, modifier = Modifier.background(if (viewMode == "card") Color(0xFFEEF2FF) else Color.Transparent, RoundedCornerShape(10.dp)).cursorHand()) {
                        Icon(Icons.Default.Window, null, tint = if (viewMode == "card") Color(0xFF4F46E5) else Color(0xFF64748B))
                    }
                    IconButton(onClick = { onViewModeChange("list") }, modifier = Modifier.background(if (viewMode == "list") Color(0xFFEEF2FF) else Color.Transparent, RoundedCornerShape(10.dp)).cursorHand()) {
                        Icon(Icons.Default.TableRows, null, tint = if (viewMode == "list") Color(0xFF4F46E5) else Color(0xFF64748B))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("$totalResults élément(s)", fontSize = 12.sp, color = EpiloteTextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onRefresh, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Text("Actualiser", modifier = Modifier.padding(start = 6.dp))
                    }
                    if (showCreateAnnouncement) {
                        FilledTonalButton(
                            onClick = onCreateAnnouncement,
                            modifier = Modifier.cursorHand(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1D3557), contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Campaign, null, modifier = Modifier.size(16.dp))
                            Text("Nouvelle diffusion", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NotificationCardGrid(items: List<AdminNotificationItem>, onViewDetail: (AdminNotificationItem) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 980.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items.forEachIndexed { index, item ->
                    AnimatedCardEntrance(index) { NotificationCard(item = item, onViewDetail = { onViewDetail(item) }) }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items.filterIndexed { index, _ -> index % 2 == 0 }.forEachIndexed { index, item ->
                        AnimatedCardEntrance(index) { NotificationCard(item = item, onViewDetail = { onViewDetail(item) }) }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items.filterIndexed { index, _ -> index % 2 == 1 }.forEachIndexed { index, item ->
                        AnimatedCardEntrance(index) { NotificationCard(item = item, onViewDetail = { onViewDetail(item) }) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NotificationListView(items: List<AdminNotificationItem>, onViewDetail: (AdminNotificationItem) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column {
            items.forEachIndexed { index, item ->
                Surface(onClick = { onViewDetail(item) }, modifier = Modifier.fillMaxWidth().cursorHand().hoverScale(1.003f), color = Color.Transparent) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        val iconTint = notificationSeverityColor(item.severity)
                        BoxWithConstraints(modifier = Modifier.size(38.dp)) {
                            Surface(shape = RoundedCornerShape(10.dp), color = iconTint.copy(alpha = 0.10f)) {
                                BoxWithConstraints(modifier = Modifier.size(38.dp)) {
                                    Icon(notificationIcon(item.sourceType, item.severity), null, modifier = Modifier.align(Alignment.Center).size(18.dp), tint = iconTint)
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.message, fontSize = 12.sp, color = EpiloteTextMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            NotificationTag(notificationSeverityLabel(item.severity), notificationSeverityColor(item.severity))
                            Text(formatDate(item.createdAt), fontSize = 11.sp, color = EpiloteTextMuted)
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF1D3557), modifier = Modifier.size(16.dp))
                    }
                }
                if (index < items.lastIndex) HorizontalDivider(color = Color(0xFFF1F5F9))
            }
        }
    }
}

@Composable
private fun NotificationCard(item: AdminNotificationItem, onViewDetail: () -> Unit) {
    val severityColor = notificationSeverityColor(item.severity)
    val sourceColor = notificationSourceColor(item.sourceType)
    Card(
        modifier = Modifier.fillMaxWidth().cursorHand().hoverScale(1.005f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onViewDetail
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = severityColor.copy(alpha = 0.10f)) {
                        Icon(notificationIcon(item.sourceType, item.severity), null, modifier = Modifier.padding(10.dp).size(18.dp), tint = severityColor)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(formatDate(item.createdAt), fontSize = 11.sp, color = EpiloteTextMuted)
                    }
                }
                NotificationTag(notificationSeverityLabel(item.severity), severityColor)
            }
            Text(item.message, fontSize = 12.sp, color = Color(0xFF475569), maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NotificationTag(notificationSourceLabel(item.sourceType), sourceColor)
                Text(item.targetLabel, fontSize = 11.sp, color = EpiloteTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun NotificationTag(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.10f)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
