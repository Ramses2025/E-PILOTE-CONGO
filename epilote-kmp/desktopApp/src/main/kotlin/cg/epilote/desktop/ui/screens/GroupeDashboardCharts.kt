package cg.epilote.desktop.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.*
import cg.epilote.desktop.ui.components.*
import cg.epilote.desktop.ui.screens.superadmin.formatXAF

// ── Row 1: Écoles par province + Indicateurs ─────────────────────────────────

@Composable
fun GroupeChartsRow1(
    stats: GroupeDashboardStatsDto,
    categoriesWithModules: List<CategorieWithModulesDto>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Gauche: Écoles par province (HorizontalBarChart)
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                    Text("Écoles par province", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                }
                val byProvince = stats.ecoles.groupBy { it.province.ifBlank { "Non définie" } }
                val entries = byProvince.entries
                    .sortedByDescending { it.value.size }
                    .map { (prov, list) -> ChartEntry(prov, list.size.toFloat(), Color(0xFF3B82F6)) }
                if (entries.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Aucune école", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                } else {
                    HorizontalBarChart(entries = entries, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // Droite: Indicateurs (ProgressRings)
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Analytics, null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp))
                    Text("Indicateurs clés", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Progression abonnement
                    val now = System.currentTimeMillis()
                    val abProgress = if (stats.abonnementDateDebut > 0 && stats.abonnementDateFin > stats.abonnementDateDebut) {
                        ((now - stats.abonnementDateDebut).toFloat() / (stats.abonnementDateFin - stats.abonnementDateDebut).toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    ProgressRing(
                        progress = abProgress,
                        label = "Abonnement",
                        value = "${(abProgress * 100).toInt()}%",
                        color = Color(0xFF059669)
                    )

                    // Modules utilisés
                    val totalModulesInPlan = categoriesWithModules.sumOf { it.modules.size }.coerceAtLeast(1)
                    val modulesUsed = stats.nbModulesActifs.toInt()
                    val moduleProgress = (modulesUsed.toFloat() / totalModulesInPlan).coerceIn(0f, 1f)
                    ProgressRing(
                        progress = moduleProgress,
                        label = "Modules",
                        value = "$modulesUsed/$totalModulesInPlan",
                        color = Color(0xFF7C3AED)
                    )

                    // Paiement
                    val payProgress = if (stats.facturationTotaleXAF > 0)
                        (stats.montantPayeXAF.toFloat() / stats.facturationTotaleXAF.toFloat()).coerceIn(0f, 1f)
                    else 0f
                    ProgressRing(
                        progress = payProgress,
                        label = "Paiement",
                        value = "${(payProgress * 100).toInt()}%",
                        color = Color(0xFF0EA5E9)
                    )
                }
            }
        }
    }
}

// ── Row 2: Facturation + Modules par catégorie ───────────────────────────────

@Composable
fun GroupeChartsRow2(
    stats: GroupeDashboardStatsDto,
    categoriesWithModules: List<CategorieWithModulesDto>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Gauche: Facturation (BarChart Facturé vs Payé)
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Receipt, null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(18.dp))
                    Text("Facturation abonnement", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                }
                val barEntries = listOf(
                    ChartEntry("Facturé", stats.facturationTotaleXAF.toFloat(), Color(0xFF0EA5E9)),
                    ChartEntry("Payé", stats.montantPayeXAF.toFloat(), Color(0xFF059669)),
                    ChartEntry("Dû", stats.montantDuXAF.toFloat(), Color(0xFFEF4444))
                )
                BarChart(entries = barEntries, modifier = Modifier.fillMaxWidth().height(140.dp))
            }
        }

        // Droite: Modules par catégorie (DonutChart)
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PieChart, null, tint = Color(0xFFEC4899), modifier = Modifier.size(18.dp))
                    Text("Modules par catégorie", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                }
                val catColors = listOf(
                    Color(0xFF3B82F6), Color(0xFF059669), Color(0xFF7C3AED),
                    Color(0xFFF59E0B), Color(0xFFEC4899), Color(0xFF0EA5E9)
                )
                val donutEntries = categoriesWithModules.mapIndexed { idx, cat ->
                    ChartEntry(cat.nom, cat.modules.size.toFloat(), catColors[idx % catColors.size])
                }
                if (donutEntries.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("Aucune catégorie", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                } else {
                    DonutChart(entries = donutEntries, modifier = Modifier.fillMaxWidth().height(140.dp))
                }
            }
        }
    }
}

// ── Row 3: Évolution facturation (LineChart) ─────────────────────────────────

@Composable
fun GroupeInvoiceTimelineChart(invoiceTimeline: List<MonthlyInvoiceStatsDto>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                Text("Évolution facturation", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
            }

            if (invoiceTimeline.size < 2) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.ShowChart, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Pas assez de données pour afficher le graphe", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Text("(minimum 2 mois)", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                    }
                }
            } else {
                val totalSeries = LineChartSeries(
                    entries = invoiceTimeline.map { ChartEntry(it.month.takeLast(5), it.totalXAF.toFloat(), Color(0xFF0EA5E9)) },
                    lineColor = Color(0xFF0EA5E9),
                    label = "Facturé"
                )
                val paidSeries = LineChartSeries(
                    entries = invoiceTimeline.map { ChartEntry(it.month.takeLast(5), it.paidXAF.toFloat(), Color(0xFF059669)) },
                    lineColor = Color(0xFF059669),
                    label = "Payé"
                )
                LineChart(
                    series = listOf(totalSeries, paidSeries),
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            }
        }
    }
}

// ── Alerte factures en retard ────────────────────────────────────────────────

@Composable
fun GroupeOverdueAlert(nbFacturesEnRetard: Long) {
    if (nbFacturesEnRetard <= 0) return

    val animAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2).copy(alpha = animAlpha))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$nbFacturesEnRetard facture${if (nbFacturesEnRetard > 1) "s" else ""} en retard",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF991B1B)
                )
                Text(
                    "Contactez E-PILOTE Congo pour régulariser votre situation",
                    fontSize = 12.sp,
                    color = Color(0xFFB91C1C)
                )
            }
        }
    }
}
