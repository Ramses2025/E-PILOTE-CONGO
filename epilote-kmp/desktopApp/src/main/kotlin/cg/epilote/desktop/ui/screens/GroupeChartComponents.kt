package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Department Distribution ────────────────────────────────────

@Composable
fun GroupeDepartmentDistribution(byDepartment: Map<String, List<GroupeDto>>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.LocationOn, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                Text("Répartition par Département", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF0F172A))
            }
            if (byDepartment.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), Alignment.Center) {
                    Text("Aucune donnée disponible", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
            } else {
                val maxCount = byDepartment.values.maxOfOrNull { it.size } ?: 1
                byDepartment.entries.sortedByDescending { it.value.size }.forEach { (dept, gs) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            dept, fontSize = 13.sp, color = Color(0xFF334155),
                            modifier = Modifier.width(120.dp), maxLines = 1,
                            overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium
                        )
                        Box(
                            Modifier.weight(1f).height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                Modifier.fillMaxHeight()
                                    .fillMaxWidth(fraction = gs.size.toFloat() / maxCount)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFF1D3557))
                            )
                        }
                        Text(
                            "${gs.size}", fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, color = Color(0xFF0F172A),
                            modifier = Modifier.width(28.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Plan Distribution Chart ────────────────────────────────────

@Composable
fun GroupePlanDistributionChart(byPlan: Map<String, List<GroupeDto>>, modifier: Modifier = Modifier) {
    val planColors = mapOf(
        "Institutionnel" to Color(0xFFE76F51),
        "Pro" to Color(0xFF6C5CE7),
        "Premium" to Color(0xFFE9C46A),
        "Gratuit" to Color(0xFF2A9D8F),
        "Non défini" to Color(0xFF94A3B8)
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CreditCard, null, tint = Color(0xFF6C5CE7), modifier = Modifier.size(20.dp))
                Text("Répartition par Plan", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF0F172A))
            }
            if (byPlan.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), Alignment.Center) {
                    Text("Aucune donnée disponible", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
            } else {
                val total = byPlan.values.sumOf { it.size }.coerceAtLeast(1)
                byPlan.entries.sortedByDescending { it.value.size }.forEach { (plan, gs) ->
                    val pct = gs.size * 100 / total
                    val color = planColors[plan] ?: Color(0xFF94A3B8)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
                        Text(
                            plan, fontSize = 13.sp, color = Color(0xFF334155),
                            modifier = Modifier.width(100.dp), fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Box(
                            Modifier.weight(1f).height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                Modifier.fillMaxHeight()
                                    .fillMaxWidth(fraction = gs.size.toFloat() / total)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(color)
                            )
                        }
                        Text(
                            "${gs.size} ($pct%)", fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A),
                            modifier = Modifier.width(64.dp)
                        )
                    }
                }
            }
        }
    }
}
