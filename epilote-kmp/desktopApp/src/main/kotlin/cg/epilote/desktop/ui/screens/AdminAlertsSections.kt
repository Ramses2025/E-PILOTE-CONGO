package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.InvoiceApiDto
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.screens.superadmin.formatXAF
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand

@Composable
internal fun AdminAlertsSectionsList(
    criticalSubs: List<SubscriptionDto>,
    warningSubs: List<SubscriptionDto>,
    expiredSubs: List<SubscriptionDto>,
    overdueInvoices: List<InvoiceApiDto>,
    groupsNoActiveSub: List<GroupeDto>,
    lowRecovery: Boolean,
    recoveryRate: Int,
    paidRevenue: Long,
    totalRevenue: Long,
    now: Long,
    groupes: List<GroupeDto>,
    totalAlerts: Int,
    isLoading: Boolean,
    onNavigatePayments: () -> Unit,
    onNavigateSubscriptions: () -> Unit,
    onNavigateInvoices: () -> Unit,
    onRecordPaymentTarget: (SubscriptionDto) -> Unit
) {
    if (totalAlerts == 0 && !isLoading) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, null, tint = EpiloteGreen, modifier = Modifier.size(36.dp))
                    Text("Aucune alerte active", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Tous les indicateurs sont au vert.", fontSize = 13.sp, color = EpiloteTextMuted)
                }
            }
        }
    }

    if (criticalSubs.isNotEmpty()) {
        AlertSection(icon = Icons.Default.ErrorOutline, title = "CRITIQUE — Abonnements expirant dans 7 jours",
            count = criticalSubs.size, accentColor = Color(0xFFDC2626),
            actionLabel = "Enregistrer un paiement", onAction = onNavigatePayments) {
            criticalSubs.forEach { sub ->
                val daysLeft = ((sub.dateFin - now) / 86_400_000L).toInt().coerceAtLeast(0)
                AlertItemRow(title = sub.groupeNom,
                    subtitle = "${sub.planNom}${if (sub.prixXAF > 0) " • ${formatMoneyXaf(sub.prixXAF)}" else ""} • Fin : ${formatDate(sub.dateFin)}",
                    badge = if (daysLeft == 0) "Aujourd'hui" else "J-$daysLeft",
                    badgeColor = Color(0xFFDC2626), actionLabel = "Payer",
                    onAction = { onRecordPaymentTarget(sub) })
            }
        }
    }

    if (warningSubs.isNotEmpty()) {
        AlertSection(icon = Icons.Default.AccessTime, title = "À surveiller — Expirent dans 30 jours",
            count = warningSubs.size, accentColor = Color(0xFFD97706),
            actionLabel = "Gérer les abonnements", onAction = onNavigateSubscriptions) {
            warningSubs.forEach { sub ->
                val daysLeft = ((sub.dateFin - now) / 86_400_000L).toInt()
                AlertItemRow(title = sub.groupeNom,
                    subtitle = "${sub.planNom}${if (sub.prixXAF > 0) " • ${formatMoneyXaf(sub.prixXAF)}" else ""} • Fin : ${formatDate(sub.dateFin)}",
                    badge = "J-$daysLeft", badgeColor = Color(0xFFD97706), actionLabel = "Payer",
                    onAction = { onRecordPaymentTarget(sub) })
            }
        }
    }

    if (expiredSubs.isNotEmpty()) {
        AlertSection(icon = Icons.Default.Warning, title = "Abonnements expirés / suspendus",
            count = expiredSubs.size, accentColor = Color(0xFF7C3AED),
            actionLabel = "Réactiver via paiement", onAction = onNavigatePayments) {
            expiredSubs.take(10).forEach { sub ->
                AlertItemRow(title = sub.groupeNom,
                    subtitle = "${sub.planNom}${if (sub.prixXAF > 0) " • ${formatMoneyXaf(sub.prixXAF)}" else ""} • Expiré le ${formatDate(sub.dateFin)}",
                    badge = subscriptionStatusLabel(sub.statut), badgeColor = Color(0xFF7C3AED),
                    actionLabel = "Réactiver", onAction = { onRecordPaymentTarget(sub) })
            }
            if (expiredSubs.size > 10) {
                Text("+ ${expiredSubs.size - 10} abonnement(s) supplémentaire(s)",
                    fontSize = 11.sp, color = EpiloteTextMuted,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
    }

    if (overdueInvoices.isNotEmpty()) {
        AlertSection(icon = Icons.Default.Receipt, title = "Factures en retard de paiement",
            count = overdueInvoices.size, accentColor = Color(0xFFDC2626),
            actionLabel = "Voir les factures", onAction = onNavigateInvoices) {
            overdueInvoices.take(10).forEach { inv ->
                val groupe = groupes.firstOrNull { it.id == inv.groupeId }
                AlertItemRow(title = groupe?.nom ?: inv.groupeId,
                    subtitle = "Éch. ${formatDate(inv.dateEcheance)} • Réf. ${inv.reference.ifBlank { inv.id.take(8) }}",
                    badge = formatXAF(inv.montantXAF), badgeColor = Color(0xFFDC2626))
            }
            if (overdueInvoices.size > 10) {
                Text("+ ${overdueInvoices.size - 10} facture(s) supplémentaire(s)",
                    fontSize = 11.sp, color = EpiloteTextMuted,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
    }

    if (groupsNoActiveSub.isNotEmpty()) {
        AlertSection(icon = Icons.Default.Business, title = "Groupes sans abonnement actif",
            count = groupsNoActiveSub.size, accentColor = Color(0xFF7C3AED),
            actionLabel = "Gérer les abonnements", onAction = onNavigateSubscriptions) {
            groupsNoActiveSub.take(10).forEach { g ->
                AlertItemRow(title = g.nom,
                    subtitle = "${g.department ?: "Département non renseigné"} • ${g.ecolesCount} école(s)",
                    badge = "Aucun abonnement", badgeColor = Color(0xFF7C3AED))
            }
            if (groupsNoActiveSub.size > 10) {
                Text("+ ${groupsNoActiveSub.size - 10} groupe(s) supplémentaire(s)",
                    fontSize = 11.sp, color = EpiloteTextMuted,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
    }

    if (lowRecovery) {
        AlertSection(icon = Icons.Default.AccountBalance, title = "Taux de recouvrement insuffisant",
            count = 1, accentColor = Color(0xFFDC2626),
            actionLabel = "Voir les factures", onAction = onNavigateInvoices) {
            AlertItemRow(title = "Taux actuel : $recoveryRate%",
                subtitle = "Seuil recommandé : 70% — ${formatXAF(paidRevenue)} encaissés sur ${formatXAF(totalRevenue)} facturés",
                badge = "Critique", badgeColor = Color(0xFFDC2626))
        }
    }
}

@Composable
internal fun AlertSection(
    icon: ImageVector,
    title: String,
    count: Int,
    accentColor: Color,
    actionLabel: String,
    onAction: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(36.dp).background(accentColor.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("$count élément(s)", fontSize = 11.sp, color = EpiloteTextMuted)
                    }
                }
                TextButton(onClick = onAction, colors = ButtonDefaults.textButtonColors(contentColor = accentColor)) {
                    Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
        }
    }
}

@Composable
internal fun AlertItemRow(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
            Text(subtitle, fontSize = 11.sp, color = EpiloteTextMuted)
        }
        Spacer(Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(999.dp), color = badgeColor.copy(alpha = 0.10f)) {
                Text(badge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.sp, color = badgeColor, fontWeight = FontWeight.SemiBold)
            }
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction, modifier = Modifier.height(28.dp).cursorHand(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = badgeColor, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)) {
                    Icon(Icons.Default.Payments, null, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(actionLabel, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
