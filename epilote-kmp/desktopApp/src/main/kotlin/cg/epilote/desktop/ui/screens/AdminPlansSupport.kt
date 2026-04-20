package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.screens.superadmin.KpiCard
import cg.epilote.desktop.ui.theme.AnimatedCardEntrance
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import cg.epilote.desktop.ui.theme.hoverScale

internal val planAccentColors = listOf(
    Color(0xFF2A9D8F),
    Color(0xFF1D3557),
    Color(0xFFE9C46A),
    Color(0xFF6C5CE7),
    Color(0xFFE76F51)
)

internal fun planColor(index: Int): Color = planAccentColors.getOrElse(index) { Color(0xFF3B82F6) }

internal fun planColorById(id: String): Color = when {
    id.contains("pro", true) -> Color(0xFF6C5CE7)
    id.contains("premium", true) -> Color(0xFFE9C46A)
    id.contains("gratuit", true) -> Color(0xFF2A9D8F)
    id.contains("institution", true) -> Color(0xFFE76F51)
    else -> Color(0xFF1D3557)
}

internal fun planIcon(index: Int) = when (index) {
    0 -> Icons.Default.Star
    1 -> Icons.AutoMirrored.Filled.StarHalf
    else -> Icons.Default.StarRate
}

@Composable
internal fun PlanKpiRow(
    totalPlans: Int,
    activePlans: Int,
    totalModulesAcrossPlans: Int,
    avgPrice: Long,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 980.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Star, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF2563EB), label = "Plans", value = "$totalPlans", trendLabel = "$activePlans actifs")
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.CheckCircle, iconBg = Color(0xFFD1FAE5), iconTint = Color(0xFF059669), label = "Actifs", value = "$activePlans", trendLabel = "sur la plateforme")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.ViewModule, iconBg = Color(0xFFEDE9FE), iconTint = Color(0xFF6D28D9), label = "Modules couverts", value = "$totalModulesAcrossPlans", trendLabel = "tous plans confondus")
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.MonetizationOn, iconBg = Color(0xFFFFEDD5), iconTint = Color(0xFFC2410C), label = "Prix moyen", value = "${avgPrice} XAF", trendLabel = "tarif moyen")
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Star, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF2563EB), label = "Plans", value = "$totalPlans", trendLabel = "$activePlans actifs")
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.CheckCircle, iconBg = Color(0xFFD1FAE5), iconTint = Color(0xFF059669), label = "Actifs", value = "$activePlans", trendLabel = "sur la plateforme")
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.ViewModule, iconBg = Color(0xFFEDE9FE), iconTint = Color(0xFF6D28D9), label = "Modules couverts", value = "$totalModulesAcrossPlans", trendLabel = "tous plans confondus")
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.MonetizationOn, iconBg = Color(0xFFFFEDD5), iconTint = Color(0xFFC2410C), label = "Prix moyen", value = "${avgPrice} XAF", trendLabel = "tarif moyen")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlanToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterStatus: String,
    onFilterStatusChange: (String) -> Unit,
    sortBy: String,
    onSortChange: (String) -> Unit,
    viewMode: String,
    onViewModeChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreatePlan: () -> Unit,
    totalResults: Int,
    modifier: Modifier = Modifier
) {
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
                label = { Text("Rechercher un plan…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFF1F5F9)) {
                    Text("$totalResults résultat(s)", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
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
                    onClick = onCreatePlan,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1D3557), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Text("Nouveau plan", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all" to "Tous", "active" to "Actifs", "inactive" to "Inactifs").forEach { (value, label) ->
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
            listOf("price" to "Prix", "name" to "Nom", "students" to "Élèves").forEach { (value, label) ->
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlanCardGrid(
    plans: List<PlanDto>,
    modules: List<ModuleDto>,
    onViewDetail: (PlanDto) -> Unit,
    onEdit: (PlanDto) -> Unit,
    onToggleStatus: (PlanDto) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cols = when {
            maxWidth >= 1400.dp -> 4
            maxWidth >= 1050.dp -> 3
            maxWidth >= 700.dp -> 2
            else -> 1
        }
        val cardWidth = (maxWidth - 16.dp * (cols - 1)) / cols
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            plans.chunked(cols).forEachIndexed { rowIndex, rowPlans ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowPlans.forEachIndexed { colIndex, plan ->
                        val globalIndex = rowIndex * cols + colIndex
                        val color = planColorById(plan.id)
                        val planModules = plan.modulesIncluded.take(4)
                        Box(modifier = Modifier.width(cardWidth)) {
                            AnimatedCardEntrance(index = globalIndex) {
                                Surface(
                                    onClick = { onViewDetail(plan) },
                                    modifier = Modifier.fillMaxWidth().hoverScale(1.01f).cursorHand(),
                                    shape = RoundedCornerShape(22.dp),
                                    color = Color.White,
                                    shadowElevation = 3.dp
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(color.copy(alpha = 0.08f))
                                                .padding(20.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.16f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                                                        Icon(planIcon(globalIndex), null, tint = color, modifier = Modifier.size(24.dp))
                                                    }
                                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                        Text(plan.nom, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text(plan.type.replaceFirstChar { it.uppercase() }, fontSize = 11.sp, color = EpiloteTextMuted)
                                                    }
                                                }
                                                Surface(shape = RoundedCornerShape(999.dp), color = Color.White.copy(alpha = 0.92f)) {
                                                    Text(if (plan.isActive) "En ligne" else "Masqué", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (plan.isActive) Color(0xFF166534) else Color(0xFFB91C1C))
                                                }
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    Text("À partir de", fontSize = 11.sp, color = Color(0xFF64748B))
                                                    Text(formatMoneyXaf(plan.prixXAF), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
                                                }
                                                Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f)) {
                                                    Text("${plan.modulesIncluded.size} modules", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color)
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                PlanStatusBadges(plan = plan, accentColor = color)
                                            }

                                            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF8FAFC)) {
                                                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    PlanFeatureRow("Capacité élèves", "${plan.maxStudents}", color)
                                                    PlanFeatureRow("Personnel inclus", "${plan.maxPersonnel}", color)
                                                    PlanFeatureRow("Type d'offre", plan.type.replaceFirstChar { it.uppercase() }, color)
                                                }
                                            }

                                            if (planModules.isNotEmpty()) {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text("Modules phares", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D3557))
                                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        planModules.forEach { slug ->
                                                            val moduleName = modules.firstOrNull { it.code == slug }?.nom ?: slug
                                                            Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.08f)) {
                                                                Text(moduleName, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium, maxLines = 1)
                                                            }
                                                        }
                                                        if (plan.modulesIncluded.size > planModules.size) {
                                                            Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFF1F5F9)) {
                                                                Text("+${plan.modulesIncluded.size - planModules.size}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { onViewDetail(plan) }, modifier = Modifier.weight(1f).cursorHand(), shape = RoundedCornerShape(12.dp)) {
                                                Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                                                Text("Détails", modifier = Modifier.padding(start = 6.dp))
                                            }
                                            OutlinedButton(onClick = { onEdit(plan) }, modifier = Modifier.weight(1f).cursorHand(), shape = RoundedCornerShape(12.dp)) {
                                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                                Text("Modifier", modifier = Modifier.padding(start = 6.dp))
                                            }
                                            FilledTonalButton(onClick = { onToggleStatus(plan) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = color.copy(alpha = 0.12f), contentColor = color)) {
                                                Text(if (plan.isActive) "Suspendre" else "Réactiver")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    repeat(cols - rowPlans.size) {
                        Spacer(Modifier.width(cardWidth))
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlanTableView(
    plans: List<PlanDto>,
    onViewDetail: (PlanDto) -> Unit,
    onEdit: (PlanDto) -> Unit,
    onToggleStatus: (PlanDto) -> Unit
) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            plans.forEach { plan ->
                val color = planColorById(plan.id)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                        Column {
                            Text(plan.nom, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${plan.type} • ${plan.prixXAF} ${plan.currency}", fontSize = 11.sp, color = EpiloteTextMuted)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlanStatusBadges(plan = plan, accentColor = color)
                        OutlinedButton(onClick = { onViewDetail(plan) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) { Text("Détails") }
                        OutlinedButton(onClick = { onEdit(plan) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) { Text("Modifier") }
                        FilledTonalButton(onClick = { onToggleStatus(plan) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Text(if (plan.isActive) "Suspendre" else "Réactiver")
                        }
                    }
                }
                if (plan != plans.last()) {
                    Spacer(Modifier.height(2.dp))
                    Surface(modifier = Modifier.fillMaxWidth().height(1.dp), color = Color(0xFFE2E8F0)) {}
                }
            }
        }
    }
}

@Composable
private fun PlanFeatureRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = EpiloteTextMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun PlanStatusBadges(plan: PlanDto, accentColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(999.dp), color = accentColor.copy(alpha = 0.1f)) {
            Text("${plan.modulesIncluded.size} modules", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
        }
        Surface(shape = RoundedCornerShape(999.dp), color = if (plan.isActive) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)) {
            Text(if (plan.isActive) "ACTIF" else "INACTIF", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, color = if (plan.isActive) Color(0xFF166534) else Color(0xFFB91C1C), fontWeight = FontWeight.SemiBold)
        }
    }
}
