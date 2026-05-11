package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.PaymentReceiptDto
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import java.awt.Desktop
import java.io.File

// ── Payment Receipt Detail Dialog ─────────────────────────────────────────────

@Composable
internal fun PaymentReceiptDetailDialog(
    receipt: PaymentReceiptDto,
    groupeNom: String,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    planNom: String = "",
    platformName: String = "E-Pilote Congo"
) {
    val scrollState = rememberScrollState()
    val receiptRef = receipt.id.take(12).uppercase()
    val methodColor = when (receipt.paymentMethod.lowercase()) {
        "cash" -> Color(0xFF059669)
        "check" -> Color(0xFF2563EB)
        "bank_transfer" -> Color(0xFF6D28D9)
        else -> Color(0xFF64748B)
    }
    AdminDialogWindow(
        title = "Reçu de paiement",
        subtitle = "$groupeNom • ${formatDate(receipt.receivedAt)}",
        onDismiss = onDismiss,
        size = DpSize(660.dp, 560.dp),
        content = {
            Column(modifier = Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF0FDF4)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF059669), modifier = Modifier.size(28.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Paiement confirmé", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF064E3B))
                                Text("Réf. $receiptRef", fontSize = 11.sp, color = Color(0xFF059669))
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(formatMoneyXaf(receipt.montantXAF), fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF059669))
                            Surface(shape = RoundedCornerShape(999.dp), color = methodColor.copy(alpha = 0.12f)) {
                                Text(receipt.paymentMethodLabel.ifBlank { receipt.paymentMethod },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    fontSize = 10.sp, color = methodColor, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Détails du reçu", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF334155))
                        PaymentDetailInfoRow("Groupe scolaire", groupeNom)
                        if (!receipt.paidBy.isNullOrBlank()) PaymentDetailInfoRow("Payé par", receipt.paidBy!!)
                        if (!receipt.externalReference.isNullOrBlank()) PaymentDetailInfoRow("Référence ext.", receipt.externalReference!!)
                        PaymentDetailInfoRow("Date de réception", formatDate(receipt.receivedAt))
                        PaymentDetailInfoRow("Reçu par", receipt.receivedBy.ifBlank { "—" })
                    }
                }
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFEDE9FE).copy(alpha = 0.35f)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Période d'accès activée", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF4C1D95))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            PaymentDetailInfoRow("Début", formatDate(receipt.accessStart), modifier = Modifier.weight(1f))
                            PaymentDetailInfoRow("Fin", formatDate(receipt.accessEnd), modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (receipt.notes.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF8E1)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Notes", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF92400E))
                            Text(receipt.notes, fontSize = 12.sp, color = Color(0xFF78350F))
                        }
                    }
                }
                if (receipt.invoiceId != null) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                            Text(
                                "Facture #${receipt.invoiceId!!.take(8)} générée automatiquement. Consultez la page Factures pour la gérer.",
                                fontSize = 11.sp, color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        },
        actions = {
            OutlinedButton(
                onClick = { printReceiptHtml(receipt, groupeNom, planNom, platformName) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.cursorHand()
            ) {
                Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                Text("  Imprimer le reçu", fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.cursorHand()) {
                    Text("Supprimer", color = Color(0xFFB3261E))
                }
            }
            Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp), modifier = Modifier.cursorHand()) {
                Text("Fermer")
            }
        }
    )
}

@Composable
private fun PaymentDetailInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = EpiloteTextMuted)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D3557), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun loadLogoBase64(): String = runCatching {
    val bytes = Thread.currentThread().contextClassLoader
        .getResourceAsStream("logo.svg")?.readBytes() ?: return@runCatching ""
    "data:image/svg+xml;base64," + java.util.Base64.getEncoder().encodeToString(bytes)
}.getOrDefault("")

