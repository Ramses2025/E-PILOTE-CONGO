package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MarkunreadMailbox
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.AnnouncementApiDto
import cg.epilote.desktop.ui.screens.superadmin.KpiCard
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.cursorHand

@Composable
internal fun AdminMessagingKpiRow(
    totalCommunications: Int,
    inboxCount: Int,
    sentCount: Int,
    trashCount: Int,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 980.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Forum,
                        iconBg = Color(0xFFDBEAFE),
                        iconTint = Color(0xFF3B82F6),
                        label = "Communications",
                        value = "$totalCommunications",
                        trendLabel = "messages + annonces"
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.MarkunreadMailbox,
                        iconBg = Color(0xFFEDE9FE),
                        iconTint = Color(0xFF6C5CE7),
                        label = "Réception",
                        value = "$inboxCount",
                        trendLabel = "à traiter"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.Send,
                        iconBg = Color(0xFFFEE2E2),
                        iconTint = Color(0xFFE63946),
                        label = "Envoyés",
                        value = "$sentCount",
                        trendLabel = "diffusions actives"
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Delete,
                        iconBg = Color(0xFFD1FAE5),
                        iconTint = Color(0xFF059669),
                        label = "Corbeille",
                        value = "$trashCount",
                        trendLabel = "éléments supprimés",
                        trendColor = Color(0xFF059669)
                    )
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Forum,
                    iconBg = Color(0xFFDBEAFE),
                    iconTint = Color(0xFF3B82F6),
                    label = "Communications",
                    value = "$totalCommunications",
                    trendLabel = "messages + annonces"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.MarkunreadMailbox,
                    iconBg = Color(0xFFEDE9FE),
                    iconTint = Color(0xFF6C5CE7),
                    label = "Réception",
                    value = "$inboxCount",
                    trendLabel = "à traiter"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.Send,
                    iconBg = Color(0xFFFEE2E2),
                    iconTint = Color(0xFFE63946),
                    label = "Envoyés",
                    value = "$sentCount",
                    trendLabel = "diffusions actives"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Delete,
                    iconBg = Color(0xFFD1FAE5),
                    iconTint = Color(0xFF059669),
                    label = "Corbeille",
                    value = "$trashCount",
                    trendLabel = "éléments supprimés",
                    trendColor = Color(0xFF059669)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AdminMessagingToolbar(
    mailbox: AdminMessagingMailbox,
    onMailboxChange: (AdminMessagingMailbox) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    targetFilter: String,
    onTargetFilterChange: (String) -> Unit,
    targetOptions: List<Pair<String, String>>,
    totalResults: Int,
    onRefresh: () -> Unit,
    onComposeMessage: () -> Unit,
    onComposeAnnouncement: () -> Unit
) {
    var showTargetMenu by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                label = { Text("Rechercher une communication…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFF1F5F9)) {
                    Text(
                        text = "$totalResults résultat(s)",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
                FilledTonalButton(onClick = onRefresh, shape = RoundedCornerShape(10.dp), modifier = Modifier.cursorHand()) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Text("Actualiser", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                }
                FilledTonalButton(
                    onClick = onComposeMessage,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1D3557), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Forum, null, modifier = Modifier.size(16.dp))
                    Text("Nouveau message", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                }
                FilledTonalButton(
                    onClick = onComposeAnnouncement,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEA580C), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Campaign, null, modifier = Modifier.size(16.dp))
                    Text("Annonce officielle", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AdminMessagingMailbox.entries.forEach { option ->
                FilterChip(
                    selected = mailbox == option,
                    onClick = { onMailboxChange(option) },
                    label = { Text(option.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFDBEAFE),
                        selectedLabelColor = Color(0xFF1D4ED8)
                    )
                )
            }
            OutlinedButton(onClick = { showTargetMenu = true }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(999.dp)) {
                Text(targetOptions.firstOrNull { it.first == targetFilter }?.second ?: "Toutes les cibles")
            }
            DropdownMenu(expanded = showTargetMenu, onDismissRequest = { showTargetMenu = false }) {
                targetOptions.forEach { (key, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        onTargetFilterChange(key)
                        showTargetMenu = false
                    })
                }
            }
        }
    }
}

@Composable
internal fun AdminMessagingThreadCard(
    thread: AdminMessageThread,
    onOpen: () -> Unit,
    onReply: () -> Unit,
    onArchive: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onRestore: (() -> Unit)?
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(thread.targetLabel, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text(thread.lastSubject, fontSize = 13.sp, color = Color(0xFF1D3557), fontWeight = FontWeight.Medium)
                    Text(thread.lastPreview, fontSize = 13.sp, color = Color(0xFF334155), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFEEF2FF)) {
                    Text("${thread.messageCount} msg", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, color = Color(0xFF4F46E5), fontWeight = FontWeight.SemiBold)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(34.dp).background(Color(0xFFEEF2FF), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Forum, null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(thread.targetType.replace('_', ' '), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1D3557))
                        Text(formatDate(thread.lastCreatedAt), fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onReply, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Reply, null, modifier = Modifier.size(16.dp))
                        Text("Répondre", modifier = Modifier.padding(start = 4.dp))
                    }
                    onArchive?.let {
                        OutlinedButton(onClick = it, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Archive, null, modifier = Modifier.size(16.dp))
                            Text("Archiver", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    onDelete?.let {
                        OutlinedButton(onClick = it, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Text("Supprimer", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                    onRestore?.let {
                        OutlinedButton(onClick = it, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.RestoreFromTrash, null, modifier = Modifier.size(16.dp))
                            Text("Restaurer", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AdminMessagingAnnouncementCard(
    announcement: AnnouncementApiDto,
    onOpen: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(announcement.titre.ifBlank { "Annonce officielle" }, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text(announcement.contenu, fontSize = 13.sp, color = Color(0xFF334155), maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFFFEDD5)) {
                    Text(announcementTargetLabel(announcement.cible), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, color = Color(0xFFEA580C), fontWeight = FontWeight.SemiBold)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(34.dp).background(Color(0xFFFFEDD5), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Campaign, null, tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(announcement.createdBy.ifBlank { "Super-admin" }, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1D3557))
                        Text(formatDate(announcement.createdAt), fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
                Text("Voir le détail", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D3557))
            }
        }
    }
}
