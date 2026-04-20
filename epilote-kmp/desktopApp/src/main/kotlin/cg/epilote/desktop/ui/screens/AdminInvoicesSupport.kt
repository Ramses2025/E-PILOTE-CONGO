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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.screens.superadmin.KpiCard
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.AnimatedCardEntrance
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import cg.epilote.desktop.ui.theme.hoverScale

internal fun invoiceStatusColor(status: String): Color = when (status.lowercase()) {
    "paid" -> Color(0xFF059669)
    "overdue" -> Color(0xFFDC2626)
    "sent" -> Color(0xFF2563EB)
    "draft" -> Color(0xFFD97706)
    "cancelled" -> Color(0xFF64748B)
    else -> Color(0xFF64748B)
}

internal fun invoiceStatusLabel(status: String): String = when (status.lowercase()) {
    "paid" -> "Payée"
    "overdue" -> "En retard"
    "sent" -> "Envoyée"
    "draft" -> "Brouillon"
    "cancelled" -> "Annulée"
    else -> status.replaceFirstChar { it.uppercase() }
}

internal fun invoiceStatusActionTitle(status: String): String = when (status.lowercase()) {
    "paid" -> "Marquer payée"
    "sent" -> "Marquer envoyée"
    "overdue" -> "Marquer en retard"
    "cancelled" -> "Annuler"
    else -> "Mettre à jour"
}

internal fun invoiceStatusActionMessage(invoice: InvoiceDto, status: String): String = when (status.lowercase()) {
    "paid" -> "Confirmez-vous le paiement de la facture ${invoice.reference} pour ${invoice.groupeNom} ?"
    "sent" -> "Confirmez-vous l'envoi de la facture ${invoice.reference} au groupe ${invoice.groupeNom} ?"
    "overdue" -> "Confirmez-vous que la facture ${invoice.reference} est désormais en retard ?"
    "cancelled" -> "Voulez-vous annuler la facture ${invoice.reference} ? Cette action impactera le suivi d'encaissement."
    else -> "Confirmez-vous la mise à jour de cette facture ?"
}

internal fun invoiceStatusActionColor(status: String): Color = when (status.lowercase()) {
    "paid" -> Color(0xFF059669)
    "sent" -> Color(0xFF2563EB)
    "overdue" -> Color(0xFFDC2626)
    "cancelled" -> Color(0xFF6B7280)
    else -> Color(0xFF1D3557)
}

