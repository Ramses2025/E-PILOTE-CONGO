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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.screens.superadmin.KpiCard
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand

private val categoryColors = mapOf(
    "scolarite" to Color(0xFF2A9D8F),
    "pedagogie" to Color(0xFF1D3557),
    "finances" to Color(0xFFE9C46A),
    "personnel" to Color(0xFFE63946),
    "vie-scolaire" to Color(0xFF6C5CE7),
    "communication" to Color(0xFF00875A)
)

internal fun categoryColor(code: String): Color = categoryColors[code] ?: Color(0xFF3B82F6)

@Composable
internal fun CategoryKpiRow(
    totalCategories: Int,
    activeCategories: Int,
    coreCategories: Int,
    linkedModules: Int,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 980.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ViewModule,
                        iconBg = Color(0xFFDBEAFE),
                        iconTint = Color(0xFF2563EB),
                        label = "Catégories",
                        value = "$totalCategories",
                        trendLabel = "$activeCategories actives"
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        iconBg = Color(0xFFD1FAE5),
                        iconTint = Color(0xFF059669),
                        label = "Actives",
                        value = "$activeCategories",
                        trendLabel = "sur la plateforme"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ViewModule,
                        iconBg = Color(0xFFEDE9FE),
                        iconTint = Color(0xFF6D28D9),
                        label = "Core",
                        value = "$coreCategories",
                        trendLabel = "catégories système"
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TableChart,
                        iconBg = Color(0xFFFFEDD5),
                        iconTint = Color(0xFFC2410C),
                        label = "Modules liés",
                        value = "$linkedModules",
                        trendLabel = "référencés"
                    )
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ViewModule,
                    iconBg = Color(0xFFDBEAFE),
                    iconTint = Color(0xFF2563EB),
                    label = "Catégories",
                    value = "$totalCategories",
                    trendLabel = "$activeCategories actives"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    iconBg = Color(0xFFD1FAE5),
                    iconTint = Color(0xFF059669),
                    label = "Actives",
                    value = "$activeCategories",
                    trendLabel = "sur la plateforme"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ViewModule,
                    iconBg = Color(0xFFEDE9FE),
                    iconTint = Color(0xFF6D28D9),
                    label = "Core",
                    value = "$coreCategories",
                    trendLabel = "catégories système"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.TableChart,
                    iconBg = Color(0xFFFFEDD5),
                    iconTint = Color(0xFFC2410C),
                    label = "Modules liés",
                    value = "$linkedModules",
                    trendLabel = "référencés"
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterStatus: String,
    onFilterStatusChange: (String) -> Unit,
    filterCore: String,
    onFilterCoreChange: (String) -> Unit,
    sortBy: String,
    onSortChange: (String) -> Unit,
    viewMode: String,
    onViewModeChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreateCategory: () -> Unit,
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
                label = { Text("Rechercher une catégorie…") },
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
                    onClick = onCreateCategory,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1D3557), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Text("Nouvelle catégorie", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all" to "Toutes", "active" to "Actives", "inactive" to "Inactives").forEach { (value, label) ->
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

            listOf("all" to "Toutes", "core" to "Core", "custom" to "Métier").forEach { (value, label) ->
                FilterChip(
                    selected = filterCore == value,
                    onClick = { onFilterCoreChange(value) },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.cursorHand(),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF6C5CE7).copy(alpha = 0.12f), selectedLabelColor = Color(0xFF6C5CE7))
                )
            }

            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE2E8F0)) {
                Text("│", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, color = Color(0xFF94A3B8))
            }

            listOf("ordre" to "Ordre", "name" to "Nom", "modules" to "Modules liés").forEach { (value, label) ->
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
internal fun CategoryCardGrid(
    categories: List<CategorieDto>,
    modulesByCategory: Map<String, List<ModuleDto>>,
    onViewDetail: (CategorieDto) -> Unit,
    onEdit: (CategorieDto) -> Unit,
    onToggleStatus: (CategorieDto) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        categories.forEach { category ->
            val color = categoryColor(category.code)
            val linkedModules = modulesByCategory[category.code].orEmpty()
            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(42.dp).background(color.copy(alpha = 0.14f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(category.nom, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(category.code, fontSize = 11.sp, color = EpiloteTextMuted)
                            }
                        }
                        CategoryStatusBadges(category = category, moduleCount = linkedModules.size, accentColor = color)
                    }
                    if (linkedModules.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            linkedModules.take(6).forEach { module ->
                                Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.1f)) {
                                    Text(module.nom, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, color = color)
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onViewDetail(category) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                            Text("Détails", modifier = Modifier.padding(start = 6.dp))
                        }
                        OutlinedButton(onClick = { onEdit(category) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Text("Modifier", modifier = Modifier.padding(start = 6.dp))
                        }
                        FilledTonalButton(onClick = { onToggleStatus(category) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Text(if (category.isActive) "Suspendre" else "Réactiver")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CategoryTableView(
    categories: List<CategorieDto>,
    modulesByCategory: Map<String, List<ModuleDto>>,
    onViewDetail: (CategorieDto) -> Unit,
    onEdit: (CategorieDto) -> Unit,
    onToggleStatus: (CategorieDto) -> Unit
) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            categories.forEach { category ->
                val color = categoryColor(category.code)
                val linkedModules = modulesByCategory[category.code].orEmpty()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
                        Column {
                            Text(category.nom, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${category.code} • ordre ${category.ordre}", fontSize = 11.sp, color = EpiloteTextMuted)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        CategoryStatusBadges(category = category, moduleCount = linkedModules.size, accentColor = color)
                        OutlinedButton(onClick = { onViewDetail(category) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Text("Détails")
                        }
                        OutlinedButton(onClick = { onEdit(category) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Text("Modifier")
                        }
                        FilledTonalButton(onClick = { onToggleStatus(category) }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Text(if (category.isActive) "Suspendre" else "Réactiver")
                        }
                    }
                }
                if (category != categories.last()) {
                    Spacer(Modifier.height(2.dp))
                    Surface(modifier = Modifier.fillMaxWidth().height(1.dp), color = Color(0xFFE2E8F0)) {}
                }
            }
        }
    }
}

@Composable
private fun CategoryStatusBadges(category: CategorieDto, moduleCount: Int, accentColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(999.dp), color = accentColor.copy(alpha = 0.1f)) {
            Text("$moduleCount modules", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
        }
        Surface(shape = RoundedCornerShape(999.dp), color = if (category.isCore) Color(0xFFEDE9FE) else Color(0xFFDBEAFE)) {
            Text(if (category.isCore) "CORE" else "MÉTIER", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, color = if (category.isCore) Color(0xFF6D28D9) else Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
        }
        Surface(shape = RoundedCornerShape(999.dp), color = if (category.isActive) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)) {
            Text(if (category.isActive) "ACTIVE" else "INACTIVE", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, color = if (category.isActive) Color(0xFF166534) else Color(0xFFB91C1C), fontWeight = FontWeight.SemiBold)
        }
    }
}
