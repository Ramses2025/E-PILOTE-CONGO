package cg.epilote.desktop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.screens.superadmin.*
import cg.epilote.shared.domain.model.UserSession

@Composable
fun SuperAdminDashboardScreen(
    session: UserSession,
    stats: AdminStats,
    isLoading: Boolean,
    onNavigateGroupes: () -> Unit,
    onNavigatePlans: () -> Unit,
    onNavigateModules: () -> Unit,
    onNavigateInvoices: () -> Unit,
    onNavigateNotifications: () -> Unit,
    onNavigateAnnouncements: () -> Unit,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Force un rafraîchissement dès l'affichage du tableau de bord pour éviter
        // l'affichage transitoire des KPIs par défaut (0 partout) tant que le refresh
        // global n'a pas encore répondu. Cf. AdminDataRepository.refreshAll().
        visible = true
        onRefresh()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4F8))
    ) {
        // ═══════════ CONTENU SCROLLABLE ═══════════
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── 1. BIENVENUE ─────────────────────────────────────
            WelcomeSection(session = session, isLoading = isLoading, onRefresh = onRefresh)

            // ── 2. KPIs PRINCIPAUX (Groupes, Abonnements, Revenus, Croissance)
            AnimatedSection(visible, 0) {
                DashboardMainKpiRow(stats = stats, onNavigateGroupes = onNavigateGroupes)
            }

            // ── 3. VUE D'ENSEMBLE NATIONALE (mini-cartes Élèves, Enseignants, Écoles, Inscriptions)
            AnimatedSection(visible, 100) {
                NationalOverviewSection(stats = stats)
            }

            // ── 4. GRAPHIQUES : Couverture par Département | Couverture nationale (anneaux)
            AnimatedSection(visible, 200) {
                DashboardChartsRow1(stats = stats)
            }

            // ── 5. CARTE DES DÉPARTEMENTS DU CONGO
            AnimatedSection(visible, 300) {
                DepartmentMapSection(stats = stats)
            }

            // ── 6. GRAPHIQUES : Performance financière | Répartition des plans
            AnimatedSection(visible, 400) {
                DashboardChartsRow2(stats = stats)
            }

            // ── 7. KPIs SECONDAIRES (Utilisateurs, Factures, Modules, Plans)
            AnimatedSection(visible, 500) {
                DashboardSecondaryKpiRow(
                    stats = stats,
                    onNavigateModules = onNavigateModules,
                    onNavigatePlans = onNavigatePlans
                )
            }

            // ── 8. GRAPHIQUE : Statut des abonnements
            AnimatedSection(visible, 600) {
                DashboardChartsRow3(stats = stats)
            }

            // ── 9. ALERTES (factures en retard)
            if (stats.invoicesOverdue > 0) {
                AlertsSection(stats.invoicesOverdue, onNavigateNotifications)
            }

            // ── 10. ACTIVITÉ RÉCENTE
            AnimatedSection(visible, 700) {
                RecentActivityRow(stats = stats, onNavigateGroupes = onNavigateGroupes, onNavigateInvoices = onNavigateInvoices)
            }

            // ── 11. ACTIONS RAPIDES
            QuickActionsRow(
                onNavigateGroupes = onNavigateGroupes,
                onNavigatePlans = onNavigatePlans,
                onNavigateModules = onNavigateModules,
                onNavigateAnnouncements = onNavigateAnnouncements
            )

            // ── 12. FOOTER
            SystemFooter()
        }
    }
}

// ── Section Bienvenue (design screenshot) ───────────────────────────────────

@Composable
private fun WelcomeSection(
    session: UserSession,
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
                Text(
                    "E-Pilot Congo \uD83C\uDDE8\uD83C\uDDEC",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF059669)
                )
                Text("•", fontSize = 13.sp, color = Color(0xFF94A3B8))
                Text(
                    formatDateLong(),
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
        OutlinedButton(
            onClick = onRefresh,
            shape = RoundedCornerShape(10.dp)
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

// ── Utilitaire d'animation séquentielle ─────────────────────────────────────

@Composable
private fun AnimatedSection(
    visible: Boolean,
    delayMs: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500, delayMs)) + slideInVertically(tween(500, delayMs)) { 40 }
    ) {
        content()
    }
}
