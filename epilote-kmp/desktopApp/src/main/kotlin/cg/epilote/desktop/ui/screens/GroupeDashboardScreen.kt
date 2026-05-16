package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cg.epilote.desktop.data.*
import cg.epilote.shared.domain.model.UserSession
import kotlinx.coroutines.launch

// ── Screen principal (orchestrateur v2) ──────────────────────────────────────

@Composable
fun GroupeDashboardScreen(
    session: UserSession,
    groupeRepo: GroupeAdminDataRepository,
    onNavigateEcoles: () -> Unit = {},
    onNavigateUtilisateurs: () -> Unit = {},
    onNavigateProfils: () -> Unit = {}
) {
    val stats by groupeRepo.dashboardStats.collectAsState()
    val isLoading by groupeRepo.isLoading.collectAsState()
    val isOffline by groupeRepo.isOffline.collectAsState()
    val categoriesWithModules by groupeRepo.categoriesWithModules.collectAsState()
    val invoiceTimeline by groupeRepo.invoiceTimeline.collectAsState()
    val activityTimeline by groupeRepo.activityTimeline.collectAsState()
    val notifications by groupeRepo.notifications.collectAsState()
    val moduleKpis by groupeRepo.moduleKpis.collectAsState()
    val scope = rememberCoroutineScope()
    var showPlanDetail by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8))
            .verticalScroll(scrollState)
            .padding(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── 1. BIENVENUE ──
        GroupeWelcomeSection(
            session   = session,
            groupeNom = stats?.groupeNom ?: "",
            isLoading = isLoading,
            onRefresh = { scope.launch { groupeRepo.refreshAll() } }
        )

        // ── 2. BANNIÈRE OFFLINE ──
        if (isOffline) {
            GroupeOfflineBanner()
        }

        // ── 3. ALERTE FACTURES EN RETARD ──
        stats?.let { s ->
            GroupeOverdueAlert(nbFacturesEnRetard = s.nbFacturesEnRetard)
        }

        // ── 4. PLAN BANNER CARD ──
        if (stats != null) {
            GroupePlanBannerCard(
                stats   = stats!!,
                onClick = { showPlanDetail = true }
            )
        } else if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp)
                    .background(Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }

        // ── 5. KPI ROW — ORGANISATION ──
        stats?.let { s ->
            GroupeOrgKpiRow(
                stats            = s,
                onNavigateEcoles = onNavigateEcoles,
                onNavigateUsers  = onNavigateUtilisateurs
            )

            // ── 6. GRAPHIQUES — Indicateurs + Province ──
            GroupeChartsRow1(stats = s, categoriesWithModules = categoriesWithModules)

            // ── 8. GRAPHIQUES — Facturation + Modules par catégorie ──
            GroupeChartsRow2(stats = s, categoriesWithModules = categoriesWithModules)

            // ── 9. ÉVOLUTION FACTURATION (LineChart) ──
            GroupeInvoiceTimelineChart(invoiceTimeline = invoiceTimeline)

            // ── 9B. ACTIVITÉ DU GROUPE + NOTIFICATIONS ──
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(2f)) {
                    GroupeActivitySection(timeline = activityTimeline)
                }
                Box(modifier = Modifier.weight(1f)) {
                    GroupeNotificationsPanel(notifications = notifications)
                }
            }

            // ── 10. KPI DYNAMIQUES PAR MODULE ──
            GroupeDynamicModuleKpis(
                categoriesWithModules = categoriesWithModules,
                stats = s,
                moduleKpis = moduleKpis
            )

            // ── 11. LISTE DES ÉCOLES ──
            GroupeEcolesSection(
                ecoles           = s.ecoles,
                onNavigateEcoles = onNavigateEcoles
            )
        } ?: run {
            if (!isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Données du dashboard indisponibles", style = MaterialTheme.typography.titleMedium, color = Color(0xFF0F172A))
                        Text("Nous n'avons pas pu charger les indicateurs du groupe pour le moment.", color = Color(0xFF64748B))
                        Button(onClick = { scope.launch { groupeRepo.refreshAll() } }, shape = RoundedCornerShape(10.dp)) {
                            Text("Réessayer")
                        }
                    }
                }
            }
        }

        // ── 12. ACTIONS RAPIDES ──
        GroupeQuickActionsRow(
            onNavigateEcoles       = onNavigateEcoles,
            onNavigateUtilisateurs = onNavigateUtilisateurs,
            onNavigateProfils      = onNavigateProfils
        )
    }

    // ── PLAN DETAIL DIALOG ──
    if (showPlanDetail && stats != null) {
        GroupePlanDetailDialog(
            stats      = stats!!,
            groupeRepo = groupeRepo,
            isOffline  = isOffline,
            onDismiss  = { showPlanDetail = false }
        )
    }
}

