package cg.epilote.desktop.ui.screens.superadmin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.ProvinceStatsDto

// ── Section carte des départements du Congo ─────────────────────────────────

@Composable
fun DepartmentMapSection(
    stats: AdminStats,
    modifier: Modifier = Modifier
) {
    val departments = congoDepartments
    val totalDepts = departments.size
    val deptsWithGroupes = stats.groupesByProvince.count { it.groupesCount > 0 }
    val totalGroupes = stats.totalGroupes
    val totalEcoles = stats.totalEcoles

    var selectedDepartment by remember { mutableStateOf<DepartmentInfo?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // En-tête avec légende
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            "République du Congo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "$totalDepts départements · $totalGroupes groupes · $totalEcoles écoles",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Légende de couleurs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DepartmentTier.entries.forEach { tier ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(tier.color, CircleShape)
                        )
                    }
                }
            }

            // Barre de progression
            ProgressBar(
                deptsWithGroupes = deptsWithGroupes,
                totalDepts = totalDepts
            )

            // Grille de départements + panneau détail
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Grille
                DepartmentGrid(
                    departments = departments,
                    provinceStats = stats.groupesByProvince,
                    selectedDepartment = selectedDepartment,
                    onSelect = { selectedDepartment = it },
                    modifier = Modifier.weight(2f)
                )

                // Panneau détail à droite
                DepartmentDetailPanel(
                    selectedDepartment = selectedDepartment,
                    provinceStats = stats.groupesByProvince,
                    totalDepts = totalDepts,
                    deptsWithGroupes = deptsWithGroupes,
                    totalGroupes = totalGroupes,
                    totalEcoles = totalEcoles,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── Grille de départements ──────────────────────────────────────────────────

@Composable
private fun DepartmentGrid(
    departments: List<DepartmentInfo>,
    provinceStats: List<ProvinceStatsDto>,
    selectedDepartment: DepartmentInfo?,
    onSelect: (DepartmentInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val statsMap = provinceStats.associateBy { it.province }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        departments.chunked(4).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { dept ->
                    val provinceStat = statsMap[dept.nom]
                    val groupes = provinceStat?.groupesCount ?: 0
                    val isSelected = selectedDepartment == dept

                    DepartmentChip(
                        department = dept,
                        groupesCount = groupes,
                        isSelected = isSelected,
                        onClick = { onSelect(dept) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Spacers pour compléter la dernière ligne
                repeat(4 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DepartmentChip(
    department: DepartmentInfo,
    groupesCount: Long,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val borderColor = if (isSelected) Color(0xFF3B82F6) else Color.Transparent
    val chipAlpha = if (isHovered) 0.85f else 1f

    Surface(
        modifier = modifier
            .hoverable(interactionSource)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = department.color.copy(alpha = chipAlpha),
        border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.linearGradient(listOf(borderColor, borderColor))
        ) else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                department.nom,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            Text(
                department.chefLieu,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Panneau détail (droite) ───────────────────────────────────────────────

@Composable
private fun DepartmentDetailPanel(
    selectedDepartment: DepartmentInfo?,
    provinceStats: List<ProvinceStatsDto>,
    totalDepts: Int,
    deptsWithGroupes: Int,
    totalGroupes: Long,
    totalEcoles: Long,
    modifier: Modifier = Modifier
) {
    val uncovered = totalDepts - deptsWithGroupes
    val coveragePct = if (totalDepts > 0) (deptsWithGroupes * 100) / totalDepts else 0
    val statsMap = provinceStats.associateBy { it.province }
    val deptStat = selectedDepartment?.let { statsMap[it.nom] }
    val deptGroupes = deptStat?.groupesCount ?: 0L
    val deptEcoles = deptStat?.ecolesCount ?: 0L
    val deptCovered = deptGroupes > 0

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedDepartment == null) {
                // ── Vue nationale par défaut
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Public, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                    Text("Vue nationale", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Couverture en %
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF3B82F6).copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("$coveragePct%", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3B82F6))
                        Text("couverture nationale", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                }

                // KPIs en grille 2×2
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NationalKpiMini(
                            label = "Groupes",
                            value = "$totalGroupes",
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                        NationalKpiMini(
                            label = "Écoles",
                            value = "$totalEcoles",
                            color = Color(0xFF6366F1),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NationalKpiMini(
                            label = "Dépts couverts",
                            value = "$deptsWithGroupes / $totalDepts",
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                        NationalKpiMini(
                            label = "Zones vides",
                            value = "$uncovered",
                            color = if (uncovered > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Légende tiers
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Légende", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
                    DepartmentTier.entries.forEach { tier ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(Modifier.size(8.dp).background(tier.color, CircleShape))
                            Text(tier.label, fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                Text(
                    "‹ Sélectionnez un département",
                    fontSize = 10.sp, color = Color(0xFFCBD5E1), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // ── Détail département sélectionné
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = selectedDepartment.color.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            selectedDepartment.nom,
                            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                            color = selectedDepartment.color
                        )
                        Text(
                            "📍 ${selectedDepartment.chefLieu}",
                            fontSize = 11.sp, color = Color(0xFF64748B)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Stats départementales
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NationalKpiMini(
                        label = "Groupes scolaires",
                        value = "$deptGroupes",
                        color = Color(0xFF10B981),
                        modifier = Modifier.fillMaxWidth()
                    )
                    NationalKpiMini(
                        label = "Écoles",
                        value = "$deptEcoles",
                        color = Color(0xFF6366F1),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Statut couverture
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (deptCovered) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFEF4444).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.size(8.dp).background(
                            if (deptCovered) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape
                        ))
                        Text(
                            if (deptCovered) "Zone couverte" else "Zone non couverte",
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = if (deptCovered) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // KPIs nationaux en bas pour référence
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Référence nationale", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
                    Text("$totalGroupes groupes · $deptsWithGroupes/$totalDepts dépts", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text("Couverture $coveragePct%", fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
private fun NationalKpiMini(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.07f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 9.sp, color = Color(0xFF64748B))
        }
    }
}

// ── Barre de progression ────────────────────────────────────────────────────

@Composable
private fun ProgressBar(
    deptsWithGroupes: Int,
    totalDepts: Int
) {
    val fraction = if (totalDepts > 0) (deptsWithGroupes.toFloat() / totalDepts).coerceIn(0f, 1f) else 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(Color(0xFFE2E8F0), RoundedCornerShape(3.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFFEC4899))
                    ),
                    RoundedCornerShape(3.dp)
                )
        )
    }
}
