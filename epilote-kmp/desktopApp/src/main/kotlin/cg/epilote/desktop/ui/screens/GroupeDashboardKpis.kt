package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import cg.epilote.desktop.data.*
import cg.epilote.desktop.ui.screens.superadmin.*
import cg.epilote.desktop.ui.theme.cursorHand
import cg.epilote.shared.domain.model.UserSession

// ── WelcomeSection ───────────────────────────────────────────────────────────

@Composable
fun GroupeWelcomeSection(
    session: UserSession,
    groupeNom: String,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Bienvenue, ${session.firstName} \uD83D\uDC4B",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (groupeNom.isNotBlank()) {
                    Text(groupeNom, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF059669))
                    Text("•", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
                Text("E-Pilot Congo \uD83C\uDDE8\uD83C\uDDEC", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF059669))
                Text("•", fontSize = 13.sp, color = Color(0xFF94A3B8))
                Text(formatDateLong(), fontSize = 13.sp, color = Color(0xFF94A3B8))
            }
        }
        OutlinedButton(
            onClick = onRefresh,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.cursorHand()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text("Actualiser", fontSize = 13.sp)
        }
    }
}

// ── Offline Banner ───────────────────────────────────────────────────────────

@Composable
fun GroupeOfflineBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFEF3C7),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.WifiOff, null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
            Column {
                Text("Mode hors ligne", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF92400E))
                Text("Les données affichées proviennent du cache local. Les modifications sont désactivées.", fontSize = 11.sp, color = Color(0xFFB45309))
            }
        }
    }
}

// ── Plan Banner Card ─────────────────────────────────────────────────────────

@Composable
fun GroupePlanBannerCard(stats: GroupeDashboardStatsDto, onClick: () -> Unit) {
    val now = System.currentTimeMillis()
    val daysUntilExpiry = if (stats.abonnementDateFin > 0) {
        ((stats.abonnementDateFin - now) / (1000L * 60 * 60 * 24)).toInt()
    } else -1

    val isExpired  = stats.abonnementStatut.lowercase() == "expired" || (stats.abonnementDateFin in 1 until now)
    val isExpiring = !isExpired && daysUntilExpiry in 0..30

    val bgColor   = when { isExpired -> Color(0xFFEF4444); isExpiring -> Color(0xFFF59E0B); else -> Color(0xFF059669) }
    val bgLight   = when { isExpired -> Color(0xFFFFEDED); isExpiring -> Color(0xFFFFF3CD); else -> Color(0xFFECFDF5) }
    val textColor = when { isExpired -> Color(0xFF7F1D1D); isExpiring -> Color(0xFF78350F); else -> Color(0xFF014737) }
    val badgeLabel = when { isExpired -> "EXPIRÉ"; isExpiring -> "EXPIRE BIENTÔT"; else -> "ACTIF" }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).cursorHand(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(52.dp).background(bgColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CreditCard, null, tint = bgColor, modifier = Modifier.size(28.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stats.planNom.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Surface(shape = RoundedCornerShape(20.dp), color = bgColor) {
                            Text(badgeLabel, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stats.planType.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = textColor.copy(alpha = 0.7f))
                        if (stats.prixXAF > 0) {
                            Text("•", fontSize = 12.sp, color = textColor.copy(alpha = 0.4f))
                            Text(formatXAF(stats.prixXAF) + "/an", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor)
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (stats.abonnementDateFin > 0) {
                    Text(
                        if (isExpired) "Expiré le ${formatDate(stats.abonnementDateFin)}" else "Expire le ${formatDate(stats.abonnementDateFin)}",
                        fontSize = 12.sp, color = textColor.copy(alpha = 0.8f)
                    )
                }
                if (isExpiring && daysUntilExpiry >= 0) {
                    Text("Dans $daysUntilExpiry jour${if (daysUntilExpiry > 1) "s" else ""}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = bgColor)
                }
                if (stats.renouvellementAuto) {
                    Text("↻ Renouvellement auto", fontSize = 11.sp, color = textColor.copy(alpha = 0.6f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Voir les détails", fontSize = 12.sp, color = bgColor, fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.ChevronRight, null, tint = bgColor, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ── KPI Row Organisation ─────────────────────────────────────────────────────

@Composable
fun GroupeOrgKpiRow(
    stats: GroupeDashboardStatsDto,
    onNavigateEcoles: () -> Unit,
    onNavigateUsers: () -> Unit,
    onNavigateProfils: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.School, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF3B82F6), label = "Écoles", value = formatNumber(stats.nbEcoles), trendLabel = "dans votre groupe", onClick = onNavigateEcoles)
        KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.People, iconBg = Color(0xFFEDE9FE), iconTint = Color(0xFF7C3AED), label = "Utilisateurs", value = formatNumber(stats.nbUtilisateurs), trendLabel = "tous rôles confondus", onClick = onNavigateUsers)
        KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Tune, iconBg = Color(0xFFFCE7F3), iconTint = Color(0xFFEC4899), label = "Profils d'accès", value = formatNumber(stats.nbProfils), trendLabel = "profils configurés", onClick = onNavigateProfils)
        KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Extension, iconBg = Color(0xFFFEF3C7), iconTint = Color(0xFFF59E0B), label = "Modules actifs", value = formatNumber(stats.nbModulesActifs), trendLabel = "inclus dans le plan")
    }
}

// ── KPI Row Monétisation ─────────────────────────────────────────────────────

@Composable
fun GroupeMonetisationRow(stats: GroupeDashboardStatsDto) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Receipt, iconBg = Color(0xFFF0F9FF), iconTint = Color(0xFF0EA5E9), label = "Total facturé", value = formatXAF(stats.facturationTotaleXAF), trendLabel = "${stats.nbFactures} facture${if (stats.nbFactures > 1) "s" else ""}")
        GroupePaymentProgressCard(stats = stats, modifier = Modifier.weight(1f))
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = if (stats.nbFacturesEnRetard > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
            iconBg = if (stats.nbFacturesEnRetard > 0) Color(0xFFFEE2E2) else Color(0xFFD1FAE5),
            iconTint = if (stats.nbFacturesEnRetard > 0) Color(0xFFEF4444) else Color(0xFF059669),
            label = "Factures en retard",
            value = formatNumber(stats.nbFacturesEnRetard),
            trendLabel = if (stats.nbFacturesEnRetard == 0L) "Tout est à jour ✓" else "À régulariser",
            trendColor = if (stats.nbFacturesEnRetard > 0) Color(0xFFEF4444) else Color(0xFF059669)
        )
    }
}