@Composable
internal fun InvoiceKpiRow(
    totalInvoices: Int,
    paidInvoices: Int,
    overdueInvoices: Int,
    outstandingAmount: Long,
    totalAmount: Long,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 980.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Receipt, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF2563EB), label = "Factures", value = "$totalInvoices", trendLabel = "$paidInvoices payées")
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.CheckCircle, iconBg = Color(0xFFD1FAE5), iconTint = Color(0xFF059669), label = "Encaissements", value = formatMoneyXaf(totalAmount - outstandingAmount), trendLabel = "encaissés")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.HourglassBottom, iconBg = Color(0xFFFEE2E2), iconTint = Color(0xFFDC2626), label = "Retards", value = "$overdueInvoices", trendLabel = "à relancer")
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Paid, iconBg = Color(0xFFEDE9FE), iconTint = Color(0xFF6D28D9), label = "Restant dû", value = formatMoneyXaf(outstandingAmount), trendLabel = "sur ${formatMoneyXaf(totalAmount)}")
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Receipt, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF2563EB), label = "Factures", value = "$totalInvoices", trendLabel = "$paidInvoices payées")
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.CheckCircle, iconBg = Color(0xFFD1FAE5), iconTint = Color(0xFF059669), label = "Encaissements", value = formatMoneyXaf(totalAmount - outstandingAmount), trendLabel = "encaissés")
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.HourglassBottom, iconBg = Color(0xFFFEE2E2), iconTint = Color(0xFFDC2626), label = "Retards", value = "$overdueInvoices", trendLabel = "à relancer")
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Paid, iconBg = Color(0xFFEDE9FE), iconTint = Color(0xFF6D28D9), label = "Restant dû", value = formatMoneyXaf(outstandingAmount), trendLabel = "sur ${formatMoneyXaf(totalAmount)}")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InvoiceToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterStatus: String,
    onFilterStatusChange: (String) -> Unit,
    filterGroupId: String,
    onFilterGroupChange: (String) -> Unit,
    groupOptions: List<Pair<String, String>>,
    sortBy: String,
    onSortChange: (String) -> Unit,
    viewMode: String,
    onViewModeChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreateInvoice: () -> Unit,
    totalResults: Int,
    modifier: Modifier = Modifier
) {
    var showGroupMenu by remember { mutableStateOf(false) }
    val selectedGroupLabel = groupOptions.firstOrNull { it.first == filterGroupId }?.second ?: "Tous les groupes"

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                label = { Text("Rechercher un groupe, un plan, une référence…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFF1F5F9)) {
                    Text("$totalResults résultat(s)", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9)) {
                    Row(Modifier.padding(2.dp)) {
                        IconButton(onClick = { onViewModeChange("card") }, modifier = Modifier.size(32.dp).cursorHand()) {
                            Icon(Icons.Default.ViewModule, "Cartes", Modifier.size(18.dp), tint = if (viewMode == "card") Color(0xFF1D3557) else Color(0xFF94A3B8))
                        }
                        IconButton(onClick = { onViewModeChange("table") }, modifier = Modifier.size(32.dp).cursorHand()) {
                            Icon(Icons.Default.TableChart, "Tableau", Modifier.size(18.dp), tint = if (viewMode == "table") Color(0xFF1D3557) else Color(0xFF94A3B8))
                        }
                    }
                }
                FilledTonalButton(onClick = onRefresh, shape = RoundedCornerShape(10.dp), modifier = Modifier.cursorHand()) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Text("Actualiser", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                }
                FilledTonalButton(
                    onClick = onCreateInvoice,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1D3557), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.AddCard, null, modifier = Modifier.size(16.dp))
                    Text("Nouvelle facture", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all" to "Tous", "draft" to "Brouillons", "sent" to "Envoyées", "paid" to "Payées", "overdue" to "Retards", "cancelled" to "Annulées").forEach { (value, label) ->
                FilterChip(
                    selected = filterStatus == value,
                    onClick = { onFilterStatusChange(value) },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.cursorHand(),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1D3557).copy(alpha = 0.12f), selectedLabelColor = Color(0xFF1D3557))
                )
            }

            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE2E8F0)) {
                Text("│", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, color = Color(0xFF94A3B8))
            }

            Box {
                OutlinedButton(onClick = { showGroupMenu = true }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                    Text(selectedGroupLabel, fontSize = 11.sp, maxLines = 1)
                }
                DropdownMenu(expanded = showGroupMenu, onDismissRequest = { showGroupMenu = false }) {
                    groupOptions.forEach { (value, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = {
                            onFilterGroupChange(value)
                            showGroupMenu = false
                        })
                    }
                }
            }

            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE2E8F0)) {
                Text("│", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, color = Color(0xFF94A3B8))
            }

            listOf("recent" to "Récent", "dueDate" to "Échéance", "amount" to "Montant", "group" to "Groupe").forEach { (value, label) ->
                FilterChip(
                    selected = sortBy == value,
                    onClick = { onSortChange(value) },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.cursorHand(),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF059669).copy(alpha = 0.12f), selectedLabelColor = Color(0xFF059669))
                )
            }
        }
    }
}

