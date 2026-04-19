package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.Icon
import java.util.Base64
import org.jetbrains.skia.Image as SkiaImage

@Composable
internal fun GroupeLogoAvatar(
    logoData: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundColor: Color = Color(0xFF1D3557).copy(alpha = 0.08f),
    iconTint: Color = Color(0xFF1D3557)
) {
    val logoBitmap = rememberLogoBitmap(logoData)

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (logoBitmap != null) {
            Image(
                bitmap = logoBitmap,
                contentDescription = "Logo du groupe",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(Icons.Default.Business, null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
internal fun rememberLogoBitmap(logoData: String?): ImageBitmap? = remember(logoData) {
    logoData
        ?.takeIf { it.isNotBlank() }
        ?.let { encodedLogo ->
            runCatching {
                val payload = encodedLogo.substringAfter("base64,", encodedLogo)
                SkiaImage.makeFromEncoded(Base64.getDecoder().decode(payload)).toComposeImageBitmap()
            }.getOrNull()
        }
}
