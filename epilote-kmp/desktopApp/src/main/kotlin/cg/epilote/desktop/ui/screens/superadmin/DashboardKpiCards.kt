package cg.epilote.desktop.ui.screens.superadmin

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Ligne 1 : KPIs principaux (4 cartes du screenshot) ─────────────────────

@Composable
fun DashboardMainKpiRow(
    stats: AdminStats,
    onNavigateGroupes: () -> Unit = {}
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // KPIs honnêtes : aucune tendance mensuelle n'est disponible côté backend pour l'instant,
        // nous évitons donc les pourcentages fictifs et affichons des métriques calculées à partir
        // des données réelles de AdminStats.
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Business,
            iconBg = Color(0xFFDBEAFE),
            iconTint = Color(0xFF3B82F6),
            label = "Groupes Scolaires",
            value = formatNumber(stats.totalGroupes),
            trend = "",
            trendLabel = "${stats.groupesActifs} actifs",
            onClick = onNavigateGroupes
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CreditCard,
            iconBg = Color(0xFFD1FAE5),
            iconTint = Color(0xFF059669),
            label = "Abonnements actifs",
            value = formatNumber(stats.activeSubscriptions),
            trend = "",
            trendLabel = "sur ${formatNumber(stats.totalSubscriptions)} total"
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Payments,
            iconBg = Color(0xFFD1FAE5),
            iconTint = Color(0xFF10B981),
            label = "Revenus encaissés",
            value = formatXAF(stats.revenuePaid),
            trend = "",
            trendLabel = "sur ${formatXAF(stats.revenueTotal)} émis"
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.School,
            iconBg = Color(0xFFDBEAFE),
            iconTint = Color(0xFF3B82F6),
            label = "Écoles",
            value = formatNumber(stats.totalEcoles),
            trend = "",
            trendLabel = "sur la plateforme"
        )
    }
}

// ── Ligne 2 : KPIs secondaires (utilisateurs, factures, modules) ────────────

@Composable
fun DashboardSecondaryKpiRow(
    stats: AdminStats,
    onNavigateModules: () -> Unit = {},
    onNavigatePlans: () -> Unit = {}
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.People,
            iconBg = Color(0xFFEDE9FE),
            iconTint = Color(0xFF7C3AED),
            label = "Utilisateurs actifs",
            value = formatNumber(stats.totalUtilisateurs),
            trend = "",
            trendLabel = "sur la plateforme"
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Receipt,
            iconBg = Color(0xFFFEF3C7),
            iconTint = Color(0xFFF59E0B),
            label = "Factures émises",
            value = formatNumber(stats.totalInvoices),
            trend = if (stats.invoicesOverdue > 0) "⚠" else "",
            trendLabel = "${stats.invoicesOverdue} en retard",
            trendColor = if (stats.invoicesOverdue > 0) Color(0xFFEF4444) else Color(0xFF10B981)
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Extension,
            iconBg = Color(0xFFFCE7F3),
            iconTint = Color(0xFFEC4899),
            label = "Modules",
            value = formatNumber(stats.totalModules),
            trend = "",
            trendLabel = "${stats.totalCategories} catégories",
            onClick = onNavigateModules
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CreditCard,
            iconBg = Color(0xFFE0F2FE),
            iconTint = Color(0xFF0EA5E9),
            label = "Plans disponibles",
            value = formatNumber(stats.totalPlans),
            trend = "",
            trendLabel = "${stats.totalCategories} catégories",
            onClick = onNavigatePlans
        )
    }
}

// ── Composant carte KPI unitaire ────────────────────────────────────────────

@Composable
fun KpiCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    trend: String = "",
    trendLabel: String = "",
    trendColor: Color = Color(0xFF10B981),
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val elevation by animateFloatAsState(
        targetValue = if (isHovered) 8f else 1f,
        animationSpec = tween(200)
    )

    Card(
        modifier = modifier
            .graphicsLayer { shadowElevation = elevation }
            .hoverable(interactionSource)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icône
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }

            // Label
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )

            // Valeur
            Text(
                value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Tendance
            if (trend.isNotEmpty() || trendLabel.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (trend.isNotEmpty()) {
                        Text(
                            trend,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = trendColor
                        )
                    }
                    Text(
                        trendLabel,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
