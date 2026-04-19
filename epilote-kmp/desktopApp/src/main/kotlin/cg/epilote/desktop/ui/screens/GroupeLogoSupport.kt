package cg.epilote.desktop.ui.screens

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import org.jetbrains.skia.Image as SkiaImage

private const val MAX_LOGO_SIZE_BYTES = 2 * 1024 * 1024
private val ACCEPTED_LOGO_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")

internal fun prepareLogoSelection(file: File): LogoSelectionResult {
    val extension = file.extension.lowercase()
    if (extension !in ACCEPTED_LOGO_EXTENSIONS) {
        return LogoSelectionResult.Failure("Format non supporté. Utilisez PNG, JPG ou WEBP.")
    }

    val bytes = runCatching { file.readBytes() }
        .getOrElse { return LogoSelectionResult.Failure("Impossible de lire le fichier sélectionné.") }

    if (bytes.isEmpty()) {
        return LogoSelectionResult.Failure("Le fichier sélectionné est vide.")
    }

    val squareBytes = cropSquareImage(bytes, extension)
        ?: return LogoSelectionResult.Failure("Le fichier sélectionné n'est pas une image exploitable.")

    if (squareBytes.size > MAX_LOGO_SIZE_BYTES) {
        return LogoSelectionResult.Failure("Le logo doit faire 2 Mo maximum pour rester léger en base.")
    }

    val mimeType = if (extension == "png") "image/png" else "image/jpeg"
    val decodable = runCatching { SkiaImage.makeFromEncoded(squareBytes) }.isSuccess
    if (!decodable) {
        return LogoSelectionResult.Failure("Le fichier sélectionné n'est pas une image exploitable.")
    }

    val dataUrl = "data:$mimeType;base64,${Base64.getEncoder().encodeToString(squareBytes)}"
    return LogoSelectionResult.Selected(dataUrl = dataUrl, fileName = file.name)
}

private fun cropSquareImage(bytes: ByteArray, extension: String): ByteArray? {
    val source = ImageIO.read(bytes.inputStream()) ?: return null
    val size = minOf(source.width, source.height)
    val x = (source.width - size) / 2
    val y = (source.height - size) / 2
    val square = source.getSubimage(x, y, size, size)
    val outputImage = BufferedImage(size, size, if (extension == "png") BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB)
    val graphics = outputImage.createGraphics()
    graphics.drawImage(square, 0, 0, null)
    graphics.dispose()

    return ByteArrayOutputStream().use { output ->
        val format = if (extension == "png") "png" else "jpg"
        if (!ImageIO.write(outputImage, format, output)) return null
        output.toByteArray()
    }
}
