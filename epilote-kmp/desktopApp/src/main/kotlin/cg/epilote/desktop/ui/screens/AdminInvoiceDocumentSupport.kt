package cg.epilote.desktop.ui.screens

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

internal data class InvoiceDocumentActionResult(
    val message: String,
    val isError: Boolean = false
)

internal fun exportInvoicePdf(invoice: InvoiceDto, pdfBytes: ByteArray): InvoiceDocumentActionResult {
    val chooser = JFileChooser().apply {
        dialogTitle = "Exporter la facture PDF"
        fileFilter = FileNameExtensionFilter("Documents PDF", "pdf")
        isAcceptAllFileFilterUsed = false
        isMultiSelectionEnabled = false
        selectedFile = File(invoicePdfFileName(invoice))
    }
    val selection = chooser.showSaveDialog(null)
    if (selection != JFileChooser.APPROVE_OPTION) {
        return InvoiceDocumentActionResult("Export de la facture annulé", isError = true)
    }

    val target = ensurePdfExtension(chooser.selectedFile ?: return InvoiceDocumentActionResult("Aucun fichier sélectionné", isError = true))
    return runCatching {
        target.writeBytes(pdfBytes)
        InvoiceDocumentActionResult("Facture exportée vers ${target.absolutePath}")
    }.getOrElse { error ->
        InvoiceDocumentActionResult("Impossible d'exporter la facture : ${error.message}", isError = true)
    }
}

internal fun openInvoicePdf(invoice: InvoiceDto, pdfBytes: ByteArray): InvoiceDocumentActionResult {
    val file = runCatching { createTempInvoicePdf(invoice, pdfBytes) }.getOrElse { error ->
        return InvoiceDocumentActionResult("Impossible de préparer le PDF : ${error.message}", isError = true)
    }
    desktopOrNull(Desktop.Action.OPEN)?.let { desktop ->
        return runCatching {
            desktop.open(file)
            InvoiceDocumentActionResult("Prévisualisation PDF ouverte")
        }.getOrElse { error ->
            openInvoicePdfWithSystemCommand(file).getOrElse {
                InvoiceDocumentActionResult("Impossible d'ouvrir le PDF : ${error.message}", isError = true)
            }
        }
    }
    return openInvoicePdfWithSystemCommand(file).getOrElse {
        InvoiceDocumentActionResult("Ouverture système indisponible sur ce poste", isError = true)
    }
}

internal fun shareInvoicePdf(invoice: InvoiceDto, pdfBytes: ByteArray): InvoiceDocumentActionResult {
    val file = runCatching { createTempInvoicePdf(invoice, pdfBytes) }.getOrElse { error ->
        return InvoiceDocumentActionResult("Impossible de préparer le PDF : ${error.message}", isError = true)
    }
    copyPathToClipboard(file.absolutePath)
    val desktop = desktopOrNull()
    if (desktop?.isSupported(Desktop.Action.MAIL) == true) {
        val subject = encodeMail("Facture plateforme ${invoice.reference.ifBlank { invoice.id }}")
        val body = encodeMail(
            "Bonjour,%0A%0AVeuillez trouver la facture plateforme du groupe ${invoice.groupeNom}.%0A%0ARéférence : ${invoice.reference.ifBlank { invoice.id }}%0AEmplacement local du PDF : ${file.absolutePath}%0A"
                .replace("%0A", "\n")
        )
        return runCatching {
            desktop.mail(URI("mailto:?subject=$subject&body=$body"))
            InvoiceDocumentActionResult("Partage préparé : chemin du PDF copié et client mail ouvert")
        }.getOrElse { error ->
            InvoiceDocumentActionResult("PDF préparé, mais impossible d'ouvrir le client mail : ${error.message}", isError = true)
        }
    }
    return InvoiceDocumentActionResult("PDF préparé et chemin copié : ${file.absolutePath}")
}

private fun createTempInvoicePdf(invoice: InvoiceDto, pdfBytes: ByteArray): File {
    val file = Files.createTempFile(invoicePdfBaseName(invoice), ".pdf").toFile()
    file.writeBytes(pdfBytes)
    file.deleteOnExit()
    return file
}

private fun invoicePdfFileName(invoice: InvoiceDto): String = "${invoicePdfBaseName(invoice)}.pdf"

private fun invoicePdfBaseName(invoice: InvoiceDto): String {
    val raw = invoice.reference.ifBlank { invoice.id }
    val sanitized = raw.replace(Regex("[^A-Za-z0-9._-]"), "-").trim('-').ifBlank { "facture-epilote" }
    return if (sanitized.length >= 3) sanitized else "facture-$sanitized"
}

private fun ensurePdfExtension(file: File): File =
    if (file.name.lowercase().endsWith(".pdf")) file else File(file.parentFile, "${file.name}.pdf")

private fun desktopOrNull(action: Desktop.Action? = null): Desktop? =
    if (!Desktop.isDesktopSupported()) {
        null
    } else {
        Desktop.getDesktop().takeIf { desktop -> action == null || desktop.isSupported(action) }
    }

private fun openInvoicePdfWithSystemCommand(file: File): Result<InvoiceDocumentActionResult> = runCatching {
    val osName = System.getProperty("os.name").orEmpty().lowercase()
    val command = when {
        "linux" in osName -> listOf("xdg-open", file.absolutePath)
        "mac" in osName -> listOf("open", file.absolutePath)
        "windows" in osName -> listOf("rundll32", "url.dll,FileProtocolHandler", file.absolutePath)
        else -> error("Aucune commande système disponible")
    }
    ProcessBuilder(command).start()
    InvoiceDocumentActionResult("Prévisualisation PDF lancée")
}

private fun copyPathToClipboard(value: String) {
    runCatching {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
    }
}

private fun encodeMail(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
