package cg.epilote.backend.admin

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
class AdminInvoicePdfService(
    private val repo: AdminRepository,
    private val planRepo: AdminPlanRepository,
    private val subscriptionRepo: AdminSubscriptionRepository
) {
    data class GeneratedInvoicePdf(
        val fileName: String,
        val bytes: ByteArray
    )

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val currencyFormatter = NumberFormat.getInstance(Locale.FRANCE)
    private val regularFont = PDType1Font(Standard14Fonts.FontName.HELVETICA)
    private val boldFont = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)

    suspend fun generateInvoicePdf(invoiceId: String): GeneratedInvoicePdf? {
        val invoice = repo.getInvoiceById(invoiceId) ?: return null
        val groupe = repo.getGroupeById(invoice.groupeId) ?: return null
        val subscription = subscriptionRepo.getSubscriptionById(invoice.subscriptionId)
        val plan = planRepo.getPlanById(subscription?.planId?.ifBlank { groupe.planId } ?: groupe.planId)
        val safeReference = invoice.reference.ifBlank { invoice.id }.replace(Regex("[^A-Za-z0-9._-]"), "-")

        val output = ByteArrayOutputStream()
        PDDocument().use { document ->
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            PDPageContentStream(document, page).use { content ->
                val pageWidth = page.mediaBox.width
                var y = page.mediaBox.height - 56f
                val left = 52f
                val right = pageWidth - 52f
                val lineHeight = 15f

                fun drawText(text: String, x: Float, yPos: Float, fontSize: Float = 11f, bold: Boolean = false) {
                    content.beginText()
                    content.setFont(if (bold) boldFont else regularFont, fontSize)
                    content.newLineAtOffset(x, yPos)
                    content.showText(text)
                    content.endText()
                }

                fun wrap(text: String, fontSize: Float, maxWidth: Float, bold: Boolean = false): List<String> {
                    val font = if (bold) boldFont else regularFont
                    if (text.isBlank()) return listOf("")
                    val words = text.trim().split(Regex("\\s+"))
                    val lines = mutableListOf<String>()
                    var current = ""
                    words.forEach { word ->
                        val candidate = if (current.isBlank()) word else "$current $word"
                        val width = font.getStringWidth(candidate) / 1000f * fontSize
                        if (width <= maxWidth || current.isBlank()) {
                            current = candidate
                        } else {
                            lines += current
                            current = word
                        }
                    }
                    if (current.isNotBlank()) lines += current
                    return lines
                }

                fun drawWrapped(label: String, value: String, x: Float, width: Float): Float {
                    drawText(label, x, y, 10f, bold = true)
                    y -= lineHeight
                    wrap(value, 10.5f, width).forEach { line ->
                        drawText(line, x, y, 10.5f)
                        y -= lineHeight
                    }
                    return y
                }

                fun divider() {
                    content.moveTo(left, y)
                    content.lineTo(right, y)
                    content.stroke()
                    y -= 16f
                }

                drawText("FACTURE OFFICIELLE", left, y, 18f, bold = true)
                drawText("E-PILOTE CONGO", right - 120f, y, 14f, bold = true)
                y -= 22f
                drawText("Plateforme de gestion scolaire", left, y, 10.5f)
                drawText("Client: ${groupe.nom}", right - 160f, y, 10.5f)
                y -= 18f
                divider()

                drawText("Référence: ${invoice.reference}", left, y, 11f, bold = true)
                drawText("Statut: ${invoice.statut.uppercase()}", right - 150f, y, 11f, bold = true)
                y -= 18f
                drawText("Date d'émission: ${formatDate(invoice.dateEmission)}", left, y)
                drawText("Date d'échéance: ${formatDate(invoice.dateEcheance)}", right - 180f, y)
                y -= 14f
                drawText("Date de paiement: ${invoice.datePaiement?.let(::formatDate) ?: "Non encaissée"}", left, y)
                y -= 18f
                divider()

                val clientWidth = 220f
                val platformWidth = 220f
                val topSectionY = y

                drawText("Émetteur", left, y, 12f, bold = true)
                y -= lineHeight
                drawWrapped("Raison sociale", "E-PILOTE CONGO", left, platformWidth)
                drawWrapped("Nature", "Contrat plateforme / abonnement groupe scolaire", left, platformWidth)
                drawWrapped("Contact", "+242 - Support plateforme", left, platformWidth)
                val platformEndY = y

                y = topSectionY
                val clientX = right - clientWidth
                drawText("Client", clientX, y, 12f, bold = true)
                y -= lineHeight
                drawWrapped("Groupe", groupe.nom, clientX, clientWidth)
                drawWrapped("Slug", groupe.slug.ifBlank { groupe.id }, clientX, clientWidth)
                drawWrapped("Email", groupe.email ?: "Non renseigné", clientX, clientWidth)
                drawWrapped("Téléphone", groupe.phone ?: "Non renseigné", clientX, clientWidth)
                drawWrapped("Adresse", listOfNotNull(groupe.address, groupe.city, groupe.department, groupe.country).joinToString(", ").ifBlank { "Non renseignée" }, clientX, clientWidth)
                val clientEndY = y

                y = minOf(platformEndY, clientEndY)
                divider()

                drawText("Objet", left, y, 12f, bold = true)
                y -= lineHeight
                drawWrapped("Abonnement", subscription?.id ?: invoice.subscriptionId, left, right - left)
                drawWrapped("Plan", plan?.nom ?: groupe.planId, left, right - left)
                drawWrapped("Description", "Accès contractuel à la plateforme E-PILOTE CONGO pour le groupe scolaire client.", left, right - left)
                divider()

                drawText("Détail financier", left, y, 12f, bold = true)
                y -= lineHeight
                drawText("Montant abonnement plateforme", left, y)
                drawText(formatMoney(invoice.montantXAF), right - 140f, y, 11f, bold = true)
                y -= 20f
                divider()
                drawText("Total TTC", left, y, 12f, bold = true)
                drawText(formatMoney(invoice.montantXAF), right - 140f, y, 13f, bold = true)
                y -= 22f

                if (invoice.notes.isNotBlank()) {
                    divider()
                    drawText("Notes", left, y, 12f, bold = true)
                    y -= lineHeight
                    wrap(invoice.notes, 10.5f, right - left).forEach { line ->
                        drawText(line, left, y, 10.5f)
                        y -= lineHeight
                    }
                }

                y = maxOf(y - 8f, 88f)
                divider()
                drawText("Document généré par la plateforme E-PILOTE CONGO.", left, y, 9.5f)
                y -= 13f
                drawText("Ce PDF constitue la facture officielle liée au contrat d'abonnement plateforme du groupe scolaire client.", left, y, 9.5f)
            }
            document.save(output)
        }
        return GeneratedInvoicePdf(fileName = "$safeReference.pdf", bytes = output.toByteArray())
    }

    private fun formatDate(epochMillis: Long): String = runCatching {
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of("UTC")).toLocalDate().format(dateFormatter)
    }.getOrDefault("-")

    private fun formatMoney(amount: Long): String = "${currencyFormatter.format(amount)} XAF"
}
