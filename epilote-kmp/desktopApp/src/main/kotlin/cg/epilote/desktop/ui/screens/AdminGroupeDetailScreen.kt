package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.*
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.data.InvoiceApiDto
import cg.epilote.desktop.data.PaymentReceiptDto
import cg.epilote.desktop.data.SubscriptionApiDto
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.screens.superadmin.formatXAF
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AdminGroupeDetailScreen(
    groupe: GroupeDto,
    plans: List<PlanDto>,
    rawSubscriptions: List<SubscriptionApiDto>,
    rawInvoices: List<InvoiceApiDto>,
    scope: CoroutineScope,
    client: DesktopAdminClient,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    var receipts by remember { mutableStateOf<List<PaymentReceiptDto>>(emptyList()) }
    var receiptsLoading by remember { mutableStateOf(true) }
    var recordPaymentTarget by remember { mutableStateOf<SubscriptionDto?>(null) }
    var actionFeedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }

    val groupSubscriptions = remember(rawSubscriptions, groupe) {
        rawSubscriptions
            .filter { it.groupeId == groupe.id }
            .sortedByDescending { it.createdAt }
    }
    val groupInvoices = remember(rawInvoices, groupe) {
        rawInvoices.filter { it.groupeId == groupe.id }.sortedByDescending { it.dateEmission }
    }

    LaunchedEffect(groupe.id) {
        receiptsLoading = true
        receipts = runCatching { client.listPaymentReceiptsByGroupe(groupe.id) }
            .getOrNull().orEmpty()
            .sortedByDescending { it.receivedAt }
        receiptsLoading = false
    }

    val totalPaid = remember(receipts) { receipts.sumOf { it.montantXAF } }
    val totalInvoiced = remember(groupInvoices) { groupInvoices.sumOf { it.montantXAF } }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Retour", tint = Color(0xFF475467))
            }
            Box(
                modifier = Modifier.size(48.dp).background(EpiloteGreen.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    groupe.nom.firstOrNull()?.uppercase() ?: "G",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = EpiloteGreen
                )
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(groupe.nom, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (groupe.isActive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ) {
                        Text(
                            if (groupe.isActive) "Actif" else "Suspendu",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (groupe.isActive) Color(0xFF15803D) else Color(0xFFB91C1C)
                        )
                    }
                }
                Text(groupe.slug, fontSize = 12.sp, color = EpiloteTextMuted)
            }
        }

        actionFeedback?.let { AdminFeedbackBanner(feedback = it, onDismiss = { actionFeedback = null }) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailKpiCard(Icons.Default.School, "Écoles", "${groupe.ecolesCount}", EpiloteGreen, Modifier.weight(1f))
            DetailKpiCard(Icons.Default.People, "Utilisateurs", "${groupe.usersCount}", Color(0xFF2563EB), Modifier.weight(1f))
            DetailKpiCard(Icons.Default.Receipt, "Factures", "${groupInvoices.size}", Color(0xFF7C3AED), Modifier.weight(1f))
            DetailKpiCard(Icons.Default.Business, "Paiements reçus", "${receipts.size}", Color(0xFF059669), Modifier.weight(1f))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Informations générales", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                HorizontalDivider(color = Color(0xFFF1F5F9))
                DetailInfoRow(Icons.Default.LocationOn, "Localisation", "${groupe.department ?: "—"}, ${groupe.city ?: "—"}")
                if (!groupe.email.isNullOrBlank()) DetailInfoRow(Icons.Default.Email, "Email", groupe.email!!)
                if (!groupe.phone.isNullOrBlank()) DetailInfoRow(Icons.Default.Phone, "Téléphone", groupe.phone!!)
                DetailInfoRow(Icons.Default.Business, "Créé le", formatDate(groupe.createdAt))
                if (groupe.foundedYear != null) DetailInfoRow(Icons.Default.School, "Fondé en", "${groupe.foundedYear}")
                if (!groupe.description.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Description", fontSize = 12.sp, color = EpiloteTextMuted)
                        Text(groupe.description!!, fontSize = 12.sp)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Abonnements (${groupSubscriptions.size})", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (groupSubscriptions.isEmpty()) {
                    Text("Aucun abonnement.", fontSize = 12.sp, color = EpiloteTextMuted)
                } else {
                    groupSubscriptions.forEach { sub ->
                        val plan = plans.firstOrNull { it.id == sub.planId }
                        val statusColor = subscriptionStatusColor(sub.statut)
                        val now = System.currentTimeMillis()
                        val in30 = now + 30L * 86_400_000L
                        val expiringSoon = sub.statut == "active" && sub.dateFin in now..in30
                        val subAsDto = SubscriptionDto(
                            id = sub.id.ifBlank { "sub::${sub.groupeId}" },
                            groupeId = sub.groupeId,
                            groupeNom = groupe.nom,
                            groupeSlug = groupe.slug,
                            planId = sub.planId,
                            planNom = plan?.nom ?: sub.planId,
                            prixXAF = plan?.prixXAF ?: 0L,
                            statut = sub.statut,
                            dateDebut = sub.dateDebut,
                            dateFin = sub.dateFin,
                            renouvellementAuto = sub.renouvellementAuto,
                            createdAt = sub.createdAt,
                            maxStudents = plan?.maxStudents ?: 0,
                            maxPersonnel = plan?.maxPersonnel ?: 0,
                            moduleCount = plan?.modulesIncluded?.size ?: 0
                        )
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(plan?.nom ?: sub.planId, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.10f)) {
                                            Text(subscriptionStatusLabel(sub.statut), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                                        }
                                        if (expiringSoon) {
                                            Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFFEF3C7)) {
                                                val daysLeft = ((sub.dateFin - now) / 86_400_000L).toInt().coerceAtLeast(0)
                                                Text("J-$daysLeft", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    val planPrice = plan?.prixXAF ?: 0L
                                    Text(
                                        "${formatDate(sub.dateDebut)} → ${formatDate(sub.dateFin)} • ${formatXAF(planPrice)}",
                                        fontSize = 11.sp,
                                        color = EpiloteTextMuted
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { recordPaymentTarget = subAsDto },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Payments, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Paiement", fontSize = 11.sp)
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 6.dp))
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Paiements reçus (${receipts.size})", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (receiptsLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = EpiloteGreen)
                }
                Text("Total encaissé : ${formatXAF(totalPaid)}", fontSize = 12.sp, color = Color(0xFF059669), fontWeight = FontWeight.SemiBold)
                if (!receiptsLoading && receipts.isEmpty()) {
                    Text("Aucun paiement enregistré.", fontSize = 12.sp, color = EpiloteTextMuted)
                } else {
                    receipts.take(5).forEach { r ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(r.paymentMethodLabel.ifBlank { r.paymentMethod }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(formatDate(r.receivedAt), fontSize = 11.sp, color = EpiloteTextMuted)
                            }
                            Text(formatXAF(r.montantXAF), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF059669))
                        }
                    }
                    if (receipts.size > 5) {
                        Text("+ ${receipts.size - 5} paiement(s) supplémentaire(s)", fontSize = 11.sp, color = EpiloteTextMuted)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Factures (${groupInvoices.size})", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Total facturé : ${formatXAF(totalInvoiced)}", fontSize = 12.sp, color = EpiloteTextMuted)
                if (groupInvoices.isEmpty()) {
                    Text("Aucune facture.", fontSize = 12.sp, color = EpiloteTextMuted)
                } else {
                    groupInvoices.take(5).forEach { inv ->
                        val statusColor = invoiceStatusColor(inv.statut)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(inv.reference.ifBlank { inv.id.take(12) }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(formatDate(inv.dateEmission), fontSize = 11.sp, color = EpiloteTextMuted)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.10f)) {
                                    Text(inv.statut, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                                }
                                Text(formatXAF(inv.montantXAF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                    if (groupInvoices.size > 5) {
                        Text("+ ${groupInvoices.size - 5} facture(s) supplémentaire(s)", fontSize = 11.sp, color = EpiloteTextMuted)
                    }
                }
            }
        }
    }

    recordPaymentTarget?.let { target ->
        RecordPaymentDialog(
            subscription = target,
            client = client,
            scope = scope,
            onDismiss = { recordPaymentTarget = null },
            onSuccess = { receipt ->
                recordPaymentTarget = null
                actionFeedback = AdminFeedbackMessage("Paiement de ${formatXAF(receipt.montantXAF)} enregistré.")
                onRefresh()
            }
        )
    }
}

@Composable
private fun DetailKpiCard(icon: ImageVector, label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
            Text(label, fontSize = 11.sp, color = EpiloteTextMuted)
        }
    }
}

@Composable
private fun DetailInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = EpiloteTextMuted, modifier = Modifier.size(16.dp))
        Text("$label : ", fontSize = 12.sp, color = EpiloteTextMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
