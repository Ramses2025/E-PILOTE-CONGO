package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import cg.epilote.desktop.ui.theme.hoverScale

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
