package cg.epilote.android.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EpiloteGreen  = Color(0xFF1B5E20)
val EpiloteYellow = Color(0xFFF9A825)
val EpiloteRed    = Color(0xFFC62828)
val EpiloteBlue   = Color(0xFF0D47A1)
val EpiloteSurface = Color(0xFFF5F5F5)

private val LightColorScheme = lightColorScheme(
    primary        = EpiloteBlue,
    onPrimary      = Color.White,
    primaryContainer   = Color(0xFFBBDEFB),
    secondary      = EpiloteGreen,
    onSecondary    = Color.White,
    background     = Color(0xFFFAFAFA),
    surface        = EpiloteSurface,
    error          = EpiloteRed,
    onError        = Color.White
)

@Composable
fun EpiloteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography(),
        content     = content
    )
}
