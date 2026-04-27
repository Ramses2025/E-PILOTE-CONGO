package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.AuditEventApiDto
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted

/**
 * Journal d'audit Super Admin — version branchée sur le **vrai** backend
 * (`GET /api/super-admin/audit-logs`) avec pagination, filtres et recherche.
 *
 * Les paramètres `groupes` et `admins` sont conservés pour compat avec les
 * appelants existants mais ne sont PAS utilisés : la source de vérité est
 * désormais la collection cloud-only `audit_logs` (CADF-friendly). Voir
 * `AdminAuditLogRepository.list(...)` côté backend.
 */
@Composable
internal fun AdminAuditScreen(
    @Suppress("UNUSED_PARAMETER") groupes: List<GroupeDto>,
    @Suppress("UNUSED_PARAMETER") admins: List<AdminUserDto>,
    isLoading: Boolean,
    client: DesktopAdminClient,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    var selectedOutcome by remember { mutableStateOf("all") }
    var page by remember { mutableIntStateOf(1) }
    val pageSize = 50

    var pageData by remember { mutableStateOf<List<AuditEventApiDto>>(emptyList()) }
    var total by remember { mutableStateOf(0L) }
    var pageLoading by remember { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var selectedEntry by remember { mutableStateOf<AuditEventApiDto?>(null) }
    var reloadTick by remember { mutableIntStateOf(0) }

    suspend fun refreshAudit() {
        pageLoading = true
        pageError = null
        val resp = runCatching {
            client.listAuditLogs(
                page = page,
                pageSize = pageSize,
                category = selectedCategory.takeIf { it != "all" },
                outcome = selectedOutcome.takeIf { it != "all" },
                search = searchQuery.takeIf { it.isNotBlank() }
            )
        }.getOrNull()
        if (resp == null) {
            pageError = "Impossible de charger le journal d'audit (backend indisponible ou non autorisé)."
            pageData = emptyList()
            total = 0
        } else {
            pageData = resp.items
            total = resp.total
        }
        pageLoading = false
    }

    LaunchedEffect(page, selectedCategory, selectedOutcome, searchQuery, reloadTick) {
        refreshAudit()
    }

    AdminAutoRefreshEffect(refreshKey = reloadTick) {
        refreshAudit()
    }

    val authCount = pageData.count { it.category.equals("auth", true) }
    val failureCount = pageData.count { it.outcome.equals("failure", true) }
    val financeCount = pageData.count { it.category.equals("invoice", true) || it.category.equals("payment", true) }
    val totalPages = if (total <= 0) 1 else ((total + pageSize - 1) / pageSize).toInt().coerceAtLeast(1)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Journal d'audit", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "Trace serveur immuable de chaque action mutante (auth, groupes, admins, abonnements, factures, paiements, plateforme, scheduler).",
                fontSize = 13.sp,
                color = EpiloteTextMuted
            )
            Text(
                "Source : collection cloud-only `audit_logs` (jamais syncée vers les bases mobiles — RGPD/CADF).",
                fontSize = 11.sp,
                color = EpiloteTextMuted
            )
        }

        pageError?.let {
            AdminFeedbackBanner(feedback = AdminFeedbackMessage(it, isError = true), onDismiss = { pageError = null })
        }

        AuditServerKpiRow(
            totalEntries = total,
            shownOnPage = pageData.size,
            authCount = authCount,
            failureCount = failureCount,
            financeCount = financeCount
        )

        AuditServerToolbar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it; page = 1 },
            selectedCategory = selectedCategory,
            onCategoryChange = { selectedCategory = it; page = 1 },
            selectedOutcome = selectedOutcome,
            onOutcomeChange = { selectedOutcome = it; page = 1 },
            totalResults = total,
            onRefresh = {
                onRefresh()
                reloadTick++
            }
        )

        if (isLoading || pageLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        } else if (pageData.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.History, null, tint = EpiloteTextMuted)
                        Text("Aucune entrée d'audit", fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
                        Text(
                            "Aucun événement ne correspond aux filtres actuellement appliqués.",
                            fontSize = 12.sp,
                            color = EpiloteTextMuted
                        )
                    }
                }
            }
        } else {
            AuditServerEntriesPanel(entries = pageData, onOpen = { selectedEntry = it })

            // Pagination
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { if (page > 1) page-- },
                    enabled = page > 1
                ) { Text("← Précédent") }
                Text("Page $page / $totalPages", fontSize = 13.sp, color = EpiloteTextMuted)
                Button(
                    onClick = { if (page < totalPages) page++ },
                    enabled = page < totalPages,
                    colors = ButtonDefaults.buttonColors(containerColor = EpiloteGreen)
                ) { Text("Suivant →") }
            }
        }
    }

    selectedEntry?.let { entry ->
        AuditServerEntryDetailDialog(entry = entry, onDismiss = { selectedEntry = null })
    }
}

/** Mini-bandeau indiquant qu'aucun audit n'est encore disponible. */
@Composable
private fun EmptyHint() {
    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.Transparent))
}
