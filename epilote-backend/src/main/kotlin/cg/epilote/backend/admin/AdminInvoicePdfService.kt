package cg.epilote.backend.admin

import com.couchbase.client.kotlin.Bucket
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Locale

/**
 * Génère le PDF d'une facture plateforme — document officiel et contractuel.
 *
 * Mise en page juridique complète :
 *  - En-tête avec logo + raison sociale + informations légales plateforme (RCCM, NIU, siège)
 *  - Bloc Client avec toutes les coordonnées du groupe scolaire
 *  - Numéro unique (format `FAC-{YYYY}-{NNNNNN}` par défaut, configurable)
 *  - Dates d'émission et d'échéance
 *  - Tableau de prestations HT / TVA / TTC
 *  - Conditions de paiement
 *  - Espace signature & cachet
 *  - Pied de page avec mentions légales
 *
 * Les informations juridiques sont lues depuis `config::platform_identity` (Super Admin
 * les saisit depuis la page « Paramètres plateforme »). Si un champ est vide, le PDF
 * affiche un placeholder discret plutôt qu'une chaîne vide.
 *
 * Références officielles :
 *  - PDFBox 3.0 : https://pdfbox.apache.org/3.0/dependencies.html (chargement TTF, PDImageXObject)
 *  - Spring `@Service` : https://docs.spring.io/spring-framework/reference/core/beans/java.html
 */
