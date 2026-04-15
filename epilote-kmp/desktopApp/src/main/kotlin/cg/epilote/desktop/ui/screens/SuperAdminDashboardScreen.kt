package cg.epilote.desktop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.GroupeApiDto
import cg.epilote.desktop.InvoiceApiDto
import cg.epilote.desktop.PlanDistributionDto
import cg.epilote.desktop.ProvinceStatsDto
import cg.epilote.desktop.ui.components.*
import cg.epilote.desktop.ui.theme.*
import cg.epilote.shared.domain.model.UserSession
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class AdminStats(
    val totalGroupes: Long = 0,
    val totalEcoles: Long = 0,
    val totalUtilisateurs: Long = 0,
    val totalModules: Long = 0,
    val totalPlans: Long = 0,
    val totalCategories: Long = 0,
    val totalSubscriptions: Long = 0,
    val activeSubscriptions: Long = 0,
    val totalInvoices: Long = 0,
    val revenueTotal: Long = 0,
    val revenuePaid: Long = 0,
    val invoicesOverdue: Long = 0,
    val groupesByProvince: List<ProvinceStatsDto> = emptyList(),
    val planDistribution: List<PlanDistributionDto> = emptyList(),
    val subscriptionsByStatus: Map<String, Long> = emptyMap(),
    val recentGroupes: List<GroupeApiDto> = emptyList(),
    val recentInvoices: List<InvoiceApiDto> = emptyList()
)

private val xafFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)
private fun formatXAF(amount: Long): String = "${xafFormat.format(amount)} XAF"
private fun formatDate(ts: Long): String =
    if (ts > 0) SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(ts)) else "—"

private val chartColors = listOf(
    Color(0xFF2A9D8F), Color(0xFF1D3557), Color(0xFFE9C46A), Color(0xFFE63946),
    Color(0xFF6C5CE7), Color(0xFF00875A), Color(0xFFFF6B6B), Color(0xFF48BFE3),
    Color(0xFFA8DADC), Color(0xFF457B9D), Color(0xFFF4A261), Color(0xFF264653),
    Color(0xFF06D6A0), Color(0xFFEF476F), Color(0xFFFFD166)
)

