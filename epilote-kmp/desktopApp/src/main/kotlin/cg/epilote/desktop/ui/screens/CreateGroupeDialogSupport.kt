package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private const val MAX_LOGO_SIZE_BYTES = 2 * 1024 * 1024
private val ACCEPTED_LOGO_EXTENSIONS = arrayOf("png", "jpg", "jpeg", "webp")

internal data class GroupePlanOption(
    val id: String,
    val label: String,
    val details: String,
    val color: Color
)

internal sealed interface LogoSelectionResult {
    data class Selected(val dataUrl: String, val fileName: String) : LogoSelectionResult
    data class Failure(val message: String) : LogoSelectionResult
    data object Cancelled : LogoSelectionResult
}

@Composable
internal fun GroupeSectionCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE6ECF4))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1D3557).copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = Color(0xFF1D3557), modifier = Modifier.size(18.dp))
                    }
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF1D3557))
                }
                content()
            }
        )
    }
}

@Composable
internal fun GroupeSelectionField(
    value: String,
    label: String,
    placeholder: String,
    expanded: Boolean,
    enabled: Boolean,
    options: List<String>,
    onExpandedChange: (Boolean) -> Unit,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .then(if (enabled) Modifier.cursorHand() else Modifier)
                .clickable(enabled = enabled) { onExpandedChange(true) },
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = {
                IconButton(onClick = { if (enabled) onExpandedChange(true) }, modifier = Modifier.cursorHand()) {
                    Icon(Icons.Default.ArrowDropDown, null)
                }
            },
            supportingText = {
                supportingText?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = EpiloteTextMuted)
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
internal fun PlanSelector(
    options: List<GroupePlanOption>,
    selectedPlanId: String,
    onPlanSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val selected = option.id == selectedPlanId
            Surface(
                onClick = { onPlanSelected(option.id) },
                modifier = Modifier.fillMaxWidth().cursorHand(),
                shape = RoundedCornerShape(14.dp),
                color = if (selected) option.color.copy(alpha = 0.10f) else Color(0xFFF8FAFD),
                border = if (selected) {
                    androidx.compose.foundation.BorderStroke(2.dp, option.color)
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(option.color, RoundedCornerShape(999.dp))
                    )
                    Text(
                        option.label,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (selected) option.color else Color(0xFF1D3557)
                    )
                    Text(
                        "—  ${option.details}",
                        fontSize = 12.sp,
                        color = EpiloteTextMuted
                    )
                }
            }
        }
    }
}

internal fun pickLogoSelection(): LogoSelectionResult {
    val chooser = JFileChooser().apply {
        dialogTitle = "Sélectionner le logo du groupe"
        fileFilter = FileNameExtensionFilter("Images", *ACCEPTED_LOGO_EXTENSIONS)
        isAcceptAllFileFilterUsed = false
        isMultiSelectionEnabled = false
    }

    val selection = chooser.showOpenDialog(null)
    if (selection != JFileChooser.APPROVE_OPTION) {
        return LogoSelectionResult.Cancelled
    }

    val file = chooser.selectedFile ?: return LogoSelectionResult.Cancelled
    return prepareLogoSelection(file)
}
