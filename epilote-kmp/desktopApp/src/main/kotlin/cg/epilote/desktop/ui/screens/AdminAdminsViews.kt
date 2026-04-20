package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.AnimatedCardEntrance
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import cg.epilote.desktop.ui.theme.hoverScale

@Composable
internal fun AdminCardGrid(
    admins: List<AdminUserDto>,
    groupes: List<GroupeDto>,
    onViewDetail: (AdminUserDto) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cols = when {
            maxWidth > 1400.dp -> 4
            maxWidth > 1050.dp -> 3
            maxWidth > 700.dp -> 2
            else -> 1
        }
        val cardWidth = (maxWidth - 16.dp * (cols - 1)) / cols
        val rows = admins.chunked(cols)

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rows.forEachIndexed { rowIndex, rowAdmins ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowAdmins.forEachIndexed { colIndex, admin ->
                        val globalIndex = rowIndex * cols + colIndex
                        Box(modifier = Modifier.width(cardWidth)) {
                            AnimatedCardEntrance(index = globalIndex) {
                                AdminGridCard(
                                    admin = admin,
                                    groupName = resolveAdminGroupName(admin, groupes),
                                    onClick = { onViewDetail(admin) }
                                )
                            }
                        }
                    }
                    repeat(cols - rowAdmins.size) {
                        Spacer(Modifier.width(cardWidth))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminGridCard(
    admin: AdminUserDto,
    groupName: String?,
    onClick: () -> Unit
) {
    val roleColor = adminRoleColor(admin.role)
    val statusColor = adminStatusColor(admin.status)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().cursorHand().hoverScale(1.008f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminAvatar(
                    avatarData = admin.avatar,
                    firstName = admin.firstName,
                    lastName = admin.lastName,
                    modifier = Modifier.size(52.dp)
                )
                Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.10f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(6.dp).background(statusColor, RoundedCornerShape(999.dp)))
                        Text(adminStatusLabel(admin.status), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${admin.firstName} ${admin.lastName}".trim(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D3557),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = admin.email.ifBlank { admin.username },
                    fontSize = 12.sp,
                    color = EpiloteTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = roleColor.copy(alpha = 0.12f)) {
                    Text(
                        text = adminRoleLabel(admin.role),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = roleColor
                    )
                }
                if (admin.mustChangePassword) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFEF3C7)) {
                        Text(
                            text = "Mdp à changer",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            AdminGridMetaRow(Icons.Default.Shield, groupName ?: "Non affecté")
            admin.phone?.takeIf { it.isNotBlank() }?.let { AdminGridMetaRow(Icons.Default.Phone, it) }
            AdminGridMetaRow(Icons.Default.CalendarMonth, admin.lastLoginAt?.let(::formatDate) ?: "Jamais connecté")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cliquer pour voir les détails",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF1D3557), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AdminGridMetaRow(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = EpiloteTextMuted)
        Text(value, fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun AdminTableView(
    admins: List<AdminUserDto>,
    groupes: List<GroupeDto>,
    onViewDetail: (AdminUserDto) -> Unit,
    onEdit: (AdminUserDto) -> Unit,
    onToggleStatus: (AdminUserDto) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminHeaderCell("Administrateur", Modifier.weight(2.6f))
                AdminHeaderCell("Rôle", Modifier.weight(1.2f))
                AdminHeaderCell("Groupe", Modifier.weight(1.5f))
                AdminHeaderCell("Téléphone", Modifier.weight(1.2f))
                AdminHeaderCell("Dernière connexion", Modifier.weight(1.2f))
                AdminHeaderCell("Statut", Modifier.weight(1f))
                AdminHeaderCell("Actions", Modifier.weight(1.2f))
            }
            HorizontalDivider(color = Color(0xFFE2E8F0))

            if (admins.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    Text("Aucun administrateur à afficher", color = EpiloteTextMuted)
                }
            }

            admins.forEachIndexed { index, admin ->
                AdminTableRow(
                    admin = admin,
                    groupName = resolveAdminGroupName(admin, groupes),
                    onViewDetail = { onViewDetail(admin) },
                    onEdit = { onEdit(admin) },
                    onToggleStatus = { onToggleStatus(admin) }
                )
                if (index < admins.lastIndex) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}

@Composable
private fun AdminHeaderCell(text: String, modifier: Modifier) {
    Text(text, modifier, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B), maxLines = 1)
}

@Composable
private fun AdminTableRow(
    admin: AdminUserDto,
    groupName: String?,
    onViewDetail: () -> Unit,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val roleColor = adminRoleColor(admin.role)
    val statusColor = adminStatusColor(admin.status)
    val statusIcon = if (admin.status == "active") Icons.Default.Block else Icons.Default.CheckCircle

    Surface(
        onClick = onViewDetail,
        modifier = Modifier.fillMaxWidth().cursorHand().hoverScale(1.004f),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(2.6f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AdminAvatar(
                    avatarData = admin.avatar,
                    firstName = admin.firstName,
                    lastName = admin.lastName,
                    modifier = Modifier.size(40.dp)
                )
                Column {
                    Text(
                        text = "${admin.firstName} ${admin.lastName}".trim(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = admin.email.ifBlank { admin.username },
                        fontSize = 11.sp,
                        color = EpiloteTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(Modifier.weight(1.2f)) {
                Surface(shape = RoundedCornerShape(6.dp), color = roleColor.copy(alpha = 0.12f)) {
                    Text(
                        text = adminRoleLabel(admin.role),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = roleColor
                    )
                }
            }

            Text(
                text = groupName ?: "Non affecté",
                modifier = Modifier.weight(1.5f),
                fontSize = 12.sp,
                color = Color(0xFF475569),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = admin.phone?.takeIf { it.isNotBlank() } ?: "—",
                modifier = Modifier.weight(1.2f),
                fontSize = 12.sp,
                color = Color(0xFF475569),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = admin.lastLoginAt?.let(::formatDate) ?: "Jamais",
                modifier = Modifier.weight(1.2f),
                fontSize = 12.sp,
                color = Color(0xFF475569),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(Modifier.weight(1f)) {
                Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.10f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(6.dp).background(statusColor, RoundedCornerShape(999.dp)))
                        Text(adminStatusLabel(admin.status), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                    }
                }
            }
            Row(
                modifier = Modifier.weight(1.2f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onViewDetail, modifier = Modifier.size(30.dp).cursorHand()) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp), tint = Color(0xFF1D3557))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp).cursorHand()) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = Color(0xFF1D3557))
                }
                IconButton(onClick = onToggleStatus, modifier = Modifier.size(30.dp).cursorHand()) {
                    Icon(statusIcon, null, modifier = Modifier.size(16.dp), tint = statusColor)
                }
            }
        }
    }
}

internal fun resolveAdminGroupName(admin: AdminUserDto, groupes: List<GroupeDto>): String? =
    admin.groupId?.let { gid -> groupes.find { it.id == gid }?.nom ?: gid }
