package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.ProfilApiDto
import cg.epilote.desktop.data.UserApiDto
import cg.epilote.desktop.ui.theme.cursorHand

@Composable
internal fun UserRow(
    user: UserApiDto,
    profils: List<ProfilApiDto>,
    rowBg: Color,
    onAssignProfil: (String) -> Unit
) {
    var showProfilMenu by remember { mutableStateOf(false) }
    val initiales = "${user.firstName.firstOrNull() ?: ""}${user.lastName.firstOrNull() ?: ""}".uppercase()
    val profilNom = profils.find { it.id == user.profilId }?.nom ?: "—"

    Row(
        modifier = Modifier.fillMaxWidth().background(rowBg).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFEDE9FE)),
            contentAlignment = Alignment.Center
        ) {
            Text(initiales, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
        }
        Text(
            "${user.firstName} ${user.lastName}",
            modifier = Modifier.weight(2f),
            fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = Color(0xFF1E293B), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(
            user.email,
            modifier = Modifier.weight(2f),
            fontSize = 12.sp, color = Color(0xFF475569),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Box(modifier = Modifier.weight(1.5f)) {
            TextButton(
                onClick = { if (profils.isNotEmpty()) showProfilMenu = true },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.cursorHand()
            ) {
                Text(profilNom, fontSize = 12.sp,
                    color = if (user.profilId != null) Color(0xFF3B82F6) else Color(0xFF94A3B8),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (profils.isNotEmpty())
                    Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(14.dp), tint = Color(0xFF94A3B8))
            }
            DropdownMenu(expanded = showProfilMenu, onDismissRequest = { showProfilMenu = false }) {
                profils.forEach { profil ->
                    DropdownMenuItem(
                        text = { Text(profil.nom, fontSize = 13.sp) },
                        onClick = { showProfilMenu = false; onAssignProfil(profil.id) },
                        leadingIcon = {
                            if (profil.id == user.profilId)
                                Icon(Icons.Default.Check, null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
        val (statusBg, statusFg) = if (user.isActive) Color(0xFFD1FAE5) to Color(0xFF059669)
                                    else Color(0xFFFEE2E2) to Color(0xFFEF4444)
        Surface(shape = RoundedCornerShape(20.dp), color = statusBg, modifier = Modifier.width(70.dp)) {
            Text(
                if (user.isActive) "Actif" else "Inactif",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                fontSize = 11.sp, fontWeight = FontWeight.Medium, color = statusFg
            )
        }
    }
}