@Composable
fun GroupePaymentProgressCard(stats: GroupeDashboardStatsDto, modifier: Modifier = Modifier) {
    val progress = if (stats.facturationTotaleXAF > 0)
        (stats.montantPayeXAF.toFloat() / stats.facturationTotaleXAF.toFloat()).coerceIn(0f, 1f)
    else 0f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(44.dp).background(Color(0xFFD1FAE5), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Payments, null, tint = Color(0xFF059669), modifier = Modifier.size(22.dp))
            }
            Text("Montant payé", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
            Text(formatXAF(stats.montantPayeXAF), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = Color(0xFF059669),
                trackColor = Color(0xFFE2E8F0)
            )
            Text("${(progress * 100).toInt()}% du total facturé", fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
    }
}

// ── Modules Section ──────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupeModulesSection(modules: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Modules inclus dans votre plan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val chipColors = listOf(
                    Color(0xFFDBEAFE) to Color(0xFF3B82F6),
                    Color(0xFFD1FAE5) to Color(0xFF059669),
                    Color(0xFFEDE9FE) to Color(0xFF7C3AED),
                    Color(0xFFFCE7F3) to Color(0xFFEC4899),
                    Color(0xFFFEF3C7) to Color(0xFFF59E0B),
                    Color(0xFFE0F2FE) to Color(0xFF0EA5E9)
                )
                modules.forEachIndexed { index, module ->
                    val (bg, fg) = chipColors[index % chipColors.size]
                    Surface(shape = RoundedCornerShape(20.dp), color = bg) {
                        Text(module, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = fg)
                    }
                }
            }
        }
    }
}

// ── Écoles Section (résumé) ──────────────────────────────────────────────────

@Composable
fun GroupeEcolesSection(ecoles: List<EcoleApiDto>, onNavigateEcoles: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Vos écoles", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                TextButton(onClick = onNavigateEcoles, modifier = Modifier.cursorHand()) {
                    Text("Gérer", fontSize = 12.sp)
                    Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
                }
            }
            if (ecoles.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("Aucune école enregistrée.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Nom", modifier = Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                        Text("Province", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                        Text("Territoire", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                        Text("Niveaux", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                    }
                    ecoles.take(8).forEachIndexed { idx, ecole ->
                        val rowBg = if (idx % 2 == 0) Color.White else Color(0xFFF8FAFC)
                        Row(
                            modifier = Modifier.fillMaxWidth().background(rowBg).padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ecole.nom, modifier = Modifier.weight(2f), fontSize = 13.sp, color = Color(0xFF1E293B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(ecole.province, modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(ecole.territoire, modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(ecole.niveaux.joinToString(", "), modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (idx < ecoles.take(8).lastIndex) HorizontalDivider(color = Color(0xFFE2E8F0))
                    }
                    if (ecoles.size > 8) {
                        TextButton(onClick = onNavigateEcoles, modifier = Modifier.fillMaxWidth().cursorHand()) {
                            Text("Voir les ${ecoles.size - 8} autres écoles →", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Quick Actions ────────────────────────────────────────────────────────────

@Composable
fun GroupeQuickActionsRow(
    onNavigateEcoles: () -> Unit,
    onNavigateUtilisateurs: () -> Unit,
    onNavigateProfils: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        GroupeQuickActionButton(Modifier.weight(1f), Icons.Default.Add, "Nouvelle école", Color(0xFF3B82F6), onNavigateEcoles)
        GroupeQuickActionButton(Modifier.weight(1f), Icons.Default.PersonAdd, "Gérer utilisateurs", Color(0xFF7C3AED), onNavigateUtilisateurs)
        GroupeQuickActionButton(Modifier.weight(1f), Icons.Default.Tune, "Gérer profils", Color(0xFFEC4899), onNavigateProfils)
    }
}

@Composable
private fun GroupeQuickActionButton(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.cursorHand(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp)
    }
}