@Service
class AdminInvoicePdfService(
    private val repo: AdminRepository,
    private val planRepo: AdminPlanRepository,
    private val subscriptionRepo: AdminSubscriptionRepository,
    private val platformIdentityRepo: AdminPlatformIdentityRepository,
    private val bucket: Bucket
) {
    data class GeneratedInvoicePdf(
        val fileName: String,
        val bytes: ByteArray
    )

    private val log = LoggerFactory.getLogger(AdminInvoicePdfService::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val currencyFormatter = NumberFormat.getInstance(Locale.FRANCE)

    companion object {
        private const val PLACEHOLDER = "« À compléter dans Paramètres plateforme »"

        /**
         * Caractères Unicode supplémentaires mappés par l'encodage WinAnsi (CP1252) :
         * Euro, guillemets typographiques, tiret long, etc. Référence :
         * https://pdfbox.apache.org/3.0/dependencies.html (Fonts → WinAnsiEncoding).
         */
        private val WIN_ANSI_EXTRA_CODE_POINTS: Set<Int> = setOf(
            0x20AC, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021, 0x02C6,
            0x2030, 0x0160, 0x2039, 0x0152, 0x017D, 0x2018, 0x2019, 0x201C,
            0x201D, 0x2022, 0x2013, 0x2014, 0x02DC, 0x2122, 0x0161, 0x203A,
            0x0153, 0x017E, 0x0178
        )
    }

    suspend fun generateInvoicePdf(invoiceId: String): GeneratedInvoicePdf? {
        val invoice = repo.getInvoiceById(invoiceId) ?: return null
        val groupe = repo.getGroupeById(invoice.groupeId) ?: return null
        val subscription = subscriptionRepo.getSubscriptionById(invoice.subscriptionId)
        val plan = planRepo.getPlanById(subscription?.planId?.ifBlank { groupe.planId } ?: groupe.planId)
        val identity = platformIdentityRepo.read()

        // Tentative de relecture du snapshot stocké dans le doc invoice (pour figer l'identité
        // plateforme telle qu'elle était au moment de l'émission). Fallback sur l'identité
        // courante si la facture est antérieure à l'introduction du snapshot.
        val snapshot = runCatching { loadInvoiceIdentitySnapshot(invoiceId) }.getOrNull()
        val effectiveIdentity = snapshot ?: identity

        val safeReference = invoice.reference.ifBlank { invoice.id }.replace(Regex("[^A-Za-z0-9._-]"), "-")

        val output = ByteArrayOutputStream()
        PDDocument().use { document ->
            val fonts = loadFonts(document)
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)

            val pageWidth = page.mediaBox.width
            val pageHeight = page.mediaBox.height
            val left = 52f
            val right = pageWidth - 52f
            val lineHeight = 14f

            PDPageContentStream(document, page).use { content ->
                val ctx = DrawContext(
                    document = document,
                    content = content,
                    fonts = fonts,
                    left = left,
                    right = right,
                    pageWidth = pageWidth,
                    lineHeight = lineHeight,
                    y = pageHeight - 48f
                )

                drawHeader(ctx, effectiveIdentity)
                drawInvoiceMeta(ctx, invoice)
                drawIssuerAndClient(ctx, effectiveIdentity, groupe)
                drawFinancialTable(ctx, invoice, plan?.nom ?: groupe.planId, effectiveIdentity)
                drawPaymentTerms(ctx, effectiveIdentity)
                drawSignatureZone(ctx)
                drawFooter(ctx, effectiveIdentity, pageHeight)
            }
            document.save(output)
        }
        return GeneratedInvoicePdf(fileName = "$safeReference.pdf", bytes = output.toByteArray())
    }

    // ── Identity snapshot lookup ─────────────────────────────────

    private suspend fun loadInvoiceIdentitySnapshot(invoiceId: String): PlatformIdentity? {
        val scope = bucket.defaultScope()
        val collections = listOf("invoices", "invoices_platform")
        for (name in collections) {
            val doc = runCatching {
                scope.collection(name).get(invoiceId).contentAs<Map<String, Any?>>()
            }.getOrNull() ?: continue
            @Suppress("UNCHECKED_CAST")
            val snap = doc["emitterIdentity"] as? Map<String, Any?> ?: continue
            return PlatformIdentity(
                raisonSociale = snap["raisonSociale"] as? String ?: "",
                rccm = snap["rccm"] as? String ?: "",
                niu = snap["niu"] as? String ?: "",
                siege = snap["siege"] as? String ?: "",
                city = snap["city"] as? String ?: "",
                country = snap["country"] as? String ?: "Congo",
                phone = snap["phone"] as? String ?: "",
                email = snap["email"] as? String ?: "",
                website = snap["website"] as? String ?: "",
                logoBase64 = "",
                tvaRate = (snap["tvaRate"] as? Number)?.toDouble() ?: 0.0,
                tvaExempted = snap["tvaExempted"] as? Boolean ?: true,
                paymentTerms = snap["paymentTerms"] as? String ?: "",
                competentCourt = snap["competentCourt"] as? String ?: "",
                iban = snap["iban"] as? String ?: "",
                bankName = snap["bankName"] as? String ?: "",
                legalMentions = snap["legalMentions"] as? String ?: ""
            )
        }
        return null
    }

    // ── Rendering ────────────────────────────────────────────────

    private class DrawContext(
        val document: PDDocument,
        val content: PDPageContentStream,
        val fonts: InvoiceFonts,
        val left: Float,
        val right: Float,
        val pageWidth: Float,
        val lineHeight: Float,
        var y: Float
    ) {
        val innerWidth: Float get() = right - left
    }

    private fun drawText(ctx: DrawContext, text: String, x: Float, y: Float, size: Float = 10.5f, bold: Boolean = false) {
        val safe = if (ctx.fonts.unicodeSafe) text else sanitizeForWinAnsi(text)
        ctx.content.beginText()
        ctx.content.setFont(if (bold) ctx.fonts.bold else ctx.fonts.regular, size)
        ctx.content.newLineAtOffset(x, y)
        ctx.content.showText(safe)
        ctx.content.endText()
    }

    private fun wrap(ctx: DrawContext, text: String, size: Float, maxWidth: Float, bold: Boolean = false): List<String> {
        val font: PDFont = if (bold) ctx.fonts.bold else ctx.fonts.regular
        val source = if (ctx.fonts.unicodeSafe) text else sanitizeForWinAnsi(text)
        if (source.isBlank()) return listOf("")
        val words = source.trim().split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isBlank()) word else "$current $word"
            val width = runCatching { font.getStringWidth(candidate) / 1000f * size }.getOrDefault(0f)
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

    private fun drawWrapped(ctx: DrawContext, text: String, x: Float, width: Float, size: Float = 10f, bold: Boolean = false) {
        wrap(ctx, text, size, width, bold).forEach { line ->
            drawText(ctx, line, x, ctx.y, size, bold)
            ctx.y -= ctx.lineHeight
        }
    }

    private fun divider(ctx: DrawContext) {
        ctx.content.setLineWidth(0.4f)
        ctx.content.moveTo(ctx.left, ctx.y)
        ctx.content.lineTo(ctx.right, ctx.y)
        ctx.content.stroke()
        ctx.y -= 10f
    }

    private fun field(value: String): String = value.trim().ifEmpty { PLACEHOLDER }

    private fun drawHeader(ctx: DrawContext, identity: PlatformIdentity) {
        val headerTop = ctx.y
        val logoDrawn = drawLogoIfPresent(ctx, identity.logoBase64, ctx.left, headerTop - 56f, 70f)
        val textX = if (logoDrawn) ctx.left + 80f else ctx.left

        drawText(ctx, field(identity.raisonSociale).uppercase(), textX, headerTop - 4f, 16f, bold = true)
        drawText(ctx, "Plateforme de gestion scolaire", textX, headerTop - 22f, 9.5f)

        val metaX = ctx.right - 160f
        drawText(ctx, "FACTURE OFFICIELLE", metaX, headerTop - 4f, 13f, bold = true)
        drawText(ctx, "Document contractuel", metaX, headerTop - 22f, 9f)

        ctx.y = headerTop - 66f
        divider(ctx)
    }

    private fun drawLogoIfPresent(ctx: DrawContext, base64: String, x: Float, y: Float, maxSize: Float): Boolean {
        if (base64.isBlank()) return false
        return runCatching {
            val cleaned = base64.substringAfter(",", base64)
            val bytes = Base64.getDecoder().decode(cleaned)
            val image = PDImageXObject.createFromByteArray(ctx.document, bytes, "logo")
            val ratio = image.width.toFloat() / image.height.toFloat()
            val width: Float
            val height: Float
            if (ratio >= 1f) {
                width = maxSize
                height = maxSize / ratio
            } else {
                height = maxSize
                width = maxSize * ratio
            }
            ctx.content.drawImage(image, x, y, width, height)
            true
        }.getOrElse {
            log.warn("Logo plateforme invalide (base64 décodage/création image) : {}", it.message)
            false
        }
    }

    private fun drawInvoiceMeta(ctx: DrawContext, invoice: InvoiceResponse) {
        val left = ctx.left
        val right = ctx.right
        val topY = ctx.y
        drawText(ctx, "Numéro de facture", left, topY, 9f, bold = true)
        drawText(ctx, invoice.reference.ifBlank { "—" }, left, topY - 13f, 11f, bold = true)

        drawText(ctx, "Date d'émission", left + 180f, topY, 9f, bold = true)
        drawText(ctx, formatDate(invoice.dateEmission), left + 180f, topY - 13f, 10.5f)

        drawText(ctx, "Date d'échéance", left + 320f, topY, 9f, bold = true)
        drawText(ctx, formatDate(invoice.dateEcheance), left + 320f, topY - 13f, 10.5f)

        drawText(ctx, "Statut", right - 80f, topY, 9f, bold = true)
        drawText(ctx, invoice.statut.uppercase(), right - 80f, topY - 13f, 10.5f, bold = true)

        ctx.y = topY - 34f
        divider(ctx)
    }

    private fun drawIssuerAndClient(ctx: DrawContext, identity: PlatformIdentity, groupe: GroupeResponse) {
        val colWidth = (ctx.innerWidth - 20f) / 2f
        val leftX = ctx.left
        val rightX = ctx.left + colWidth + 20f
        val topY = ctx.y

        // Émetteur
        drawText(ctx, "ÉMETTEUR", leftX, topY, 10f, bold = true)
        ctx.y = topY - ctx.lineHeight
        drawWrapped(ctx, field(identity.raisonSociale), leftX, colWidth, 10.5f, bold = true)
        drawWrapped(ctx, "RCCM : ${field(identity.rccm)}", leftX, colWidth, 9.5f)
        drawWrapped(ctx, "NIU : ${field(identity.niu)}", leftX, colWidth, 9.5f)
        drawWrapped(
            ctx,
            "Siège : ${listOfNotNull(identity.siege.takeIf { it.isNotBlank() }, identity.city.takeIf { it.isNotBlank() }, identity.country.takeIf { it.isNotBlank() }).joinToString(", ").ifBlank { PLACEHOLDER }}",
            leftX,
            colWidth,
            9.5f
        )
        drawWrapped(ctx, "Téléphone : ${field(identity.phone)}", leftX, colWidth, 9.5f)
        drawWrapped(ctx, "Email : ${field(identity.email)}", leftX, colWidth, 9.5f)
        if (identity.website.isNotBlank()) {
            drawWrapped(ctx, "Web : ${identity.website}", leftX, colWidth, 9.5f)
        }
        val issuerEndY = ctx.y

        // Client
        ctx.y = topY
        drawText(ctx, "CLIENT (GROUPE SCOLAIRE)", rightX, topY, 10f, bold = true)
        ctx.y = topY - ctx.lineHeight
        drawWrapped(ctx, groupe.nom.ifBlank { "—" }, rightX, colWidth, 10.5f, bold = true)
        val address = listOfNotNull(groupe.address, groupe.city, groupe.department, groupe.country).joinToString(", ")
        drawWrapped(ctx, "Adresse : ${address.ifBlank { "—" }}", rightX, colWidth, 9.5f)
        drawWrapped(ctx, "Téléphone : ${groupe.phone?.ifBlank { null } ?: "—"}", rightX, colWidth, 9.5f)
        drawWrapped(ctx, "Email : ${groupe.email?.ifBlank { null } ?: "—"}", rightX, colWidth, 9.5f)
        drawWrapped(ctx, "Slug : ${groupe.slug.ifBlank { groupe.id }}", rightX, colWidth, 9.5f)
        val clientEndY = ctx.y

        ctx.y = minOf(issuerEndY, clientEndY) - 2f
        divider(ctx)
    }

    private fun drawFinancialTable(
        ctx: DrawContext,
        invoice: InvoiceResponse,
        planName: String,
        identity: PlatformIdentity
    ) {
        drawText(ctx, "DÉTAIL DES PRESTATIONS", ctx.left, ctx.y, 10f, bold = true)
        ctx.y -= ctx.lineHeight + 4f

        // Colonnes : Description | Qté | PU HT | Total HT
        val descX = ctx.left
        val qtyX = ctx.right - 260f
        val puX = ctx.right - 180f
        val totalX = ctx.right - 70f

        val headerY = ctx.y
        ctx.content.setNonStrokingColor(0.92f, 0.94f, 0.97f)
        ctx.content.addRect(ctx.left, headerY - 4f, ctx.innerWidth, 20f)
        ctx.content.fill()
        ctx.content.setNonStrokingColor(0f, 0f, 0f)
        drawText(ctx, "Description", descX + 4f, headerY + 4f, 9.5f, bold = true)
        drawText(ctx, "Qté", qtyX, headerY + 4f, 9.5f, bold = true)
        drawText(ctx, "PU HT", puX, headerY + 4f, 9.5f, bold = true)
        drawText(ctx, "Total HT", totalX, headerY + 4f, 9.5f, bold = true)
        ctx.y = headerY - 20f

        // Calcul HT / TVA / TTC
        val tvaRate = if (identity.tvaExempted) 0.0 else identity.tvaRate.coerceAtLeast(0.0)
        val ttc = invoice.montantXAF
        val ht = if (tvaRate > 0.0) (ttc / (1.0 + tvaRate / 100.0)).toLong() else ttc
        val tva = ttc - ht

        val descLines = wrap(
            ctx,
            "Abonnement plateforme E-PILOTE CONGO — Plan « $planName »",
            10f,
            qtyX - descX - 8f
        )
        val rowStartY = ctx.y
        descLines.forEachIndexed { idx, line ->
            drawText(ctx, line, descX + 4f, ctx.y, 10f)
            if (idx == 0) {
                drawText(ctx, "1", qtyX, ctx.y, 10f)
                drawText(ctx, formatMoney(ht), puX, ctx.y, 10f)
                drawText(ctx, formatMoney(ht), totalX, ctx.y, 10f)
            }
            ctx.y -= ctx.lineHeight
        }
        ctx.y = minOf(ctx.y, rowStartY - ctx.lineHeight)
        ctx.y -= 6f
        divider(ctx)

        // Totaux
        val totalsX = ctx.right - 220f
        val amountX = ctx.right - 10f - 80f
        drawText(ctx, "Total HT", totalsX, ctx.y, 10f)
        drawText(ctx, formatMoney(ht), amountX, ctx.y, 10f)
        ctx.y -= ctx.lineHeight

        val tvaLabel = if (identity.tvaExempted) "TVA (exonérée)" else "TVA (${formatPercent(tvaRate)})"
        drawText(ctx, tvaLabel, totalsX, ctx.y, 10f)
        drawText(ctx, formatMoney(tva), amountX, ctx.y, 10f)
        ctx.y -= ctx.lineHeight + 2f

        ctx.content.setLineWidth(0.6f)
        ctx.content.moveTo(totalsX - 4f, ctx.y + ctx.lineHeight - 2f)
        ctx.content.lineTo(ctx.right, ctx.y + ctx.lineHeight - 2f)
        ctx.content.stroke()

        drawText(ctx, "Total TTC", totalsX, ctx.y, 11f, bold = true)
        drawText(ctx, formatMoney(ttc), amountX, ctx.y, 11.5f, bold = true)
        ctx.y -= ctx.lineHeight + 8f

        if (invoice.notes.isNotBlank()) {
            drawText(ctx, "Notes", ctx.left, ctx.y, 9.5f, bold = true)
            ctx.y -= ctx.lineHeight
            drawWrapped(ctx, invoice.notes, ctx.left, ctx.innerWidth, 9.5f)
        }
        divider(ctx)
    }

    private fun drawPaymentTerms(ctx: DrawContext, identity: PlatformIdentity) {
        drawText(ctx, "CONDITIONS DE PAIEMENT", ctx.left, ctx.y, 10f, bold = true)
        ctx.y -= ctx.lineHeight
        drawWrapped(ctx, field(identity.paymentTerms), ctx.left, ctx.innerWidth, 9.5f)
        val bankLine = buildString {
            if (identity.bankName.isNotBlank()) append("Banque : ${identity.bankName}")
            if (identity.iban.isNotBlank()) {
                if (isNotEmpty()) append(" — ")
                append("IBAN : ${identity.iban}")
            }
        }
        if (bankLine.isNotBlank()) {
            drawWrapped(ctx, bankLine, ctx.left, ctx.innerWidth, 9.5f)
        }
        val momoLine = buildString {
            val momos = mutableListOf<String>()
            if (identity.mtnMomoNumber.isNotBlank()) momos += "MTN MoMo : ${identity.mtnMomoNumber}"
            if (identity.airtelMoneyNumber.isNotBlank()) momos += "Airtel Money : ${identity.airtelMoneyNumber}"
            append(momos.joinToString(" — "))
        }
        if (momoLine.isNotBlank()) {
            drawWrapped(ctx, momoLine, ctx.left, ctx.innerWidth, 9.5f)
        }
        divider(ctx)
    }

    private fun drawSignatureZone(ctx: DrawContext) {
        val boxWidth = (ctx.innerWidth - 30f) / 2f
        val boxHeight = 60f
        val leftX = ctx.left
        val rightX = ctx.left + boxWidth + 30f
        val topY = ctx.y

        drawText(ctx, "Signature & cachet émetteur", leftX, topY, 9.5f, bold = true)
        drawText(ctx, "Signature & cachet client", rightX, topY, 9.5f, bold = true)
        ctx.content.setLineWidth(0.4f)
        ctx.content.addRect(leftX, topY - boxHeight - 4f, boxWidth, boxHeight)
        ctx.content.addRect(rightX, topY - boxHeight - 4f, boxWidth, boxHeight)
        ctx.content.stroke()
        ctx.y = topY - boxHeight - 14f
    }

    private fun drawFooter(ctx: DrawContext, identity: PlatformIdentity, pageHeight: Float) {
        val footerY = 48f
        ctx.content.setLineWidth(0.3f)
        ctx.content.moveTo(ctx.left, footerY + 28f)
        ctx.content.lineTo(ctx.right, footerY + 28f)
        ctx.content.stroke()

        val parts = mutableListOf<String>()
        if (identity.raisonSociale.isNotBlank()) parts += identity.raisonSociale
        if (identity.rccm.isNotBlank()) parts += "RCCM ${identity.rccm}"
        if (identity.niu.isNotBlank()) parts += "NIU ${identity.niu}"
        val header = if (parts.isEmpty()) PLACEHOLDER else parts.joinToString(" — ")
        drawText(ctx, header, ctx.left, footerY + 16f, 8.5f, bold = true)

        val legal = identity.legalMentions.ifBlank {
            "Facture émise par la plateforme E-PILOTE CONGO. Document contractuel opposable, généré électroniquement et archivé côté serveur."
        }
        val line1 = wrap(ctx, legal, 8f, ctx.innerWidth).firstOrNull() ?: legal
        drawText(ctx, line1, ctx.left, footerY + 4f, 8f)
        if (identity.competentCourt.isNotBlank()) {
            drawText(ctx, "Tribunal compétent : ${identity.competentCourt}", ctx.left, footerY - 8f, 8f)
        }
    }

    // ── Fonts & sanitisation ─────────────────────────────────────

    private data class InvoiceFonts(
        val regular: PDFont,
        val bold: PDFont,
        /** true si les polices supportent UTF-8 (TTF embarquée), false si fallback Helvetica WinAnsi. */
        val unicodeSafe: Boolean
    )

    private fun loadFonts(document: PDDocument): InvoiceFonts {
        return runCatching {
            val regular = ClassPathResource("fonts/DejaVuSans.ttf").inputStream.use {
                PDType0Font.load(document, it, true)
            }
            val bold = ClassPathResource("fonts/DejaVuSans-Bold.ttf").inputStream.use {
                PDType0Font.load(document, it, true)
            }
            InvoiceFonts(regular, bold, unicodeSafe = true)
        }.getOrElse { error ->
            log.warn(
                "DejaVu TTF indisponible pour la facture, fallback Helvetica Standard-14 (rendu UTF-8 partiel) : {}",
                error.message
            )
            InvoiceFonts(
                regular = PDType1Font(Standard14Fonts.FontName.HELVETICA),
                bold = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD),
                unicodeSafe = false
            )
        }
    }

    private fun sanitizeForWinAnsi(text: String): String {
        if (text.isEmpty()) return text
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
        val builder = StringBuilder(normalized.length)
        normalized.forEach { ch ->
            builder.append(if (isWinAnsiCompatible(ch)) ch else '?')
        }
        return builder.toString()
    }

    private fun isWinAnsiCompatible(ch: Char): Boolean {
        if (ch == '\n' || ch == '\r' || ch == '\t') return false
        val code = ch.code
        if (code in 0x20..0x7E) return true
        if (code in 0xA0..0xFF) return true
        return code in WIN_ANSI_EXTRA_CODE_POINTS
    }

    // ── Formatters ────────────────────────────────────────────────

    private fun formatDate(epochMillis: Long): String = runCatching {
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of("UTC")).toLocalDate().format(dateFormatter)
    }.getOrDefault("-")

    private fun formatMoney(amount: Long): String = "${currencyFormatter.format(amount)} XAF"

    private fun formatPercent(rate: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.FRANCE).apply {
            maximumFractionDigits = 2
        }
        return "${formatter.format(rate)} %"
    }
}
