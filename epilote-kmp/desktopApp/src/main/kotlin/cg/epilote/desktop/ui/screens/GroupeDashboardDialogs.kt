package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.*
import cg.epilote.desktop.ui.screens.superadmin.*
import cg.epilote.desktop.ui.theme.cursorHand
import kotlinx.coroutines.launch

// ── Plan Detail Dialog ────────────────────────────────────────────────────────

@Composable
fun GroupePlanDetailDialog(
    stats: GroupeDashboardStatsDto,
    groupeRepo: GroupeAdminDataRepository,
    onDismiss: () -> Unit
) {
    var requestType by remember { mutableStateOf<String?>(null) }

    AdminDialogWindow(
        title     = "Abonnement & Facturation",
        subtitle  = "${stats.groupeNom} — ${stats.planNom}",
        onDismiss = onDismiss,
        size      = DpSize(680.dp, 560.dp),
        content   = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                GroupePlanDetailSection(stats = stats)
                if (stats.derniereFacture != null) {
                    GroupeLastInvoiceSection(invoice = stats.derniereFacture)
                }
                GroupeBillingRecapSection(stats = stats)
            }
        },
        actions = {
            TextButton(onClick = onDismiss, modifier = Modifier.cursorHand()) {
                Text("Fermer")
            }
            Spacer(Modifier.width(4.dp))
            val now = System.currentTimeMillis()
            val isExpiredOrExpiring = stats.abonnementStatut.lowercase() == "expired"
                || stats.abonnementStatut.lowercase() == "pending"
                || (stats.abonnementDateFin > 0 && (stats.abonnementDateFin - now) < 60L * 24 * 3600 * 1000)
            if (isExpiredOrExpiring) {
                OutlinedButton(
                    onClick = { requestType = "RENEWAL_REQUEST" },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand()
                ) {
                    Icon(Icons.Default.Autorenew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Demander un renouvellement", fontSize = 13.sp)
                }
            }
            Button(
                onClick = { requestType = "PLAN_CHANGE_REQUEST" },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.cursorHand(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Changer de plan", fontSize = 13.sp)
            }
        }
    )

    if (requestType != null) {
        GroupeSubscriptionRequestDialog(
            requestType = requestType!!,
            groupeRepo  = groupeRepo,
            onDismiss   = { requestType = null }
        )
    }
}

// ── Plan Detail Section ──────────────────────────────────────────────────────

@Composable
fun GroupePlanDetailSection(stats: GroupeDashboardStatsDto) {
    val now = System.currentTimeMillis()
    val isExpired = stats.abonnementStatut.lowercase() == "expired" || (stats.abonnementDateFin in 1 until now)
    val badgeColor = when {
        isExpired -> Color(0xFFEF4444)
        stats.abonnementStatut.lowercase() == "active" -> Color(0xFF059669)
        else -> Color(0xFFF59E0B)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Abonnement actuel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stats.planNom, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Surface(shape = RoundedCornerShape(20.dp), color = badgeColor) {
                        Text(
                            stats.abonnementStatut.uppercase(),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    GroupeDetailItem("Type", stats.planType.replaceFirstChar { it.uppercase() })
                    if (stats.prixXAF > 0) GroupeDetailItem("Prix", formatXAF(stats.prixXAF) + "/an")
                    if (stats.abonnementDateDebut > 0) GroupeDetailItem("Début", formatDate(stats.abonnementDateDebut))
                    if (stats.abonnementDateFin > 0) GroupeDetailItem("Fin", formatDate(stats.abonnementDateFin))
                    GroupeDetailItem("Renouvellement auto", if (stats.renouvellementAuto) "Oui" else "Non")
                }
            }
        }
    }
}

// ── Last Invoice Section ─────────────────────────────────────────────────────

@Composable
fun GroupeLastInvoiceSection(invoice: InvoiceApiDto) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Dernière facture", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    GroupeDetailItem("Référence", invoice.reference.ifBlank { invoice.id.take(12) })
                    GroupeDetailItem("Montant", formatXAF(invoice.montantXAF))
                    GroupeDetailItem("Date", formatDate(invoice.dateEmission))
                }
                val invoiceStatutColor = when (invoice.statut.lowercase()) {
                    "paid" -> Color(0xFF059669)
                    "overdue" -> Color(0xFFEF4444)
                    "cancelled" -> Color(0xFF94A3B8)
                    else -> Color(0xFFF59E0B)
                }
                Surface(shape = RoundedCornerShape(20.dp), color = invoiceStatutColor.copy(alpha = 0.12f)) {
                    Text(
                        invoice.statut.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = invoiceStatutColor
                    )
                }
            }
        }
    }
}

