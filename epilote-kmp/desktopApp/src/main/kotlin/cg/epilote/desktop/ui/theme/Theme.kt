package cg.epilote.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EpiloteGreen      = Color(0xFF00875A)
val EpiloteGreenDark  = Color(0xFF006644)
val EpiloteGreenLight = Color(0xFFE3F7EF)
val EpiloteOrange     = Color(0xFFFF8B00)
val EpiloteRed        = Color(0xFFDE350B)
val EpiloteSurface    = Color(0xFFF4F5F7)
val EpiloteSidebar    = Color(0xFF1A2332)
val EpiloteSidebarSelected = Color(0xFF243447)
val EpiloteTextOnDark = Color(0xFFE8EDF2)
val EpiloteTextMuted  = Color(0xFF8C9BAB)

private val LightColors = lightColorScheme(
    primary          = EpiloteGreen,
    onPrimary        = Color.White,
    primaryContainer = EpiloteGreenLight,
    secondary        = EpiloteOrange,
    background       = EpiloteSurface,
    surface          = Color.White,
    error            = EpiloteRed,
    onBackground     = Color(0xFF172B4D),
    onSurface        = Color(0xFF172B4D),
)

@Composable
fun EpiloteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
