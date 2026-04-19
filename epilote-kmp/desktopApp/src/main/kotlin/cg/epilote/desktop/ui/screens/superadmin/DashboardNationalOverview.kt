package cg.epilote.desktop.ui.screens.superadmin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.PlanDistributionDto
import cg.epilote.desktop.data.ProvinceStatsDto
import cg.epilote.desktop.ui.components.*

// ── Section "Vue d'ensemble nationale" ──────────────────────────────────────

@Composable
fun NationalOverviewSection(stats: AdminStats) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Titre
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Public,
                contentDescription = null,
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    "Vue d'ensemble nationale",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    "République du Congo — Couverture éducative",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // 4 mini-cartes : Élèves, Enseignants, Écoles, Inscriptions
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MiniMetricCard(
                Modifier.weight(1f),
                icon = Icons.Default.People,
                iconBg = Color(0xFFDBEAFE),
                iconTint = Color(0xFF3B82F6),
                label = "Élèves",
                value = "0"
            )
            MiniMetricCard(
                Modifier.weight(1f),
                icon = Icons.Default.School,
                iconBg = Color(0xFFD1FAE5),
                iconTint = Color(0xFF059669),
                label = "Enseignants",
                value = "0"
            )
            MiniMetricCard(
                Modifier.weight(1f),
                icon = Icons.Default.AccountBalance,
                iconBg = Color(0xFFD1FAE5),
                iconTint = Color(0xFF10B981),
                label = "Écoles",
                value = formatNumber(stats.totalEcoles)
            )
            MiniMetricCard(
                Modifier.weight(1f),
                icon = Icons.Default.HowToReg,
                iconBg = Color(0xFFFEF3C7),
                iconTint = Color(0xFFF59E0B),
                label = "Inscriptions",
                value = "0"
            )
        }
    }
}

// ── Section graphiques ──────────────────────────────────────────────────────

private val chartColors = listOf(
    Color(0xFF2A9D8F), Color(0xFF1D3557), Color(0xFFE9C46A), Color(0xFFE63946),
    Color(0xFF6C5CE7), Color(0xFF00875A), Color(0xFFFF6B6B), Color(0xFF48BFE3),
    Color(0xFFA8DADC), Color(0xFF457B9D), Color(0xFFF4A261), Color(0xFF264653),
    Color(0xFF06D6A0), Color(0xFFEF476F), Color(0xFFFFD166)
)

@Composable
fun DashboardChartsRow1(stats: AdminStats) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
    ) {
        // Couverture par département
        ChartCard(
            title = "Couverture par Département",
            icon = Icons.Default.LocationOn,
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            val entries = stats.groupesByProvince
                .filter { it.groupesCount > 0 }
                .sortedByDescending { it.groupesCount }
            if (entries.isEmpty()) {
                EmptyChartPlaceholder(
                    Icons.Default.LocationOn,
                    "Aucune donnée disponible",
                    "Les groupes doivent avoir une ville ou un département renseigné"
                )
            } else {
                HorizontalBarChart(
                    entries = entries.mapIndexed { i, p ->
                        ChartEntry(p.province, p.groupesCount.toFloat(), chartColors[i % chartColors.size])
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }

        // Couverture — anneaux de progression
        ChartCard(
            title = "Couverture nationale",
            icon = Icons.Default.DonutSmall,
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            val totalDepts = congoDepartments.size.toFloat()
            val deptsCouverts = stats.groupesByProvince.count { it.groupesCount > 0 }
            val couverture = if (totalDepts > 0) deptsCouverts / totalDepts else 0f
            val aboPct = if (stats.totalSubscriptions > 0)
                stats.activeSubscriptions.toFloat() / stats.totalSubscriptions else 0f
            val revPct = if (stats.revenueTotal > 0)
                stats.revenuePaid.toFloat() / stats.revenueTotal else 0f
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProgressRing(couverture, "Départements",
                    "${(couverture * 100).toInt()}%", Color(0xFF3B82F6))
                ProgressRing(aboPct, "Abonnements",
                    "${(aboPct * 100).toInt()}%", Color(0xFF10B981))
                ProgressRing(revPct, "Recouvrement",
                    "${(revPct * 100).toInt()}%", Color(0xFFF59E0B))
            }
        }
    }
}

@Composable
fun DashboardChartsRow2(stats: AdminStats) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
    ) {
        // Performance financière
        ChartCard(
            title = "Performance financière",
            icon = Icons.Default.BarChart,
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            BarChart(
                entries = listOf(
                    ChartEntry("Facturé", stats.revenueTotal.toFloat(), Color(0xFF1E40AF)),
                    ChartEntry("Encaissé", stats.revenuePaid.toFloat(), Color(0xFF10B981))
                ),
                modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 8.dp)
            )
        }

        // Répartition des plans
        ChartCard(
            title = "Répartition des plans",
            icon = Icons.Default.PieChart,
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            val plans = stats.planDistribution.sortedByDescending { it.groupesCount }
            DonutChart(
                entries = plans.mapIndexed { i, p ->
                    ChartEntry(p.planNom, p.groupesCount.toFloat(), chartColors[i % chartColors.size])
                },
                modifier = Modifier.fillMaxWidth().height(140.dp).padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun DashboardChartsRow3(stats: AdminStats) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
    ) {
        // Statut des abonnements
        ChartCard(
            title = "Statut des abonnements",
            icon = Icons.Default.Subscriptions,
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            val statusColors = mapOf(
                "active" to Color(0xFF10B981), "expired" to Color(0xFFEF4444),
                "suspended" to Color(0xFFF59E0B), "cancelled" to Color(0xFF94A3B8)
            )
            val statusLabels = mapOf(
                "active" to "Actif", "expired" to "Expiré",
                "suspended" to "Suspendu", "cancelled" to "Annulé"
            )
            DonutChart(
                entries = stats.subscriptionsByStatus.map { (status, count) ->
                    ChartEntry(
                        statusLabels[status] ?: status,
                        count.toFloat(),
                        statusColors[status] ?: Color(0xFF94A3B8)
                    )
                },
                modifier = Modifier.fillMaxWidth().height(140.dp).padding(top = 8.dp)
            )
        }
    }
}

// ── Composants réutilisables ────────────────────────────────────────────────

@Composable
private fun MiniMetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(label, fontSize = 12.sp, color = Color(0xFF94A3B8))
                Text(
                    value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }
        }
    }
}

@Composable
fun ChartCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A)
                )
            }
            content()
        }
    }
}

@Composable
private fun EmptyChartPlaceholder(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(36.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF94A3B8))
        Text(
            subtitle,
            fontSize = 11.sp,
            color = Color(0xFFCBD5E1),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
