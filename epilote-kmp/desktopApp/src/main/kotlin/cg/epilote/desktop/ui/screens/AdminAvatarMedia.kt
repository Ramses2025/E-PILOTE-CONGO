package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun AdminAvatar(
    avatarData: String?,
    firstName: String,
    lastName: String,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    backgroundColor: Color = Color(0xFF1D3557).copy(alpha = 0.10f),
    contentColor: Color = Color(0xFF1D3557)
) {
    val avatarBitmap = rememberLogoBitmap(avatarData)
    val initials = buildString {
        firstName.firstOrNull()?.let { append(it.uppercaseChar()) }
        lastName.firstOrNull()?.let { append(it.uppercaseChar()) }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (avatarBitmap != null) {
            Image(
                bitmap = avatarBitmap,
                contentDescription = "Photo de profil",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (initials.isNotBlank()) {
            Text(
                text = initials,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