// ── Billing Recap Section ────────────────────────────────────────────────────

@Composable
fun GroupeBillingRecapSection(stats: GroupeDashboardStatsDto) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Récapitulatif facturation", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GroupeBillingStatCard("Total facturé", formatXAF(stats.facturationTotaleXAF), Color(0xFF0EA5E9), Modifier.weight(1f))
            GroupeBillingStatCard("Payé", formatXAF(stats.montantPayeXAF), Color(0xFF059669), Modifier.weight(1f))
            GroupeBillingStatCard("Reste à payer", formatXAF(stats.montantDuXAF), if (stats.montantDuXAF > 0) Color(0xFFEF4444) else Color(0xFF059669), Modifier.weight(1f))
            GroupeBillingStatCard("En retard", "${stats.nbFacturesEnRetard}", if (stats.nbFacturesEnRetard > 0) Color(0xFFEF4444) else Color(0xFF059669), Modifier.weight(1f))
        }
    }
}

@Composable
fun GroupeBillingStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontSize = 11.sp, color = Color(0xFF64748B))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun GroupeDetailItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
    }
}

// ── Subscription Request Dialog ──────────────────────────────────────────────

@Composable
fun GroupeSubscriptionRequestDialog(
    requestType: String,
    groupeRepo: GroupeAdminDataRepository,
    onDismiss: () -> Unit
) {
    val isRenewal = requestType == "RENEWAL_REQUEST"
    val title = if (isRenewal) "Demande de renouvellement" else "Demande de changement de plan"
    val subtitle = if (isRenewal) "Votre demande sera transmise à E-PILOTE Congo" else "Précisez le plan souhaité dans le message"

    var message by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }
    val scope = rememberCoroutineScope()

    AdminDialogWindow(
        title     = title,
        subtitle  = subtitle,
        onDismiss = { if (!isSubmitting) onDismiss() },
        size      = DpSize(520.dp, 340.dp),
        content   = {
            feedback?.let { fb ->
                AdminFeedbackBanner(feedback = fb, onDismiss = { feedback = null })
            }
            Text(
                if (isRenewal)
                    "Votre demande de renouvellement sera visible par l'équipe E-PILOTE Congo qui vous contactera rapidement."
                else
                    "Décrivez ci-dessous le plan souhaité et vos besoins. L'équipe E-PILOTE Congo vous soumettra un devis.",
                fontSize = 13.sp,
                color = Color(0xFF475569)
            )
            OutlinedTextField(
                value = message,
                onValueChange = { if (it.length <= 500) message = it },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                label = { Text("Message (optionnel)", fontSize = 12.sp) },
                placeholder = { Text("Ajoutez des précisions si nécessaire…", fontSize = 12.sp) },
                maxLines = 5,
                supportingText = { Text("${message.length}/500", fontSize = 11.sp, color = Color(0xFF94A3B8)) }
            )
        },
        actions = {
            TextButton(onClick = { if (!isSubmitting) onDismiss() }, modifier = Modifier.cursorHand()) {
                Text("Annuler")
            }
            Button(
                onClick = {
                    scope.launch {
                        isSubmitting = true
                        val ok = groupeRepo.createSubscriptionRequest(
                            SubscriptionRequestDto(type = requestType, message = message.takeIf { it.isNotBlank() })
                        )
                        isSubmitting = false
                        if (ok) {
                            feedback = AdminFeedbackMessage("Votre demande a été envoyée à E-PILOTE Congo \u2713")
                        } else {
                            feedback = AdminFeedbackMessage("Échec de l'envoi. Vérifiez votre connexion.", isError = true)
                        }
                    }
                },
                enabled  = !isSubmitting,
                shape    = RoundedCornerShape(10.dp),
                modifier = Modifier.cursorHand(),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Envoi en cours…", fontSize = 13.sp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Envoyer la demande", fontSize = 13.sp)
                }
            }
        }
    )
}
