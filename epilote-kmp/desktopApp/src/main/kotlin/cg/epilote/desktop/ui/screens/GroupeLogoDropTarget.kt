package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import java.awt.Color as AwtColor
import java.awt.Font
import java.awt.GridBagLayout
import java.awt.datatransfer.DataFlavor
import java.io.File
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.TransferHandler

@Composable
internal fun GroupeLogoDropTarget(
    modifier: Modifier = Modifier,
    onLogoSelected: (String, String) -> Unit,
    onLogoSelectionError: (String) -> Unit
) {
    SwingPanel(
        modifier = modifier.fillMaxWidth().height(86.dp),
        factory = {
            buildLogoDropPanel(onLogoSelected, onLogoSelectionError)
        }
    )
}

private fun buildLogoDropPanel(
    onLogoSelected: (String, String) -> Unit,
    onLogoSelectionError: (String) -> Unit
): JPanel {
    val label = JLabel(
        "<html><div style='text-align:center;'>Déposez le logo ici<br/><span style='font-size:10px;'>Le fichier sera recadré au format carré automatiquement</span></div></html>",
        SwingConstants.CENTER
    ).apply {
        foreground = AwtColor(0x5B, 0x6B, 0x83)
        font = Font("SansSerif", Font.PLAIN, 12)
    }

    return JPanel(GridBagLayout()).apply {
        isOpaque = true
        background = AwtColor.WHITE
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createDashedBorder(AwtColor(0xC9, 0xD7, 0xE8), 6f, 6f),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        )
        add(label)
        transferHandler = object : TransferHandler() {
            override fun canImport(support: TransferSupport): Boolean {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
            }

            override fun importData(support: TransferSupport): Boolean {
                if (!canImport(support)) return false
                val file = extractDroppedFile(support) ?: return false
                return when (val result = prepareLogoSelection(file)) {
                    is LogoSelectionResult.Selected -> {
                        onLogoSelected(result.dataUrl, result.fileName)
                        true
                    }
                    is LogoSelectionResult.Failure -> {
                        onLogoSelectionError(result.message)
                        false
                    }
                    LogoSelectionResult.Cancelled -> false
                }
            }
        }
    }
}

private fun extractDroppedFile(support: TransferHandler.TransferSupport): File? {
    val files = runCatching {
        support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>
    }.getOrNull().orEmpty()
    return files.filterIsInstance<File>().firstOrNull()
}
