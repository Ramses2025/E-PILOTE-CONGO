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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
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
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand

internal val modulePlanOptions = listOf(
    "all" to "Tous les plans",
    "gratuit" to "Gratuit",
    "premium" to "Premium",
    "pro" to "Pro"
)

internal fun modulePlanAccent(plan: String): Color = when (plan.lowercase()) {
    "gratuit" -> Color(0xFF059669)
    "premium" -> Color(0xFF1D4ED8)
    "pro" -> Color(0xFFC2410C)
    else -> Color(0xFF475569)
}

internal fun modulePlanLabel(plan: String): String = plan.uppercase()

@Composable
internal fun ModuleKpiRow(
    totalModules: Int,
    activeModules: Int,
    coreModules: Int,
    premiumModules: Int,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 980.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Extension,
                        iconBg = Color(0xFFDBEAFE),
                        iconTint = Color(0xFF2563EB),
                        label = "Modules",
                        value = "$totalModules",
                        trendLabel = "$activeModules actifs"
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        iconBg = Color(0xFFD1FAE5),
                        iconTint = Color(0xFF059669),
                        label = "Actifs",
                        value = "$activeModules",
                        trendLabel = "visibles plateforme"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ViewModule,
                        iconBg = Color(0xFFEDE9FE),
                        iconTint = Color(0xFF6D28D9),
                        label = "Core",
                        value = "$coreModules",
                        trendLabel = "modules système"
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TableChart,
                        iconBg = Color(0xFFFFEDD5),
                        iconTint = Color(0xFFC2410C),
                        label = "Premium/Pro",
                        value = "$premiumModules",
                        trendLabel = "plans supérieurs"
                    )
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Extension,
                    iconBg = Color(0xFFDBEAFE),
                    iconTint = Color(0xFF2563EB),
                    label = "Modules",
                    value = "$totalModules",
                    trendLabel = "$activeModules actifs"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    iconBg = Color(0xFFD1FAE5),
                    iconTint = Color(0xFF059669),
                    label = "Actifs",
                    value = "$activeModules",
                    trendLabel = "visibles plateforme"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ViewModule,
                    iconBg = Color(0xFFEDE9FE),
                    iconTint = Color(0xFF6D28D9),
                    label = "Core",
                    value = "$coreModules",
                    trendLabel = "modules système"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.TableChart,
                    iconBg = Color(0xFFFFEDD5),
                    iconTint = Color(0xFFC2410C),
                    label = "Premium/Pro",
                    value = "$premiumModules",
                    trendLabel = "plans supérieurs"
                )
            }
        }
    }
}

@Composable
internal fun ModuleToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    statusOptions: List<Pair<String, String>>,
    filterStatus: String,
    onFilterStatusChange: (String) -> Unit,
    categoryOptions: List<Pair<String, String>>,
    filterCategory: String,
    onFilterCategoryChange: (String) -> Unit,
    filterPlan: String,
    onFilterPlanChange: (String) -> Unit,
    sortOptions: List<Pair<String, String>>,
    sortBy: String,
    onSortChange: (String) -> Unit,
    viewMode: String,
    onViewModeChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreateModule: () -> Unit,
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
                label = { Text("Rechercher un module…") },
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
                    onClick = onCreateModule,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1D3557), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Text("Nouveau module", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                }
            }
        }

        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DropdownFilterButton(
                label = statusOptions.firstOrNull { it.first == filterStatus }?.second ?: "Statut",
                options = statusOptions,
                onSelect = onFilterStatusChange
            )
            DropdownFilterButton(
                label = categoryOptions.firstOrNull { it.first == filterCategory }?.second ?: "Catégorie",
                options = categoryOptions,
                onSelect = onFilterCategoryChange
            )
            DropdownFilterButton(
                label = modulePlanOptions.firstOrNull { it.first == filterPlan }?.second ?: "Plan",
                options = modulePlanOptions,
                onSelect = onFilterPlanChange
            )
            DropdownFilterButton(
                label = sortOptions.firstOrNull { it.first == sortBy }?.second ?: "Tri",
                options = sortOptions,
                onSelect = onSortChange
            )
        }
    }
}

@Composable
private fun DropdownFilterButton(
    label: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(999.dp)) {
            Text(label, fontSize = 12.sp)
            Spacer(Modifier.size(4.dp))
            Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text, fontSize = 12.sp) },
                    onClick = { onSelect(value); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ModuleCardGrid(
    modules: List<ModuleDto>,
    categoriesByCode: Map<String, CategorieDto>,
    onViewDetail: (ModuleDto) -> Unit,
    onEdit: (ModuleDto) -> Unit,
    onToggleStatus: (ModuleDto) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cols = when {
            maxWidth >= 1400.dp -> 4
            maxWidth >= 1050.dp -> 3
            maxWidth >= 700.dp -> 2
            else -> 1
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            modules.chunked(cols).forEach { rowModules ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    rowModules.forEach { module ->
                        val category = categoriesByCode[module.categorieCode]
                        val accent = categoryColor(module.categorieCode)
                        val categoryLabel = category?.nom ?: module.categorieCode.ifBlank { "Sans catégorie" }
                        Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(42.dp).background(accent.copy(alpha = 0.14f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Extension, null, tint = accent, modifier = Modifier.size(18.dp))
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(module.nom, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(module.code, fontSize = 11.sp, color = EpiloteTextMuted)
                                        }
                                    }
                                    ModuleStatusBadges(module = module, categoryLabel = categoryLabel, accentColor = accent)
                                }
                                if (module.description.isNotBlank()) {
                                    Text(module.description, fontSize = 12.sp, color = Color(0xFF475569), maxLines = 2)
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(shape = RoundedCornerShape(999.dp), color = accent.copy(alpha = 0.1f)) {
                                        Text("Ordre ${module.ordre}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, color = accent, fontWeight = FontWeight.SemiBold)
                                    }
                                    Surface(shape = RoundedCornerShape(999.dp), color = modulePlanAccent(module.requiredPlan).copy(alpha = 0.1f)) {
                                        Text(modulePlanLabel(module.requiredPlan), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, color = modulePlanAccent(module.requiredPlan), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onViewDetail(module) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                                        Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                                        Text("Détails", modifier = Modifier.padding(start = 6.dp))
                                    }
                                    OutlinedButton(onClick = { onEdit(module) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                        Text("Modifier", modifier = Modifier.padding(start = 6.dp))
                                    }
                                    FilledTonalButton(onClick = { onToggleStatus(module) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                                        Text(if (module.isActive) "Suspendre" else "Réactiver")
                                    }
                                }
                            }
                        }
                    }
                    repeat(cols - rowModules.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