@Composable
fun SuperAdminDashboardScreen(
    session: UserSession,
    stats: AdminStats,
    isLoading: Boolean,
    onNavigateGroupes: () -> Unit,
    onNavigatePlans: () -> Unit,
    onNavigateModules: () -> Unit,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    val provinces = stats.groupesByProvince.sortedByDescending { it.groupesCount }
    val plans = stats.planDistribution.sortedByDescending { it.groupesCount }
    val subStatus = stats.subscriptionsByStatus
    val recentGroupes = stats.recentGroupes
    val recentInvoices = stats.recentInvoices

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8))
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ═══════════════════════════════════════════════════════════
        // SECTION 1 : Bandeau premium en-tête avec gradient
        // ═══════════════════════════════════════════════════════════
        DashboardHeroBanner(
            session = session,
            stats = stats,
            isLoading = isLoading,
            onRefresh = onRefresh
        )

        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ═══════════════════════════════════════════════════════════
            // SECTION 2 : KPI premium — ligne 1
            // ═══════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 40 }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PremiumKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Business,
                        accentColor = Color(0xFF2A9D8F),
                        targetValue = stats.totalGroupes,
                        label = "Groupes Scolaires",
                        subtitle = "${stats.totalEcoles} écoles rattachées",
                        sparkData = listOf(0f, 1f, 1f, 2f, 2f, 3f, stats.totalGroupes.toFloat()),
                        onClick = onNavigateGroupes
                    )
                    PremiumKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.People,
                        accentColor = Color(0xFF3B82F6),
                        targetValue = stats.totalUtilisateurs,
                        label = "Utilisateurs actifs",
                        subtitle = "sur la plateforme",
                        sparkData = listOf(0f, 0f, 1f, 1f, 1f, 1f, stats.totalUtilisateurs.toFloat())
                    )
                    PremiumKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Subscriptions,
                        accentColor = Color(0xFF8B5CF6),
                        targetValue = stats.activeSubscriptions,
                        label = "Abonnements actifs",
                        subtitle = "sur ${stats.totalSubscriptions} total",
                        sparkData = listOf(0f, 0f, 0f, 1f, 1f, stats.activeSubscriptions.toFloat())
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 2b : KPI premium — ligne 2 (revenus)
            // ═══════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600, 100)) + slideInVertically(tween(600, 100)) { 40 }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PremiumKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Payments,
                        accentColor = Color(0xFF10B981),
                        targetValue = stats.revenuePaid,
                        label = "Revenus encaissés",
                        subtitle = "facturé : ${formatXAF(stats.revenueTotal)}",
                        displayAsXAF = true,
                        sparkData = listOf(0f, stats.revenuePaid.toFloat() * 0.3f, stats.revenuePaid.toFloat() * 0.7f, stats.revenuePaid.toFloat())
                    )
                    PremiumKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Receipt,
                        accentColor = Color(0xFFF59E0B),
                        targetValue = stats.totalInvoices,
                        label = "Factures émises",
                        subtitle = "${stats.invoicesOverdue} en retard",
                        sparkData = listOf(0f, stats.totalInvoices.toFloat())
                    )
                    PremiumKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        accentColor = Color(0xFFEF4444),
                        targetValue = stats.invoicesOverdue,
                        label = "Factures impayées",
                        subtitle = "nécessitent une relance",
                        isAlert = stats.invoicesOverdue > 0,
                        sparkData = listOf(0f, stats.invoicesOverdue.toFloat())
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 2c : KPI premium — ligne 3 (modules + plans)
            // ═══════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700, 200)) + slideInVertically(tween(700, 200)) { 40 }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PremiumKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Extension,
                        accentColor = Color(0xFFEC4899),
                        targetValue = stats.totalModules,
                        label = "Modules",
                        subtitle = "${stats.totalCategories} catégories",
                        sparkData = listOf(0f, stats.totalModules.toFloat()),
                        onClick = onNavigateModules
                    )
                    PremiumKpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CreditCard,
                        accentColor = Color(0xFF0EA5E9),
                        targetValue = stats.totalPlans,
                        label = "Plans d'abonnement",
                        subtitle = "configurés",
                        sparkData = listOf(0f, stats.totalPlans.toFloat()),
                        onClick = onNavigatePlans
                    )
                    Spacer(Modifier.weight(1f))
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 3 : Graphiques (ligne 1)
            // ═══════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800, 300)) + slideInVertically(tween(800, 300)) { 40 }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PremiumChartCard(
                        title = "Performance financière",
                        subtitle = "Montants facturés vs encaissés",
                        modifier = Modifier.weight(1.5f)
                    ) {
                        BarChart(
                            entries = listOf(
                                ChartEntry("Facturé", stats.revenueTotal.toFloat(), Color(0xFF1E40AF)),
                                ChartEntry("Encaissé", stats.revenuePaid.toFloat(), Color(0xFF10B981))
                            ),
                            modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 8.dp)
                        )
                    }

                    PremiumChartCard(
                        title = "Répartition des plans",
                        subtitle = "Distribution par type d'abonnement",
                        modifier = Modifier.weight(1f)
                    ) {
                        DonutChart(
                            entries = plans.mapIndexed { i, p ->
                                ChartEntry(p.planNom, p.groupesCount.toFloat(), chartColors[i % chartColors.size])
                            },
                            modifier = Modifier.fillMaxWidth().height(140.dp).padding(top = 8.dp)
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 3b : Graphiques (ligne 2)
            // ═══════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(900, 400)) + slideInVertically(tween(900, 400)) { 40 }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PremiumChartCard(
                        title = "Groupes par département",
                        subtitle = "Répartition géographique — Congo (15 départements)",
                        modifier = Modifier.weight(1f)
                    ) {
                        HorizontalBarChart(
                            entries = provinces.filter { it.groupesCount > 0 }.mapIndexed { i, p ->
                                ChartEntry(p.province, p.groupesCount.toFloat(), chartColors[i % chartColors.size])
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }

                    PremiumChartCard(
                        title = "Statut des abonnements",
                        subtitle = "Répartition par état",
                        modifier = Modifier.weight(1f)
                    ) {
                        val statusColors = mapOf(
                            "active" to Color(0xFF10B981), "expired" to Color(0xFFEF4444),
                            "suspended" to Color(0xFFF59E0B), "cancelled" to Color(0xFF94A3B8)
                        )
                        val statusLabels = mapOf(
                            "active" to "Actif", "expired" to "Expiré",
                            "suspended" to "Suspendu", "cancelled" to "Annulé"
                        )
                        BarChart(
                            entries = subStatus.map { (status, count) ->
                                ChartEntry(
                                    statusLabels[status] ?: status,
                                    count.toFloat(),
                                    statusColors[status] ?: Color(0xFF94A3B8)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 8.dp)
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 4 : Alertes intelligentes
            // ═══════════════════════════════════════════════════════════
            if (stats.invoicesOverdue > 0) {
                AlertsSection(stats.invoicesOverdue)
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 5 : Activité récente + factures
            // ═══════════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 500)) + slideInVertically(tween(1000, 500)) { 40 }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RecentGroupesCard(
                        groupes = recentGroupes,
                        modifier = Modifier.weight(1f),
                        onNavigateGroupes = onNavigateGroupes
                    )
                    RecentInvoicesCard(
                        invoices = recentInvoices,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 6 : Actions rapides
            // ═══════════════════════════════════════════════════════════
            Text("Actions rapides", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ActionCard(Modifier.weight(1f), "Nouveau Groupe",
                    "Créer un groupe scolaire", Icons.Default.AddBusiness, Color(0xFF2A9D8F), onNavigateGroupes)
                ActionCard(Modifier.weight(1f), "Gérer les Plans",
                    "Configurer les abonnements", Icons.Default.CreditCard, Color(0xFF1E40AF), onNavigatePlans)
                ActionCard(Modifier.weight(1f), "Modules & Catégories",
                    "Référentiel fonctionnel", Icons.Default.Extension, Color(0xFF8B5CF6), onNavigateModules)
                ActionCard(Modifier.weight(1f), "Annonces globales",
                    "Diffuser aux groupes", Icons.Default.Campaign, Color(0xFFF59E0B)) {}
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 7 : Pied système
            // ═══════════════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("E-PILOTE CONGO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("v1.0.0", color = Color(0xFF64748B), fontSize = 11.sp)
                        Text("•", color = Color(0xFF64748B), fontSize = 11.sp)
                        Text("Spring Boot + Couchbase Capella", color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PulsatingDot(Color(0xFF10B981), "Backend")
                        PulsatingDot(Color(0xFFF59E0B), "IA")
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
// Composables privés réutilisables — Premium SaaS Design
// ══════════════════════════════════════════════════════════════════════

@Composable
private fun DashboardHeroBanner(
    session: UserSession,
    stats: AdminStats,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E3A5F), Color(0xFF2A9D8F)),
        start = Offset.Zero,
        end = Offset(1200f, 300f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradientBrush)
            .padding(horizontal = 32.dp, vertical = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF2A9D8F), Color(0xFF10B981))),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${session.firstName.firstOrNull() ?: "S"}${session.lastName.firstOrNull() ?: "A"}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Column {
                        Text(
                            "Bonjour, ${session.firstName} ${session.lastName}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Tableau de bord — Pilotage National",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HeroPill(Icons.Default.Business, "${stats.totalGroupes} groupes", Color(0xFF2A9D8F))
                    HeroPill(Icons.Default.People, "${stats.totalUtilisateurs} utilisateurs", Color(0xFF3B82F6))
                    HeroPill(Icons.Default.School, "${stats.totalEcoles} écoles", Color(0xFF8B5CF6))
                    HeroPill(Icons.Default.Payments, formatXAF(stats.revenuePaid), Color(0xFF10B981))
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Actualiser", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Text(
                    "E-PILOTE CONGO • ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.FRANCE).format(Date())}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun HeroPill(icon: ImageVector, text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun PremiumKpiCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    accentColor: Color,
    targetValue: Long,
    label: String,
    subtitle: String = "",
    displayAsXAF: Boolean = false,
    isAlert: Boolean = false,
    sparkData: List<Float> = emptyList(),
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.03f else 1f,
        animationSpec = tween(200)
    )
    val elevationPx by animateFloatAsState(
        targetValue = if (isHovered) 12f else 2f,
        animationSpec = tween(200)
    )

    val animatedValue by animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
    )

    val bgColor = if (isAlert) Color(0xFFFEF2F2) else Color.White
    val tintBg = accentColor.copy(alpha = 0.06f)

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevationPx.dp, RoundedCornerShape(18.dp), ambientColor = accentColor.copy(alpha = 0.15f))
            .hoverable(interactionSource)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor, accentColor.copy(alpha = 0.3f))
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(tintBg, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
                    }

                    if (sparkData.size >= 2) {
                        KpiSparkLine(
                            values = sparkData,
                            color = accentColor,
                            modifier = Modifier.width(60.dp).height(28.dp)
                        )
                    } else if (onClick != null) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(accentColor.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ChevronRight, null, tint = accentColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    if (displayAsXAF) formatXAF(animatedValue.toLong()) else xafFormat.format(animatedValue.toLong()),
                    fontSize = if (displayAsXAF) 20.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        fontSize = 11.sp,
                        color = EpiloteTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiSparkLine(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (values.size < 2) return
    val maxV = values.max().coerceAtLeast(1f)
    val minV = values.min()
    Canvas(modifier = modifier) {
        val stepX = size.width / (values.size - 1)
        val range = (maxV - minV).coerceAtLeast(1f)
        val path = Path()
        val fillPath = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - minV) / range) * size.height * 0.85f
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()
        drawPath(fillPath, Brush.verticalGradient(listOf(color.copy(alpha = 0.2f), Color.Transparent)))
        drawPath(path, color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
    }
}

@Composable
private fun PremiumChartCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFF0F172A).copy(alpha = 0.08f)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
            Text(subtitle, fontSize = 11.sp, color = EpiloteTextMuted)
            content()
        }
    }
}

