package cg.epilote.desktop.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.data.InvoiceApiDto
import cg.epilote.desktop.data.SubscriptionApiDto
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.screens.superadmin.formatXAF
import kotlinx.coroutines.CoroutineScope

@Composable
fun AdminAlertsScreen(
    groupes: List<GroupeDto>,
    rawSubscriptions: List<SubscriptionApiDto>,
    rawInvoices: List<InvoiceApiDto>,
    plans: List<PlanDto>,
    isLoading: Boolean,
    scope: CoroutineScope,
    client: DesktopAdminClient,
    onRefresh: () -> Unit,
    onNavigateSubscriptions: () -> Unit,
    onNavigateInvoices: () -> Unit,
    onNavigatePayments: () -> Unit
) {
    val now = System.currentTimeMillis()
    val in7Days = now + 7L * 86_400_000L
    val in30Days = now + 30L * 86_400_000L

    var recordPaymentTarget by remember { mutableStateOf<SubscriptionDto?>(null) }
    var alertFeedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }

    val allSubscriptions = remember(rawSubscriptions, groupes, plans) {
        rawSubscriptions.mapNotNull { api ->
            val group = groupes.firstOrNull { it.id == api.groupeId } ?: return@mapNotNull null
            val plan = plans.firstOrNull { it.id == api.planId }
            SubscriptionDto(
                id = api.id.ifBlank { "sub::${api.groupeId}" },
                groupeId = api.groupeId,
                groupeNom = group.nom,
                groupeSlug = group.slug,
                planId = api.planId,
                planNom = plan?.nom ?: api.planId,
                prixXAF = plan?.prixXAF ?: 0L,
                statut = api.statut,
                dateDebut = api.dateDebut,
                dateFin = api.dateFin,
                renouvellementAuto = api.renouvellementAuto,
                createdAt = api.createdAt,
                maxStudents = plan?.maxStudents ?: 0,
                maxPersonnel = plan?.maxPersonnel ?: 0,
                moduleCount = plan?.modulesIncluded?.size ?: 0
            )
        }
    }

    val criticalSubs = remember(allSubscriptions, now, in7Days) {
        allSubscriptions.filter { it.statut == "active" && it.dateFin in now..in7Days }.sortedBy { it.dateFin }
    }
    val warningSubs = remember(allSubscriptions, in7Days, in30Days) {
        allSubscriptions.filter { it.statut == "active" && it.dateFin in (in7Days + 1L)..in30Days }.sortedBy { it.dateFin }
    }
    val expiredSubs = remember(allSubscriptions, now) {
        allSubscriptions.filter { it.statut in listOf("suspended", "expired", "inactive") || (it.statut == "active" && it.dateFin < now) }
            .sortedByDescending { it.dateFin }
    }
    val overdueInvoices = remember(rawInvoices) {
        rawInvoices.filter { it.statut == "overdue" }.sortedByDescending { it.dateEcheance }
    }
    val groupsNoActiveSub = remember(groupes, rawSubscriptions) {
        groupes.filter { g -> rawSubscriptions.none { it.groupeId == g.id && it.statut == "active" } }
            .sortedBy { it.nom.lowercase() }
    }

    val totalRevenue = remember(rawInvoices) { rawInvoices.sumOf { it.montantXAF } }
    val paidRevenue = remember(rawInvoices) { rawInvoices.filter { it.statut == "paid" }.sumOf { it.montantXAF } }
    val recoveryRate = if (totalRevenue > 0) (paidRevenue * 100 / totalRevenue).toInt() else 100
    val lowRecovery = recoveryRate < 70

    val expiringCount = criticalSubs.size + warningSubs.size
    val totalAlerts = expiringCount + expiredSubs.size + overdueInvoices.size +
        groupsNoActiveSub.size + (if (lowRecovery) 1 else 0)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Alertes & Seuils", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    if (totalAlerts > 0) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFFEE2E2)
                        ) {
                            Text(
                                "$totalAlerts",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }
                }
                Text(
                    "Surveillance automatique des abonnements, factures et groupes. Mise à jour en temps réel.",
                    fontSize = 13.sp,
                    color = EpiloteTextMuted
                )
            }
            IconButton(onClick = onRefresh) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = EpiloteGreen)
                } else {
                    Icon(Icons.Default.Refresh, "Actualiser", tint = EpiloteTextMuted)
                }
            }
        }

        alertFeedback?.let { fb ->
            AdminFeedbackBanner(feedback = fb, onDismiss = { alertFeedback = null })
        }

        // ── KPI summary ──
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AlertKpiCard(
                icon = if (criticalSubs.isNotEmpty()) Icons.Default.ErrorOutline else Icons.Default.AccessTime,
                label = if (criticalSubs.isNotEmpty()) "Critique (≤ 7j)" else "Expirent dans 30j",
                value = expiringCount.toString(),
                color = if (criticalSubs.isNotEmpty()) Color(0xFFDC2626) else Color(0xFFD97706),
                modifier = Modifier.weight(1f)
            )
            AlertKpiCard(
                icon = Icons.Default.Receipt,
                label = "Factures en retard",
                value = overdueInvoices.size.toString(),
                color = Color(0xFFDC2626),
                modifier = Modifier.weight(1f)
            )
            AlertKpiCard(
                icon = Icons.Default.Business,
                label = "Sans abonnement",
                value = groupsNoActiveSub.size.toString(),
                color = Color(0xFF7C3AED),
                modifier = Modifier.weight(1f)
            )
            AlertKpiCardWithProgress(
                label = "Taux de recouvrement",
                value = "$recoveryRate%",
                progress = recoveryRate / 100f,
                color = when {
                    recoveryRate >= 90 -> EpiloteGreen
                    recoveryRate >= 70 -> Color(0xFFD97706)
                    else -> Color(0xFFDC2626)
                },
                modifier = Modifier.weight(1f)
            )
        }

        // ── Dashboard Charts (always visible) ──
        val activeCount = remember(allSubscriptions) { allSubscriptions.count { it.statut == "active" } }
        val suspendedCount = remember(allSubscriptions) { allSubscriptions.count { it.statut in listOf("suspended", "expired", "inactive") } }

        val paidInvoices = remember(rawInvoices) { rawInvoices.count { it.statut == "paid" } }
        val pendingInvoices = remember(rawInvoices) { rawInvoices.count { it.statut in listOf("pending", "draft", "sent") } }
        val overdueCount = remember(rawInvoices) { rawInvoices.count { it.statut == "overdue" } }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AlertDistributionCard(
                title = "Santé des abonnements",
                modifier = Modifier.weight(1f),
                segments = listOf(
                    SegmentData("Actifs", activeCount, Color(0xFF059669)),
                    SegmentData("Expirant ≤ 30j", expiringCount, Color(0xFFD97706)),
                    SegmentData("Expirés / Suspendus", suspendedCount, Color(0xFFDC2626))
                )
            )
            AlertDistributionCard(
                title = "Répartition des factures",
                modifier = Modifier.weight(1f),
                segments = listOf(
                    SegmentData("Payées", paidInvoices, Color(0xFF059669)),
                    SegmentData("En attente", pendingInvoices, Color(0xFF2563EB)),
                    SegmentData("En retard", overdueCount, Color(0xFFDC2626))
                )
            )
        }


        AdminAlertsSectionsList(
            criticalSubs            = criticalSubs,
            warningSubs             = warningSubs,
            expiredSubs             = expiredSubs,
            overdueInvoices         = overdueInvoices,
            groupsNoActiveSub       = groupsNoActiveSub,
            lowRecovery             = lowRecovery,
            recoveryRate            = recoveryRate,
            paidRevenue             = paidRevenue,
            totalRevenue            = totalRevenue,
            now                     = now,
            groupes                 = groupes,
            totalAlerts             = totalAlerts,
            isLoading               = isLoading,
            onNavigatePayments      = onNavigatePayments,
            onNavigateSubscriptions = onNavigateSubscriptions,
            onNavigateInvoices      = onNavigateInvoices,
            onRecordPaymentTarget   = { recordPaymentTarget = it }
        )
    }

    recordPaymentTarget?.let { target ->
        RecordPaymentDialog(
            subscription = target,
            client = client,
            scope = scope,
            onDismiss = { recordPaymentTarget = null },
            onSuccess = { receipt ->
                recordPaymentTarget = null
                alertFeedback = AdminFeedbackMessage(
                    "Paiement de ${formatMoneyXaf(receipt.montantXAF)} enregistré pour ${target.groupeNom}"
                )
                onRefresh()
            }
        )
    }
}

