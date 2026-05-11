package cg.epilote.desktop.ui.screens

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Exporte des données au format CSV (séparateur ;, encodage UTF-8 BOM)
 * dans le dossier Bureau (Desktop) de l'utilisateur, ou à défaut dans le
 * répertoire home.
 *
 * Retourne le chemin absolu du fichier créé, ou null en cas d'erreur.
 */
internal fun exportCsvToDesktop(
    baseName: String,
    headers: List<String>,
    rows: List<List<String>>
): String? = runCatching {
    val desktop = File(System.getProperty("user.home"), "Desktop")
    val dir = if (desktop.exists() && desktop.isDirectory) desktop else File(System.getProperty("user.home"))
    val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val file = File(dir, "${baseName}_$ts.csv")
    file.writeText(
        buildString {
            append("\uFEFF") // UTF-8 BOM pour Excel
            appendLine(headers.joinToString(";") { "\"${it.replace("\"", "\"\"")}\"" })
            rows.forEach { row ->
                appendLine(row.joinToString(";") { "\"${it.replace("\"", "\"\"")}\"" })
            }
        },
        Charsets.UTF_8
    )
    file.absolutePath
}.getOrNull()
