package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.GroupeNotificationDto
import cg.epilote.desktop.data.MonthlyActivityDto

// ── Activité du groupe (graphe simplifié barres) ─────────────────────────────

@Composable
fun GroupeActivitySection(timeline: List<MonthlyActivityDto>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Activité du groupe", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot(Color(0xFF3B82F6), "Utilisateurs")
                    LegendDot(Color(0xFF059669), "Écoles")
                }
            }

            if (timeline.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Aucune donnée d'activité disponible.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }
            } else {
                val maxValue = timeline.maxOf { maxOf(it.nbUsersCreated, it.nbEcolesCreated) }.coerceAtLeast(1)
                val displayTimeline = timeline.takeLast(6)

                Row(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    displayTimeline.forEach { month ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                val usersHeight = (month.nbUsersCreated.toFloat() / maxValue * 80).coerceAtLeast(4f)
                                val ecolesHeight = (month.nbEcolesCreated.toFloat() / maxValue * 80).coerceAtLeast(4f)
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .height(usersHeight.dp)
                                        .background(Color(0xFF3B82F6), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .align(Alignment.Bottom)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .height(ecolesHeight.dp)
                                        .background(Color(0xFF059669), RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .align(Alignment.Bottom)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                month.month.takeLast(5),
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(label, fontSize = 10.sp, color = Color(0xFF64748B))
    }
}

// ── Notifications Panel ──────────────────────────────────────────────────────

@Composable
fun GroupeNotificationsPanel(notifications: List<GroupeNotificationDto>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Notifications, null, tint = Color(0xFF1E293B), modifier = Modifier.size(18.dp))
                    Text("Notifications", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                }
                if (notifications.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFEF4444)) {
                        Text(
                            "${notifications.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF059669), modifier = Modifier.size(28.dp))
                        Text("Tout est en ordre", fontSize = 13.sp, color = Color(0xFF059669), fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    notifications.forEach { notif ->
                        NotificationItem(notif)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(notif: GroupeNotificationDto) {
    val (iconColor, bgColor) = when (notif.type) {
        "error" -> Color(0xFFEF4444) to Color(0xFFFEE2E2)
        "warning" -> Color(0xFFF59E0B) to Color(0xFFFEF3C7)
        else -> Color(0xFF3B82F6) to Color(0xFFDBEAFE)
    }
    val icon = when (notif.type) {
        "error" -> Icons.Default.Error
        "warning" -> Icons.Default.Warning
        else -> Icons.Default.Info
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(bgColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(notif.titre, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(notif.message, fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Surface(shape = RoundedCornerShape(8.dp), color = bgColor) {
            Text(
                notif.category.replaceFirstChar { it.uppercase() },
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = iconColor
            )
        }
    }
}
