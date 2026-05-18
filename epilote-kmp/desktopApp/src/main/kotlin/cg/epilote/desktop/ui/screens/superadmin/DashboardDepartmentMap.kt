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

            DepartmentProgressBar(
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

