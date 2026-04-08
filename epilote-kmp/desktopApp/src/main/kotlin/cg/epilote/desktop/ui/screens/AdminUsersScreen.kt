package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.*
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val ecoleId: String = "",
    val groupeId: String = "",
    val profilId: String = "",
    val role: String = "USER",
    val isActive: Boolean = true,
    val createdAt: Long = 0
)

@Composable
fun AdminUsersScreen(
    users: List<UserDto>,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Utilisateurs", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Gestion des comptes utilisateurs du système", fontSize = 13.sp, color = EpiloteTextMuted)
            }
            FilledTonalButton(onClick = onRefresh, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Actualiser")
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        }

        // Stats
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF2A9D8F).copy(alpha = 0.1f)) {
                Text("Total : ${users.size} utilisateurs",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2A9D8F))
            }
            val activeCount = users.count { it.isActive }
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF00875A).copy(alpha = 0.1f)) {
                Text("Actifs : $activeCount",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF00875A))
            }
        }

        if (users.isEmpty() && !isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.People, null, tint = EpiloteTextMuted, modifier = Modifier.size(48.dp))
                        Text("Aucun utilisateur", fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
                        Text("Les utilisateurs apparaîtront ici après création", fontSize = 12.sp, color = EpiloteTextMuted)
                    }
                }
            }
        }

        // Users list
        users.forEach { user ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(
                                roleColor(user.role).copy(alpha = 0.15f), CircleShape
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${user.firstName.take(1)}${user.lastName.take(1)}".uppercase(),
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = roleColor(user.role)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${user.firstName} ${user.lastName}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(user.email.ifBlank { user.username }, fontSize = 11.sp, color = EpiloteTextMuted)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(4.dp), color = roleColor(user.role).copy(alpha = 0.1f)) {
                            Text(
                                user.role.replace("_", " "),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = roleColor(user.role)
                            )
                        }
                        Box(
                            modifier = Modifier.size(8.dp).background(
                                if (user.isActive) Color(0xFF2A9D8F) else Color(0xFFDE350B),
                                CircleShape
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun roleColor(role: String): Color = when (role) {
    "SUPER_ADMIN" -> Color(0xFFE63946)
    "ADMIN_SYSTEME" -> Color(0xFF1D3557)
    "ADMIN_GROUPE" -> Color(0xFF6C5CE7)
    "DIRECTOR" -> Color(0xFFE9C46A)
    else -> Color(0xFF2A9D8F)
}
