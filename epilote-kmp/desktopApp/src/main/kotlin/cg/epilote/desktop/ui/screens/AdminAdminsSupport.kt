package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisedUserCircle
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.screens.superadmin.KpiCard
import cg.epilote.desktop.ui.theme.cursorHand

// ── KPI Row (same style as GroupesKpiRow) ──────────────────────────

@Composable
internal fun AdminKpiRow(
    totalAdmins: Int,
    adminGroupeCount: Int,
    superAdminCount: Int,
    activeCount: Int,
    modifier: Modifier = Modifier
) {
    val pctActifs = if (totalAdmins > 0) (activeCount * 100 / totalAdmins) else 0
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 980.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.SupervisedUserCircle,
                        iconBg = Color(0xFFDBEAFE),
                        iconTint = Color(0xFF3B82F6),
                        label = "Total Admins",
                        value = "$totalAdmins",
                        trend = if (activeCount > 0) "↑" else "",
                        trendLabel = "$activeCount actifs"
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Shield,
                        iconBg = Color(0xFFEDE9FE),
                        iconTint = Color(0xFF6C5CE7),
                        label = "Admins Groupe",
                        value = "$adminGroupeCount",
                        trendLabel = "groupes scolaires"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Shield,
                        iconBg = Color(0xFFFEE2E2),
                        iconTint = Color(0xFFE63946),
                        label = "Super Admins",
                        value = "$superAdminCount",
                        trendLabel = "plateforme"
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        iconBg = Color(0xFFD1FAE5),
                        iconTint = Color(0xFF059669),
                        label = "Actifs",
                        value = "$activeCount",
                        trend = "$pctActifs%",
                        trendLabel = "en activité",
                        trendColor = Color(0xFF059669)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.SupervisedUserCircle,
                    iconBg = Color(0xFFDBEAFE),
                    iconTint = Color(0xFF3B82F6),
                    label = "Total Admins",
                    value = "$totalAdmins",
                    trend = if (activeCount > 0) "↑" else "",
                    trendLabel = "$activeCount actifs"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Shield,
                    iconBg = Color(0xFFEDE9FE),
                    iconTint = Color(0xFF6C5CE7),
                    label = "Admins Groupe",
                    value = "$adminGroupeCount",
                    trendLabel = "groupes scolaires"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Shield,
                    iconBg = Color(0xFFFEE2E2),
                    iconTint = Color(0xFFE63946),
                    label = "Super Admins",
                    value = "$superAdminCount",
                    trendLabel = "plateforme"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    iconBg = Color(0xFFD1FAE5),
                    iconTint = Color(0xFF059669),
                    label = "Actifs",
                    value = "$activeCount",
                    trend = "$pctActifs%",
                    trendLabel = "en activité",
                    trendColor = Color(0xFF059669)
                )
            }
        }
    }
}

// ── Toolbar ────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AdminToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterStatus: String,
    onFilterStatusChange: (String) -> Unit,
    filterRole: String,
    onFilterRoleChange: (String) -> Unit,
    filterGroupId: String,
    onFilterGroupChange: (String) -> Unit,
    groupOptions: List<Pair<String, String>>,
    sortBy: String,
    onSortChange: (String) -> Unit,
    viewMode: String,
    onViewModeChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreateAdmin: () -> Unit,
    totalResults: Int,
    modifier: Modifier = Modifier
) {
    var showGroupMenu by remember { mutableStateOf(false) }
    val selectedGroupLabel = groupOptions.find { it.first == filterGroupId }?.second ?: "Tous les groupes"

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                label = { Text("Rechercher un admin…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFF1F5F9)) {
                    Text(
                        text = "$totalResults résultat(s)",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9)) {
                    Row(Modifier.padding(2.dp)) {
                        IconButton(onClick = { onViewModeChange("card") }, modifier = Modifier.size(32.dp).cursorHand()) {
                            Icon(Icons.Default.ViewModule, "Cartes", Modifier.size(18.dp), tint = if (viewMode == "card") Color(0xFF1D3557) else Color(0xFF94A3B8))
                        }
                        IconButton(onClick = { onViewModeChange("table") }, modifier = Modifier.size(32.dp).cursorHand()) {
                            Icon(Icons.Default.TableChart, "Tableau", Modifier.size(18.dp), tint = if (viewMode == "table") Color(0xFF1D3557) else Color(0xFF94A3B8))
                        }
                    }
                }
                FilledTonalButton(onClick = onRefresh, shape = RoundedCornerShape(10.dp), modifier = Modifier.cursorHand()) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Text("Actualiser", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                }
                FilledTonalButton(
                    onClick = onCreateAdmin,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand(),
                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1D3557), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Text("Nouvel admin", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Status filter
            listOf("all" to "Tous", "active" to "Actif", "suspended" to "Suspendu", "locked" to "Verrouillé").forEach { (value, label) ->
                FilterChip(
                    selected = filterStatus == value,
                    onClick = { onFilterStatusChange(value) },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.cursorHand(),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1D3557).copy(alpha = 0.12f), selectedLabelColor = Color(0xFF1D3557))
                )
            }

            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE2E8F0)) {
                Text("│", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, color = Color(0xFF94A3B8))
            }

            BoxWithConstraints {
                OutlinedButton(
                    onClick = { showGroupMenu = true },
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder,
                    modifier = Modifier.cursorHand()
                ) {
                    Text(selectedGroupLabel, fontSize = 11.sp, maxLines = 1)
                    Spacer(Modifier.size(4.dp))
                    Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
                }
                DropdownMenu(expanded = showGroupMenu, onDismissRequest = { showGroupMenu = false }) {
                    groupOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onFilterGroupChange(value)
                                showGroupMenu = false
                            }
                        )
                    }
                }
            }

            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE2E8F0)) {
                Text("│", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, color = Color(0xFF94A3B8))
            }

            // Role filter
            listOf("all" to "Tous", "ADMIN_GROUPE" to "Admin Groupe", "SUPER_ADMIN" to "Super Admin").forEach { (value, label) ->
                FilterChip(
                    selected = filterRole == value,
                    onClick = { onFilterRoleChange(value) },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.cursorHand(),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF6C5CE7).copy(alpha = 0.12f), selectedLabelColor = Color(0xFF6C5CE7))
                )
            }

            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE2E8F0)) {
                Text("│", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, color = Color(0xFF94A3B8))
            }

            // Sort
            listOf("recent" to "Récent", "name" to "Nom", "lastLogin" to "Dernière connexion").forEach { (value, label) ->
                FilterChip(
                    selected = sortBy == value,
                    onClick = { onSortChange(value) },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.cursorHand(),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF059669).copy(alpha = 0.12f), selectedLabelColor = Color(0xFF059669))
                )
            }
        }
    }
}
