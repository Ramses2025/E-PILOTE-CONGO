package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand

@Composable
internal fun GroupeLogoPicker(
    logoData: String?,
    logoFileName: String,
    errorMessage: String?,
    onLogoSelected: (String, String) -> Unit,
    onLogoSelectionError: (String) -> Unit,
    onLogoCleared: () -> Unit
) {
    var circularPreview by remember(logoData) { mutableStateOf(false) }
    val previewShape = if (circularPreview) CircleShape else RoundedCornerShape(18.dp)
    val borderColor = if (errorMessage != null) MaterialTheme.colorScheme.error else Color(0xFFD6E0EE)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF8FAFD),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1D3557).copy(alpha = 0.08f)
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                null,
                                tint = Color(0xFF1D3557),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Identité visuelle", fontSize = 14.sp, color = Color(0xFF1D3557))
                            Text("PNG, JPG ou WEBP, 2 Mo max", fontSize = 12.sp, color = EpiloteTextMuted)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Aperçu :", fontSize = 12.sp, color = EpiloteTextMuted)
                FilterChip(
                    selected = !circularPreview,
                    onClick = { circularPreview = false },
                    modifier = Modifier.cursorHand(),
                    label = { Text("Carré", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.CropSquare, null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1D3557).copy(alpha = 0.12f),
                        selectedLabelColor = Color(0xFF1D3557)
                    )
                )
                FilterChip(
                    selected = circularPreview,
                    onClick = { circularPreview = true },
                    modifier = Modifier.cursorHand(),
                    label = { Text("Circulaire", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Circle, null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1D3557).copy(alpha = 0.12f),
                        selectedLabelColor = Color(0xFF1D3557)
                    )
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = previewShape,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                if (logoData.isNullOrBlank()) {
                    GroupeLogoDropTarget(
                        onLogoSelected = onLogoSelected,
                        onLogoSelectionError = onLogoSelectionError
                    )
                } else {
                    GroupeLogoAvatar(
                        logoData = logoData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        shape = previewShape,
                        backgroundColor = Color(0xFFF8FAFD)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        when (val result = pickLogoSelection()) {
                            is LogoSelectionResult.Selected -> onLogoSelected(result.dataUrl, result.fileName)
                            is LogoSelectionResult.Failure -> onLogoSelectionError(result.message)
                            LogoSelectionResult.Cancelled -> Unit
                        }
                    },
                    modifier = Modifier.cursorHand(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEAF1FB), contentColor = Color(0xFF1D3557))
                ) {
                    Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (logoData.isNullOrBlank()) "Choisir un logo" else "Remplacer")
                }
                if (!logoData.isNullOrBlank()) {
                    TextButton(onClick = onLogoCleared, modifier = Modifier.cursorHand()) {
                        Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Retirer")
                    }
                }
            }

            when {
                errorMessage != null -> Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                logoFileName.isNotBlank() -> Text(logoFileName, fontSize = 12.sp, color = EpiloteTextMuted)
                else -> Text("Le logo est optionnel mais recommandé. Les images déposées ou choisies sont recadrées au carré automatiquement.", fontSize = 12.sp, color = EpiloteTextMuted)
            }
        }
    }
}
