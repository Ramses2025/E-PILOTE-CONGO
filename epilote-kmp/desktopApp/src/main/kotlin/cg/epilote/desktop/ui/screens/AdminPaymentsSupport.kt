package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.PaymentReceiptDto
import cg.epilote.desktop.ui.screens.superadmin.KpiCard
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.screens.superadmin.formatXAF
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import java.awt.Desktop
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun PaymentRecordButton(
    subscriptions: List<SubscriptionDto>,
    unpaidInvoices: List<InvoiceDto>,
    onRecordPayment: (SubscriptionDto, InvoiceDto?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(containerColor = EpiloteGreen),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
            Text("  Enregistrer un paiement", fontSize = 13.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (unpaidInvoices.isEmpty() && subscriptions.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Aucune facture ni abonnement disponible", color = EpiloteTextMuted) },
                    onClick = { expanded = false }
                )
            } else {
                if (unpaidInvoices.isNotEmpty()) {
                    Text(
                        "Régler une facture impayée",
                        fontSize = 11.sp,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    unpaidInvoices.take(15).forEach { inv ->
                        val sub = subscriptions.firstOrNull { it.id == inv.subscriptionId }
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("${inv.groupeNom} — #${inv.reference.take(10)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${formatXAF(inv.montantXAF)} \u2022 ${invoiceStatusLabel(inv.statut)} \u2022 \u00e9ch. ${formatDate(inv.dateEcheance)}",
                                        fontSize = 11.sp,
                                        color = if (inv.statut == "overdue") Color(0xFFDC2626) else EpiloteTextMuted
                                    )
                                }
                            },
                            onClick = {
                                expanded = false
                                val target = sub ?: SubscriptionDto(
                                    id = inv.subscriptionId,
                                    groupeId = inv.groupeId,
                                    groupeNom = inv.groupeNom,
                                    groupeSlug = inv.groupeSlug,
                                    planId = inv.planId,
                                    planNom = inv.planNom,
                                    prixXAF = inv.montantXAF,
                                    statut = "active",
                                    dateDebut = 0L,
                                    dateFin = 0L,
                                    renouvellementAuto = false,
                                    createdAt = 0L,
                                    maxStudents = 0,
                                    maxPersonnel = 0,
                                    moduleCount = 0
                                )
                                onRecordPayment(target, inv)
                            }
                        )
                    }
                }
                val groupsWithUnpaid = unpaidInvoices.map { it.groupeId }.toSet()
                val now = System.currentTimeMillis()
                val directSubs = subscriptions.filter { sub ->
                    sub.groupeId !in groupsWithUnpaid &&
                        sub.prixXAF > 0L &&
                        sub.statut !in listOf("cancelled", "suspended") &&
                        (sub.statut == "pending" || (sub.dateFin in 1..now))
                }
                if (directSubs.isNotEmpty()) {
                    Text(
                        "Paiement direct (sans facture pr\u00e9alable)",
                        fontSize = 11.sp,
                        color = EpiloteTextMuted,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    directSubs.forEach { sub ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(sub.groupeNom, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${sub.planNom} \u2022 ${subscriptionStatusLabel(sub.statut)}", fontSize = 11.sp, color = EpiloteTextMuted)
                                }
                            },
                            onClick = { expanded = false; onRecordPayment(sub, null) }
                        )
                    }
                }
            }
        }
    }
}

internal fun formatMoneyXafShort(amount: Long): String = when {
    amount >= 1_000_000L -> "${amount / 1_000_000L}M FCFA"
    amount >= 1_000L -> "${amount / 1_000L}K"
    else -> "$amount"
}

// ── KPI Row ──────────────────────────────────────────────────────────────────

@Composable
internal fun PaymentKpiRow(
    totalReceipts: Int,
    totalEncaisse: Long,
    uniqueGroups: Int,
    avgAmount: Long,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 900.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Payments,
                        iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF2563EB),
                        label = "Paiements reçus", value = "$totalReceipts",
                        trendLabel = "$uniqueGroups groupe(s)"
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AccountBalance,
                        iconBg = Color(0xFFD1FAE5), iconTint = Color(0xFF059669),
                        label = "Total encaissé", value = formatMoneyXaf(totalEncaisse),
                        trendLabel = "tous modes confondus"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Groups,
                        iconBg = Color(0xFFEDE9FE), iconTint = Color(0xFF6D28D9),
                        label = "Groupes payeurs", value = "$uniqueGroups",
                        trendLabel = "groupes uniques"
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TrendingUp,
                        iconBg = Color(0xFFFEF3C7), iconTint = Color(0xFFD97706),
                        label = "Montant moyen", value = formatMoneyXaf(avgAmount),
                        trendLabel = "par paiement"
                    )
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Payments,
                    iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF2563EB),
                    label = "Paiements reçus", value = "$totalReceipts",
                    trendLabel = "$uniqueGroups groupe(s)"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AccountBalance,
                    iconBg = Color(0xFFD1FAE5), iconTint = Color(0xFF059669),
                    label = "Total encaissé", value = formatMoneyXaf(totalEncaisse),
                    trendLabel = "tous modes confondus"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Groups,
                    iconBg = Color(0xFFEDE9FE), iconTint = Color(0xFF6D28D9),
                    label = "Groupes payeurs", value = "$uniqueGroups",
                    trendLabel = "groupes uniques"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.TrendingUp,
                    iconBg = Color(0xFFFEF3C7), iconTint = Color(0xFFD97706),
                    label = "Montant moyen", value = formatMoneyXaf(avgAmount),
                    trendLabel = "par paiement"
                )
            }
        }
    }
}

// ── Monthly Bar Chart ─────────────────────────────────────────────────────────

@Composable
internal fun PaymentMonthlyChart(
    receipts: List<PaymentReceiptDto>,
    modifier: Modifier = Modifier
) {
    val zone = ZoneId.systemDefault()
    val now = LocalDate.now(zone)
    val monthFmt = DateTimeFormatter.ofPattern("MMM yy", Locale.FRENCH)
    val months = (5 downTo 0).map { now.minusMonths(it.toLong()) }

    val buckets = months.map { month ->
        val label = month.format(monthFmt).replaceFirstChar { it.uppercase() }
        val total = receipts.filter { r ->
            val d = Instant.ofEpochMilli(r.receivedAt).atZone(zone).toLocalDate()
            d.year == month.year && d.monthValue == month.monthValue
        }.sumOf { it.montantXAF }
        label to total
    }
    val maxVal = buckets.maxOfOrNull { it.second }.takeIf { it != null && it > 0L } ?: 1L

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Encaissements — 6 derniers mois",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF1D3557)
                    )
                    Text(
                        "${receipts.size} paiement(s) sur la période",
                        fontSize = 11.sp,
                        color = EpiloteTextMuted
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFD1FAE5)
                ) {
                    Text(
                        formatMoneyXaf(receipts.sumOf { it.montantXAF }),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF059669)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                buckets.forEach { (label, total) ->
                    val barFraction = (total.toFloat() / maxVal).coerceIn(0f, 1f)
                    val barHeightDp = (barFraction * 80f).dp.coerceAtLeast(if (total > 0) 6.dp else 2.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (total > 0) {
                            Text(
                                formatMoneyXafShort(total),
                                fontSize = 8.sp,
                                color = Color(0xFF059669),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(barHeightDp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (total > 0) Color(0xFF059669) else Color(0xFFE2E8F0))
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            label,
                            fontSize = 9.sp,
                            color = EpiloteTextMuted,
                            maxLines = 1,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