private fun printReceiptHtml(
    receipt: PaymentReceiptDto,
    groupeNom: String,
    planNom: String = "",
    platformName: String = "E-Pilote Congo"
) {
    val ref = receipt.id.take(12).uppercase()
    val now = formatDate(System.currentTimeMillis())
    val invoiceRef = receipt.invoiceId?.take(14)?.uppercase()
    val logoDataUri = loadLogoBase64()
    val html = """
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8">
            <title>Reçu $ref — $platformName</title>
            <style>
                @page { margin: 20mm; }
                @media print { .no-print { display: none !important; } body { margin: 0; } }
                * { box-sizing: border-box; }
                body { font-family: 'Segoe UI', Arial, sans-serif; max-width: 680px; margin: 0 auto; color: #0f172a; background: #fff; padding: 32px 24px; }
                .platform-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 3px solid #059669; }
                .platform-name { font-size: 22px; font-weight: 800; color: #059669; letter-spacing: -0.5px; }
                .platform-sub { font-size: 11px; color: #64748b; margin-top: 2px; }
                .receipt-badge { background: #059669; color: #fff; padding: 6px 14px; border-radius: 6px; font-size: 11px; font-weight: 700; letter-spacing: 1px; }
                .receipt-title { text-align: center; margin: 20px 0; }
                .receipt-title h1 { font-size: 18px; font-weight: 700; color: #1e3a5f; margin: 0; text-transform: uppercase; letter-spacing: 1.5px; }
                .receipt-title .ref { font-size: 13px; color: #64748b; margin-top: 4px; }
                .amount-box { background: linear-gradient(135deg, #f0fdf4, #dcfce7); border: 2px solid #86efac; border-radius: 12px; text-align: center; padding: 20px; margin: 20px 0; }
                .amount-box .amount { font-size: 38px; font-weight: 800; color: #059669; line-height: 1; }
                .amount-box .method { margin-top: 8px; font-size: 13px; color: #047857; font-weight: 600; }
                .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin: 20px 0; }
                .info-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px 14px; }
                .info-card .label { font-size: 10px; color: #94a3b8; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
                .info-card .value { font-size: 13px; font-weight: 600; color: #0f172a; }
                .details-section { margin: 20px 0; }
                .details-section h3 { font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; border-bottom: 1px solid #e2e8f0; padding-bottom: 8px; margin-bottom: 12px; }
                table { width: 100%; border-collapse: collapse; }
                tr:not(:last-child) td, tr:not(:last-child) th { border-bottom: 1px solid #f1f5f9; }
                th, td { padding: 8px 4px; font-size: 12px; text-align: left; }
                th { color: #64748b; font-weight: 600; width: 42%; }
                td { color: #1e293b; font-weight: 500; }
                .signature { display: grid; grid-template-columns: 1fr 1fr; gap: 40px; margin-top: 32px; padding-top: 16px; border-top: 1px solid #e2e8f0; }
                .sig-box { text-align: center; }
                .sig-box .sig-line { border-top: 1px dashed #94a3b8; margin-top: 40px; }
                .sig-box .sig-label { font-size: 11px; color: #64748b; margin-top: 4px; }
                .footer { margin-top: 28px; padding-top: 16px; border-top: 2px solid #059669; display: flex; justify-content: space-between; align-items: center; }
                .footer .footer-brand { font-size: 12px; font-weight: 700; color: #059669; }
                .footer .footer-info { font-size: 10px; color: #94a3b8; text-align: right; }
                .btn-print { display: block; margin: 28px auto 0; padding: 12px 32px; background: #059669; color: #fff; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; }
            </style>
        </head>
        <body>
            <div class="platform-header">
                <div style="display: flex; align-items: center; gap: 12px;">
                    ${if (logoDataUri.isNotBlank()) """<img src="$logoDataUri" alt="Logo" style="width: 48px; height: 48px; border-radius: 50%;">""" else ""}
                    <div><div class="platform-name">$platformName</div><div class="platform-sub">Plateforme de gestion scolaire</div></div>
                </div>
                <div class="receipt-badge">REÇU OFFICIEL</div>
            </div>
            <div class="receipt-title">
                <h1>Reçu de Paiement</h1>
                <div class="ref">Réf. $ref &nbsp;•&nbsp; Émis le $now</div>
            </div>
            <div class="amount-box">
                <div class="amount">${formatMoneyXaf(receipt.montantXAF)}</div>
                <div class="method">${receipt.paymentMethodLabel.ifBlank { receipt.paymentMethod }}</div>
            </div>
            <div class="info-grid">
                <div class="info-card"><div class="label">Groupe scolaire</div><div class="value">$groupeNom</div></div>
                ${if (planNom.isNotBlank()) """<div class="info-card"><div class="label">Plan / Abonnement</div><div class="value">$planNom</div></div>""" else ""}
                <div class="info-card"><div class="label">Période d'accès</div><div class="value">${formatDate(receipt.accessStart)} → ${formatDate(receipt.accessEnd)}</div></div>
                <div class="info-card"><div class="label">Date de réception</div><div class="value">${formatDate(receipt.receivedAt)}</div></div>
            </div>
            <div class="details-section">
                <h3>Détails du paiement</h3>
                <table>
                    ${if (!receipt.paidBy.isNullOrBlank()) "<tr><th>Payé par</th><td>${receipt.paidBy}</td></tr>" else ""}
                    ${if (!receipt.externalReference.isNullOrBlank()) "<tr><th>Réf. externe</th><td>${receipt.externalReference}</td></tr>" else ""}
                    ${if (invoiceRef != null) "<tr><th>Facture liée</th><td>#$invoiceRef</td></tr>" else ""}
                    <tr><th>Reçu par</th><td>${receipt.receivedBy.let { r -> when { r.isBlank() -> "Agent $platformName"; r.startsWith("user::") -> "Administrateur $platformName"; else -> r } }}</td></tr>
                    ${if (receipt.notes.isNotBlank()) "<tr><th>Notes</th><td>${receipt.notes}</td></tr>" else ""}
                </table>
            </div>
            <div class="signature">
                <div class="sig-box"><div class="sig-line"></div><div class="sig-label">Signature du payeur</div></div>
                <div class="sig-box"><div class="sig-line"></div><div class="sig-label">Cachet &amp; Signature $platformName</div></div>
            </div>
            <div class="footer">
                <div style="display: flex; align-items: center; gap: 8px;">
                    ${if (logoDataUri.isNotBlank()) """<img src="$logoDataUri" alt="Logo" style="width: 24px; height: 24px; border-radius: 50%;">""" else ""}
                    <div class="footer-brand">$platformName</div>
                </div>
                <div class="footer-info">Document officiel &mdash; émis le $now<br>Ce reçu fait foi de paiement auprès de $platformName.</div>
            </div>
            <button class="btn-print no-print" onclick="window.print()">Imprimer le reçu</button>
        </body>
        </html>
    """.trimIndent()
    try {
        val tmpFile = File.createTempFile("recu_$ref", ".html")
        tmpFile.writeText(html)
        Desktop.getDesktop().browse(tmpFile.toURI())
    } catch (_: Exception) { /* Best-effort */ }
}
