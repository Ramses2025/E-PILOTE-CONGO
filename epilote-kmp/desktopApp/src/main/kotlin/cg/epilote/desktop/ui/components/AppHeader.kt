package cg.epilote.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.shared.domain.model.SyncStatus
import cg.epilote.shared.domain.model.UserSession
import cg.epilote.shared.presentation.viewmodel.SyncIndicatorViewModel

// ── Header fixe de l'application ─────────────────────────────────────────────

@Composable
fun AppHeader(
    session: UserSession,
    currentScreen: DesktopScreen,
    syncViewModel: SyncIndicatorViewModel,
    onLogout: () -> Unit,
    onNavigate: (DesktopScreen) -> Unit = {}
) {
    val syncStatus by syncViewModel.syncStatus.collectAsState()
    val pendingCount by syncViewModel.pendingCount.collectAsState()
    val syncLabel by syncViewModel.label.collectAsState(initial = "Hors ligne")

    var showProfileMenu by remember { mutableStateOf(false) }
    var showNotifPanel by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Gauche : Icône + Titre de la page courante ──
            PageTitle(currentScreen)

            // ── Droite : Notifications, Sync, Paramètres, Profil ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Notifications
                NotificationButton(
                    expanded = showNotifPanel,
                    onToggle = { showNotifPanel = !showNotifPanel },
                    onDismiss = { showNotifPanel = false }
                )

                // Sync status badge — Super Admin est toujours en ligne (API REST)
                if (session.role == "SUPER_ADMIN") {
                    SyncBadge(SyncStatus.SYNCED, "En ligne", 0L)
                } else {
                    SyncBadge(syncStatus, syncLabel, pendingCount)
                }

                // Paramètres
                IconButton(
                    onClick = { /* TODO: ouvrir paramètres */ },
                    modifier = Modifier.size(36.dp).pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Paramètres",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Séparateur vertical
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(Color(0xFFE2E8F0))
                )

                // Profil dropdown
                ProfileDropdown(
                    session = session,
                    expanded = showProfileMenu,
                    onToggle = { showProfileMenu = !showProfileMenu },
                    onDismiss = { showProfileMenu = false },
                    onLogout = onLogout,
                    onNavigate = onNavigate
                )
            }
        }
    }
}

// ── Titre de page dynamique ──────────────────────────────────────────────────

@Composable
private fun PageTitle(screen: DesktopScreen) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            screen.icon,
            contentDescription = null,
            tint = Color(0xFF1E293B),
            modifier = Modifier.size(22.dp)
        )
        Text(
            screen.label,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
        )
    }
}

// ── Bouton Notifications ─────────────────────────────────────────────────────

@Composable
private fun NotificationButton(
    expanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    Box {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(36.dp).pointerHoverIcon(PointerIcon.Hand)
        ) {
            Box {
                Icon(
                    Icons.Default.NotificationsNone,
                    contentDescription = "Notifications",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(22.dp)
                )
                // Badge rouge (exemple: 0 = pas de badge)
                // En production, connecter au nombre réel de notifications non lues
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            offset = DpOffset((-120).dp, 4.dp)
        ) {
            Column(
                modifier = Modifier.width(300.dp).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Notifications",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                HorizontalDivider(color = Color(0xFFE2E8F0))
                NotifItem(
                    Icons.Default.Info,
                    Color(0xFF3B82F6),
                    "Système prêt",
                    "Backend et base de données connectés"
                )
                NotifItem(
                    Icons.Default.CloudDone,
                    Color(0xFF10B981),
                    "Synchronisation active",
                    "Les données sont à jour"
                )
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Text(
                    "Voir toutes les notifications",
                    fontSize = 12.sp,
                    color = Color(0xFF3B82F6),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable { onDismiss() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun NotifItem(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { }
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
        }
    }
}

// ── Badge synchronisation ────────────────────────────────────────────────────

@Composable
private fun SyncBadge(status: SyncStatus, label: String, pending: Long) {
    val (bgColor, fgColor, icon) = when (status) {
        SyncStatus.SYNCED  -> Triple(Color(0xFFD1FAE5), Color(0xFF059669), Icons.Default.CloudDone)
        SyncStatus.PENDING -> Triple(Color(0xFFFEF3C7), Color(0xFFF59E0B), Icons.Default.CloudSync)
        else               -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), Icons.Default.CloudOff)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = fgColor, modifier = Modifier.size(14.dp))
            Text(
                if (status == SyncStatus.PENDING && pending > 0) "$label ($pending)"
                else label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = fgColor
            )
        }
    }
}

// ── Dropdown profil ──────────────────────────────────────────────────────────

@Composable
private fun ProfileDropdown(
    session: UserSession,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onNavigate: (DesktopScreen) -> Unit
) {
    val initials = "${session.firstName.firstOrNull()?.uppercase() ?: ""}${session.lastName.firstOrNull()?.uppercase() ?: ""}"
    val roleName = when (session.role) {
        "SUPER_ADMIN"  -> "Super Administrateur"
        "ADMIN_GROUPE" -> "Administrateur Groupe"
        else           -> session.role
    }

    Box {
        Row(
            modifier = Modifier
                .pointerHoverIcon(PointerIcon.Hand)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onToggle() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${session.firstName} ${session.lastName}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                Text(roleName, fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF3B82F6)),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            offset = DpOffset(0.dp, 4.dp)
        ) {
            Column(modifier = Modifier.width(220.dp).padding(4.dp)) {
                // En-tête profil
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Column {
                        Text(
                            "${session.firstName} ${session.lastName}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(roleName, fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))

                ProfileMenuItem(Icons.Default.Person, "Mon profil") { onDismiss() }
                ProfileMenuItem(Icons.Default.Settings, "Paramètres") { onDismiss() }
                ProfileMenuItem(Icons.Default.Help, "Aide & Support") { onDismiss() }

                HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                // Déconnexion en rouge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onDismiss()
                            onLogout()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Logout, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    Text("Déconnexion", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
        Text(label, fontSize = 13.sp, color = Color(0xFF334155))
    }
}