@Composable
private fun AlertsSection(overdueCount: Long) {
    val pulseAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer { alpha = pulseAlpha }
                    .background(
                        Brush.radialGradient(listOf(Color(0xFFEF4444).copy(alpha = 0.25f), Color.Transparent)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Alertes critiques", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFEF4444))
                Text(
                    "$overdueCount facture(s) impayée(s) nécessitent une relance immédiate",
                    fontSize = 12.sp, color = Color(0xFF991B1B)
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEF4444)
            ) {
                Text(
                    "Voir les factures", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentGroupesCard(
    groupes: List<GroupeApiDto>,
    modifier: Modifier = Modifier,
    onNavigateGroupes: () -> Unit
) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFF0F172A).copy(alpha = 0.06f)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).background(Color(0xFF2A9D8F).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Business, null, tint = Color(0xFF2A9D8F), modifier = Modifier.size(16.dp))
                    }
                    Text("Derniers groupes créés", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                }
                TextButton(onClick = onNavigateGroupes) {
                    Text("Voir tout", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2A9D8F))
                }
            }
            if (groupes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Aucune création récente", fontSize = 12.sp, color = EpiloteTextMuted)
                    }
                }
            } else {
                groupes.forEach { g ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).background(Color(0xFF2A9D8F).copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        g.nom.firstOrNull()?.uppercase() ?: "G",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF2A9D8F)
                                    )
                                }
                                Column {
                                    Text(g.nom, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                    Text(g.province, fontSize = 11.sp, color = EpiloteTextMuted)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${g.ecolesCount} école(s)", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(formatDate(g.createdAt), fontSize = 10.sp, color = EpiloteTextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentInvoicesCard(
    invoices: List<InvoiceApiDto>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(18.dp), ambientColor = Color(0xFF0F172A).copy(alpha = 0.06f)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).background(Color(0xFFF59E0B).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Receipt, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    }
                    Text("Dernières factures", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                }
                TextButton(onClick = {}) {
                    Text("Voir tout", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2A9D8F))
                }
            }
            if (invoices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Receipt, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Aucune facture récente", fontSize = 12.sp, color = EpiloteTextMuted)
                    }
                }
            } else {
                invoices.forEach { inv ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(inv.reference, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                Text(formatDate(inv.dateEmission), fontSize = 11.sp, color = EpiloteTextMuted)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatXAF(inv.montantXAF), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                InvoiceStatusBadge(inv.statut)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(if (isHovered) 1.04f else 1f, tween(150))

    Card(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .hoverable(interactionSource)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = if (isHovered) 0.12f else 0.06f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).background(color.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF0F172A))
                Text(description, fontSize = 10.sp, color = EpiloteTextMuted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun InvoiceStatusBadge(statut: String) {
    val (bg, fg, text) = when (statut) {
        "paid"      -> Triple(Color(0xFFD1FAE5), Color(0xFF059669), "Payée")
        "sent"      -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), "Envoyée")
        "overdue"   -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "En retard")
        "cancelled" -> Triple(Color(0xFFF1F5F9), Color(0xFF94A3B8), "Annulée")
        else        -> Triple(Color(0xFFF8FAFC), Color(0xFF64748B), "Brouillon")
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun PulsatingDot(color: Color, label: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
    )

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale; alpha = pulseAlpha }
                    .background(color, CircleShape)
            )
            Box(modifier = Modifier.size(7.dp).background(color, CircleShape))
        }
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
