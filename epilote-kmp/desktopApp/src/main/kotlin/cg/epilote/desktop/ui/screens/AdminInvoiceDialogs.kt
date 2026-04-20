package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val invoiceDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun formatInvoiceDateInput(epochMillis: Long): String = runCatching {
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(invoiceDateFormatter)
}.getOrDefault("")

private fun parseInvoiceDateInput(value: String): Long? = try {
    LocalDate.parse(value.trim(), invoiceDateFormatter)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
} catch (_: DateTimeParseException) {
    null
}

@Composable
internal fun InvoiceDetailDialog(
    invoice: InvoiceDto,
    onDismiss: () -> Unit,
    onStatusChange: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val statusColor = invoiceStatusColor(invoice.statut)
    val accentColor = if (invoice.planId.isNotBlank()) planColorById(invoice.planId) else Color(0xFF2563EB)
    val availableActions = remember(invoice.statut) {
        buildList {
            if (invoice.statut != "sent") add("sent")
            if (invoice.statut != "paid") add("paid")
            if (invoice.statut != "overdue") add("overdue")
            if (invoice.statut != "cancelled") add("cancelled")
        }
    }

    AdminDialogWindow(
        title = invoice.groupeNom,
        subtitle = invoice.reference.ifBlank { invoice.id },
        onDismiss = onDismiss,
        size = DpSize(680.dp, 560.dp),
        content = {
            Column(modifier = Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.10f)) {
                        Text(invoiceStatusLabel(invoice.statut), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                    }
                    Surface(shape = RoundedCornerShape(999.dp), color = accentColor.copy(alpha = 0.12f)) {
                        Text(invoice.planNom, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(color = Color(0xFFE9EEF5))
                InvoiceDetailRow("Groupe", invoice.groupeNom)
                InvoiceDetailRow("Identifiant groupe", invoice.groupeId)
                InvoiceDetailRow("Abonnement", invoice.subscriptionId)
                InvoiceDetailRow("Montant", formatMoneyXaf(invoice.montantXAF))
                InvoiceDetailRow("Émission", formatDate(invoice.dateEmission))
                InvoiceDetailRow("Échéance", formatDate(invoice.dateEcheance))
                InvoiceDetailRow("Paiement", invoice.datePaiement?.let(::formatDate) ?: "Non encaissée")
                HorizontalDivider(color = Color(0xFFE9EEF5))
                if (invoice.notes.isNotBlank()) {
                    Text("Notes", fontSize = 12.sp, color = EpiloteTextMuted)
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF8FAFC)) {
                        Text(invoice.notes, modifier = Modifier.fillMaxWidth().padding(14.dp), fontSize = 13.sp, color = Color(0xFF334155))
                    }
                }
            }
        },
        actions = {
            availableActions.forEach { action ->
                TextButton(onClick = { onStatusChange(action) }, modifier = Modifier.cursorHand()) {
                    Text(invoiceStatusActionTitle(action), color = invoiceStatusActionColor(action))
                }
            }
            Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp), modifier = Modifier.cursorHand()) {
                Text("Fermer")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InvoiceFormDialog(
    invoiceOptions: List<InvoiceOption>,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (option: InvoiceOption, amount: Long, dueDate: Long, notes: String) -> Unit
) {
    val scrollState = rememberScrollState()
    var groupMenuExpanded by remember { mutableStateOf(false) }
    var selectedGroupId by remember { mutableStateOf(invoiceOptions.firstOrNull()?.groupeId.orEmpty()) }
    val selectedOption = invoiceOptions.firstOrNull { it.groupeId == selectedGroupId } ?: invoiceOptions.firstOrNull()
    var amountValue by remember(selectedOption?.groupeId) { mutableStateOf((selectedOption?.suggestedAmountXAF ?: 0L).takeIf { it > 0 }?.toString().orEmpty()) }
    var dueDateValue by remember(selectedOption?.groupeId) { mutableStateOf(selectedOption?.suggestedDueDate?.let(::formatInvoiceDateInput).orEmpty()) }
    var notes by remember { mutableStateOf("") }
    val amount = amountValue.toLongOrNull()
    val dueDate = parseInvoiceDateInput(dueDateValue)
    val canSubmit = selectedOption != null && amount != null && amount > 0 && dueDate != null

    AdminDialogWindow(
        title = "Nouvelle facture",
        subtitle = "Émettre une facture plateforme pour un groupe abonné",
        onDismiss = onDismiss,
        size = DpSize(700.dp, 620.dp),
        content = {
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (invoiceOptions.isEmpty()) {
                    AdminFeedbackBanner(
                        feedback = AdminFeedbackMessage("Aucun abonnement disponible pour générer une facture.", isError = true),
                        onDismiss = {}
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Groupe scolaire", fontSize = 12.sp, color = EpiloteTextMuted)
                        OutlinedButton(onClick = { groupMenuExpanded = true }, modifier = Modifier.fillMaxWidth().cursorHand(), shape = RoundedCornerShape(12.dp)) {
                            Text(selectedOption?.groupeNom ?: "Sélectionner un groupe", modifier = Modifier.fillMaxWidth(), maxLines = 1)
                        }
                        DropdownMenu(expanded = groupMenuExpanded, onDismissRequest = { groupMenuExpanded = false }) {
                            invoiceOptions.forEach { option ->
                                DropdownMenuItem(text = { Text("${option.groupeNom} • ${option.planNom}") }, onClick = {
                                    selectedGroupId = option.groupeId
                                    groupMenuExpanded = false
                                })
                            }
                        }
                    }

                    selectedOption?.let { option ->
                        Surface(shape = RoundedCornerShape(14.dp), color = planColorById(option.planId).copy(alpha = 0.08f)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(option.groupeNom, fontWeight = FontWeight.Bold, color = Color(0xFF1D3557))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    InvoiceBadge(Icons.Default.Receipt, option.planNom, planColorById(option.planId))
                                    InvoiceBadge(Icons.Default.Paid, formatMoneyXaf(option.suggestedAmountXAF), Color(0xFF059669))
                                    InvoiceBadge(Icons.Default.CheckCircle, invoiceStatusLabel(option.subscriptionStatus), invoiceStatusColor(option.subscriptionStatus))
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountValue,
                        onValueChange = { amountValue = it.filter(Char::isDigit) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Montant XAF") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Paid, null) }
                    )

                    OutlinedTextField(
                        value = dueDateValue,
                        onValueChange = { dueDateValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Date d'échéance (yyyy-MM-dd)") },
                        supportingText = { Text("Exemple: 2026-04-30") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, null) }
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        label = { Text("Notes internes") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                errorMessage?.let {
                    AdminFeedbackBanner(feedback = AdminFeedbackMessage(it, isError = true), onDismiss = {})
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting, modifier = Modifier.cursorHand()) { Text("Annuler") }
            Button(
                onClick = { selectedOption?.let { option -> onSubmit(option, amount ?: 0L, dueDate ?: 0L, notes.trim()) } },
                enabled = !isSubmitting && canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D3557)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.cursorHand()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Text("Traitement…", modifier = Modifier.padding(start = 8.dp))
                } else {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Text("Émettre la facture", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    )
}

@Composable
private fun InvoiceDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = EpiloteTextMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D3557))
    }
}

@Composable
private fun InvoiceBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}