@Composable
internal fun InvoiceCardGrid(invoices: List<InvoiceDto>, onViewDetail: (InvoiceDto) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cols = when {
            maxWidth > 1400.dp -> 4
            maxWidth > 1050.dp -> 3
            maxWidth > 700.dp -> 2
            else -> 1
        }
        val cardWidth = (maxWidth - 16.dp * (cols - 1)) / cols
        val rows = invoices.chunked(cols)

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rows.forEachIndexed { rowIndex, rowInvoices ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowInvoices.forEachIndexed { colIndex, invoice ->
                        val globalIndex = rowIndex * cols + colIndex
                        Box(modifier = Modifier.width(cardWidth)) {
                            AnimatedCardEntrance(index = globalIndex) {
                                InvoiceGridCard(invoice = invoice, onClick = { onViewDetail(invoice) })
                            }
                        }
                    }
                    repeat(cols - rowInvoices.size) {
                        Spacer(Modifier.width(cardWidth))
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceGridCard(invoice: InvoiceDto, onClick: () -> Unit) {
    val statusColor = invoiceStatusColor(invoice.statut)
    val accentColor = if (invoice.planId.isNotBlank()) planColorById(invoice.planId) else Color(0xFF2563EB)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().cursorHand().hoverScale(1.008f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = accentColor.copy(alpha = 0.12f)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Text(invoice.planNom, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    }
                }
                Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.10f)) {
                    Text(invoiceStatusLabel(invoice.statut), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(invoice.groupeNom, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D3557), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(invoice.reference.ifBlank { invoice.groupeSlug.ifBlank { invoice.id } }, fontSize = 12.sp, color = EpiloteTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC)) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InvoiceMetaRow(Icons.Default.Paid, formatMoneyXaf(invoice.montantXAF))
                    InvoiceMetaRow(Icons.Default.CalendarMonth, "Émise ${formatDate(invoice.dateEmission)}")
                    InvoiceMetaRow(Icons.Default.HourglassBottom, "Échéance ${formatDate(invoice.dateEcheance)}")
                }
            }

            if (invoice.notes.isNotBlank()) {
                Text(invoice.notes, fontSize = 12.sp, color = Color(0xFF475569), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Cliquer pour gérer la facture", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF1D3557), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun InvoiceMetaRow(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = EpiloteTextMuted)
        Text(value, fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun InvoiceTableView(invoices: List<InvoiceDto>, onViewDetail: (InvoiceDto) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                InvoiceHeaderCell("Groupe", Modifier.weight(2.2f))
                InvoiceHeaderCell("Plan", Modifier.weight(1.2f))
                InvoiceHeaderCell("Référence", Modifier.weight(1.2f))
                InvoiceHeaderCell("Montant", Modifier.weight(1.1f))
                InvoiceHeaderCell("Échéance", Modifier.weight(1.1f))
                InvoiceHeaderCell("Statut", Modifier.weight(1f))
                InvoiceHeaderCell("Action", Modifier.weight(0.8f))
            }
            HorizontalDivider(color = Color(0xFFE2E8F0))

            if (invoices.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    Text("Aucune facture à afficher", color = EpiloteTextMuted)
                }
            }

            invoices.forEachIndexed { index, invoice ->
                InvoiceTableRow(invoice = invoice, onViewDetail = { onViewDetail(invoice) })
                if (index < invoices.lastIndex) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}

@Composable
private fun InvoiceHeaderCell(text: String, modifier: Modifier) {
    Text(text, modifier, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B), maxLines = 1)
}

@Composable
private fun InvoiceTableRow(invoice: InvoiceDto, onViewDetail: () -> Unit) {
    val statusColor = invoiceStatusColor(invoice.statut)
    val accentColor = if (invoice.planId.isNotBlank()) planColorById(invoice.planId) else Color(0xFF2563EB)

    Surface(onClick = onViewDetail, modifier = Modifier.fillMaxWidth().cursorHand().hoverScale(1.004f), color = Color.Transparent) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(2.2f)) {
                Text(invoice.groupeNom, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(invoice.groupeSlug.ifBlank { invoice.groupeId }, fontSize = 11.sp, color = EpiloteTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.weight(1.2f)) {
                Surface(shape = RoundedCornerShape(6.dp), color = accentColor.copy(alpha = 0.12f)) {
                    Text(invoice.planNom, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
                }
            }
            Text(invoice.reference, modifier = Modifier.weight(1.2f), fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1)
            Text(formatMoneyXaf(invoice.montantXAF), modifier = Modifier.weight(1.1f), fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1)
            Text(formatDate(invoice.dateEcheance), modifier = Modifier.weight(1.1f), fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1)
            Box(Modifier.weight(1f)) {
                Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.10f)) {
                    Text(invoiceStatusLabel(invoice.statut), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                }
            }
            IconButton(onClick = onViewDetail, modifier = Modifier.weight(0.8f).size(30.dp).cursorHand()) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp), tint = Color(0xFF1D3557))
            }
        }
    }
}
