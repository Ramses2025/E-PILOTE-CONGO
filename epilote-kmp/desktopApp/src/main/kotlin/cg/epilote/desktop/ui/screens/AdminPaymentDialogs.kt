package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.data.PaymentMethodDto
import cg.epilote.desktop.data.PaymentReceiptDto
import cg.epilote.desktop.data.RecordPaymentDto
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Formulaire « Enregistrer un paiement présentiel ».
 *
 * Workflow métier : le groupe scolaire se déplace au siège, paie en espèces (ou chèque,
 * virement…), le Super Admin saisit ce formulaire — l'abonnement est activé/renouvelé,
 * une facture est émise + marquée payée, et un reçu de paiement est horodaté.
 *
 * Mobile Money et Carte bancaire sont affichés mais désactivés — voir markers // TODO.
 */
@Composable
internal fun RecordPaymentDialog(
    subscription: SubscriptionDto,
    client: DesktopAdminClient,
    scope: kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit,
    onSuccess: (PaymentReceiptDto) -> Unit
) {
    var methods by remember { mutableStateOf<List<PaymentMethodDto>>(emptyList()) }
    var loadingMethods by remember { mutableStateOf(true) }
    var montantText by remember { mutableStateOf(subscription.prixXAF.takeIf { it > 0L }?.toString() ?: "") }
    var selectedMethodCode by remember { mutableStateOf("cash") }
    var methodMenuExpanded by remember { mutableStateOf(false) }
    var durationText by remember { mutableStateOf("12") }
    var externalReference by remember { mutableStateOf("") }
    var paidBy by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // Clé d'idempotence stable pour la durée de vie du dialogue : si l'utilisateur
    // double-clique ou si le réseau retry, le backend retourne le même reçu
    // (pas de facture doublon). Pattern Stripe documenté.
    val idempotencyKey = remember { UUID.randomUUID().toString() }

    LaunchedEffect(Unit) {
        loadingMethods = true
        val list = runCatching { client.listPaymentMethods() }.getOrNull().orEmpty()
        methods = list.ifEmpty {
            // Fallback si l'endpoint échoue — garde l'UI utilisable en mode dégradé.
            listOf(
                PaymentMethodDto("cash", "Espèces (présentiel)", true),
                PaymentMethodDto("check", "Chèque", true),
                PaymentMethodDto("bank_transfer", "Virement bancaire", true),
                PaymentMethodDto("mobile_money", "Mobile Money (à venir)", false),
                PaymentMethodDto("card", "Carte bancaire (à venir)", false)
            )
        }
        // S'assure que la sélection initiale est cohérente (cash si dispo).
        val preferred = methods.firstOrNull { it.code == "cash" && it.enabled }
            ?: methods.firstOrNull { it.enabled }
        preferred?.let { selectedMethodCode = it.code }
        loadingMethods = false
    }

    val selectedMethod = methods.firstOrNull { it.code == selectedMethodCode }
    val isSelectedEnabled = selectedMethod?.enabled == true

    AdminDialogWindow(
        title = "Enregistrer un paiement",
        subtitle = "${subscription.groupeNom} • ${subscription.planNom}",
        onDismiss = onDismiss,
        size = DpSize(620.dp, 640.dp),
        content = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Paiement présentiel reçu au siège", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF1D4ED8))
                        Text(
                            "Le groupe sera activé/renouvelé pour la durée indiquée. Une facture juridique sera émise " +
                                "et marquée comme payée.",
                            fontSize = 11.sp,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                }

                OutlinedTextField(
                    value = montantText,
                    onValueChange = { value -> montantText = value.filter { it.isDigit() } },
                    label = { Text("Montant reçu (FCFA)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Méthode de paiement", fontSize = 12.sp, color = EpiloteTextMuted)
                    OutlinedButton(
                        onClick = { methodMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth().cursorHand(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            selectedMethod?.label ?: "Choisir une méthode",
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )
                    }
                    DropdownMenu(expanded = methodMenuExpanded, onDismissRequest = { methodMenuExpanded = false }) {
                        methods.forEach { method ->
                            // TODO: Mobile Money — câblage de l'intégration MTN MoMo / Airtel Money à prévoir
                            //       dans une phase ultérieure. Pour l'instant, l'option est affichée mais désactivée.
                            // TODO: Carte bancaire — câblage de la passerelle (Stripe / GIM-UEMOA) à prévoir
                            //       dans une phase ultérieure. Pour l'instant, l'option est affichée mais désactivée.
                            DropdownMenuItem(
                                enabled = method.enabled,
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            method.label,
                                            color = if (method.enabled) Color(0xFF101828) else Color(0xFF98A2B3)
                                        )
                                        if (!method.enabled) {
                                            AssistChip(
                                                onClick = {},
                                                enabled = false,
                                                label = { Text("À venir", fontSize = 10.sp) },
                                                colors = AssistChipDefaults.assistChipColors(labelColor = Color(0xFF98A2B3))
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    if (method.enabled) {
                                        selectedMethodCode = method.code
                                        methodMenuExpanded = false
                                    }
                                }
                            )
                        }
                    }
                    if (!isSelectedEnabled && selectedMethod != null) {
                        Text(
                            "Cette méthode n'est pas encore disponible — sera câblée dans une phase ultérieure.",
                            fontSize = 11.sp,
                            color = Color(0xFFB42318)
                        )
                    }
                }

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { value -> durationText = value.filter { it.isDigit() }.take(3) },
                    label = { Text("Durée d'accès (mois)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = externalReference,
                    onValueChange = { externalReference = it },
                    label = { Text("Référence externe (N° chèque, N° transaction…)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = paidBy,
                    onValueChange = { paidBy = it },
                    label = { Text("Payeur (nom de la personne au guichet)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optionnel)") },
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                    maxLines = 4
                )

                errorMessage?.let { msg ->
                    Text(msg, color = Color(0xFFB42318), fontSize = 12.sp)
                }

                if (loadingMethods) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp))
                        Text("Chargement des méthodes de paiement…", fontSize = 11.sp, color = EpiloteTextMuted)
                    }
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
            Button(
                enabled = !submitting && !loadingMethods && isSelectedEnabled,
                shape = RoundedCornerShape(10.dp),
                onClick = {
                    val montant = montantText.toLongOrNull()
                    val duration = durationText.toIntOrNull() ?: 12
                    if (montant == null || montant <= 0L) {
                        errorMessage = "Le montant doit être un entier positif."
                        return@Button
                    }
                    if (duration <= 0) {
                        errorMessage = "La durée doit être supérieure à zéro."
                        return@Button
                    }
                    errorMessage = null
                    submitting = true
                    scope.launch {
                        val payload = RecordPaymentDto(
                            groupeId = subscription.groupeId,
                            subscriptionId = subscription.id,
                            montantXAF = montant,
                            paymentMethod = selectedMethodCode,
                            durationMonths = duration,
                            externalReference = externalReference.trim().ifBlank { null },
                            paidBy = paidBy.trim().ifBlank { null },
                            notes = notes.trim(),
                            idempotencyKey = idempotencyKey
                        )
                        val receipt = runCatching { client.recordPayment(payload) }.getOrNull()
                        submitting = false
                        if (receipt != null) {
                            onSuccess(receipt)
                        } else {
                            errorMessage = "Impossible d'enregistrer le paiement. Vérifie la connexion et réessaie."
                        }
                    }
                }
            ) {
                Text(if (submitting) "Enregistrement…" else "Enregistrer")
            }
        }
    )
}

/**
 * Affiche l'historique des paiements présentiels pour un groupe donné.
 * Source : `GET /api/super-admin/groupes/{groupeId}/payment-receipts`.
 */
@Composable
internal fun PaymentHistoryDialog(
    subscription: SubscriptionDto,
    client: DesktopAdminClient,
    scope: kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit
) {
    var receipts by remember { mutableStateOf<List<PaymentReceiptDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(subscription.groupeId) {
        loading = true
        error = null
        val list = runCatching { client.listPaymentReceiptsByGroupe(subscription.groupeId) }.getOrNull()
        if (list == null) {
            error = "Impossible de charger l'historique."
            receipts = emptyList()
        } else {
            receipts = list.sortedByDescending { it.receivedAt }
        }
        loading = false
    }

    AdminDialogWindow(
        title = "Historique des paiements",
        subtitle = "${subscription.groupeNom} • ${receipts.size} paiement(s)",
        onDismiss = onDismiss,
        size = DpSize(720.dp, 560.dp),
        content = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    loading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text("Chargement…", fontSize = 12.sp, color = EpiloteTextMuted)
                    }
                    error != null -> Text(error!!, color = Color(0xFFB42318), fontSize = 12.sp)
                    receipts.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Aucun paiement enregistré", fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
                            Text(
                                "Les paiements présentiels apparaîtront ici dès qu'ils seront saisis.",
                                fontSize = 12.sp,
                                color = EpiloteTextMuted
                            )
                        }
                    }
                    else -> {
                        receipts.forEach { r ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(formatMoneyXaf(r.montantXAF), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF101828))
                                        Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFDCFCE7)) {
                                            Text(
                                                r.paymentMethodLabel.ifBlank { r.paymentMethod },
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                fontSize = 10.sp,
                                                color = Color(0xFF15803D),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFFEAECF0))
                                    Text(
                                        "Accès : ${cg.epilote.desktop.ui.screens.superadmin.formatDate(r.accessStart)} → " +
                                            cg.epilote.desktop.ui.screens.superadmin.formatDate(r.accessEnd),
                                        fontSize = 12.sp,
                                        color = Color(0xFF475467)
                                    )
                                    Text("Reçu le ${cg.epilote.desktop.ui.screens.superadmin.formatDate(r.receivedAt)}", fontSize = 11.sp, color = EpiloteTextMuted)
                                    if (!r.paidBy.isNullOrBlank()) {
                                        Text("Payeur : ${r.paidBy}", fontSize = 11.sp, color = EpiloteTextMuted)
                                    }
                                    if (!r.externalReference.isNullOrBlank()) {
                                        Text("Réf. externe : ${r.externalReference}", fontSize = 11.sp, color = EpiloteTextMuted)
                                    }
                                    if (r.notes.isNotBlank()) {
                                        Text(r.notes, fontSize = 11.sp, color = EpiloteTextMuted)
                                    }
                                    if (!r.invoiceId.isNullOrBlank()) {
                                        Text("Facture liée : ${r.invoiceId}", fontSize = 10.sp, color = Color(0xFF6B7280))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        actions = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) { Text("Fermer") }
        }
    )
}
