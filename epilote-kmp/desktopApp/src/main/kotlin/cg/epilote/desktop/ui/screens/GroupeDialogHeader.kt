package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand

@Composable
internal fun GroupeDialogHeader(onDismiss: () -> Unit, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(Color(0xFF1D3557).copy(alpha = 0.08f), RoundedCornerShape(10.dp)), Alignment.Center) {
                Icon(Icons.Default.Business, null, tint = Color(0xFF1D3557), modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D3557))
                Text(subtitle, fontSize = 12.sp, color = EpiloteTextMuted)
            }
        }
        IconButton(onClick = onDismiss, modifier = Modifier.cursorHand()) {
            Icon(Icons.Default.Close, "Fermer")
        }
    }
    HorizontalDivider(color = Color(0xFFE9EEF5))
}
