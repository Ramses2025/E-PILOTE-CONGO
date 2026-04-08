package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.*
import cg.epilote.shared.domain.model.UserSession

@Composable
fun DashboardScreen(session: UserSession) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Bonjour, ${session.firstName} !",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Voici un aperçu de votre établissement",
                fontSize = 14.sp,
                color = EpiloteTextMuted
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DashboardCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.People,
                iconBg = Color(0xFFE3F7EF),
                iconColor = EpiloteGreen,
                label = "Élèves",
                value = "—",
                subtitle = "effectif total"
            )
            DashboardCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Class,
                iconBg = Color(0xFFE6F0FF),
                iconColor = Color(0xFF0052CC),
                label = "Classes",
                value = "—",
                subtitle = "actives"
            )
            DashboardCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CheckBox,
                iconBg = Color(0xFFFFF0D1),
                iconColor = EpiloteOrange,
                label = "Absences",
                value = "—",
                subtitle = "aujourd'hui"
            )
            DashboardCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Grade,
                iconBg = Color(0xFFFFEDEB),
                iconColor = EpiloteRed,
                label = "Notes en attente",
                value = "—",
                subtitle = "à valider"
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickAccessCard(
                modifier = Modifier.weight(1f),
                title = "Saisie rapide des notes",
                description = "Accéder directement à la grille de notation de votre classe",
                icon = Icons.Default.Grade,
                color = EpiloteGreen
            )
            QuickAccessCard(
                modifier = Modifier.weight(1f),
                title = "Appel du jour",
                description = "Marquer les présences et absences pour aujourd'hui",
                icon = Icons.Default.CheckBox,
                color = Color(0xFF0052CC)
            )
            QuickAccessCard(
                modifier = Modifier.weight(1f),
                title = "Bulletins",
                description = "Générer les bulletins du trimestre en cours",
                icon = Icons.Default.Description,
                color = EpiloteOrange
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Accès rapide", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "Modules disponibles : ${session.permissions.map { it.moduleSlug }.joinToString(", ")}",
                    fontSize = 12.sp,
                    color = EpiloteTextMuted
                )
            }
        }
    }
}

@Composable
private fun DashboardCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    label: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 11.sp, color = EpiloteTextMuted)
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(description, fontSize = 11.sp, color = EpiloteTextMuted, maxLines = 2)
            }
        }
    }
}
