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
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
internal fun SubscriptionCardGrid(
    subscriptions: List<SubscriptionDto>,
    onViewDetail: (SubscriptionDto) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cols = when {
            maxWidth > 1400.dp -> 4
            maxWidth > 1050.dp -> 3
            maxWidth > 700.dp -> 2
            else -> 1
        }
        val cardWidth = (maxWidth - 16.dp * (cols - 1)) / cols
        val rows = subscriptions.chunked(cols)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            rows.forEachIndexed { rowIndex, rowSubs ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowSubs.forEachIndexed { colIndex, subscription ->
                        val globalIndex = rowIndex * cols + colIndex
                        Box(modifier = Modifier.width(cardWidth)) {
                            AnimatedCardEntrance(index = globalIndex) {
                                SubscriptionGridCard(subscription = subscription, onClick = { onViewDetail(subscription) })
                            }
                        }
                    }
                    repeat(cols - rowSubs.size) { Spacer(Modifier.width(cardWidth)) }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionGridCard(subscription: SubscriptionDto, onClick: () -> Unit) {
    val statusColor = subscriptionStatusColor(subscription.statut)
    val accentColor = planColorById(subscription.planId)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().cursorHand().hoverScale(1.008f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = accentColor.copy(alpha = 0.12f)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Subscriptions, null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Text(subscription.planNom, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor)
                    }
                }
                Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.10f)) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(statusColor, RoundedCornerShape(999.dp)))
                        Text(subscriptionStatusLabel(subscription.statut), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(subscription.groupeNom, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D3557), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subscription.groupeSlug.ifBlank { subscription.groupeId }, fontSize = 12.sp, color = EpiloteTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC)) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubMetaRow(Icons.Default.Paid, formatMoneyXaf(subscription.prixXAF))
                    SubMetaRow(Icons.Default.CalendarMonth, "Début ${formatDate(subscription.dateDebut)}")
                    SubMetaRow(Icons.Default.Autorenew, if (subscription.renouvellementAuto) "Renouvellement auto" else "Renouvellement manuel")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SubInfoPill("${subscription.maxStudents} élèves", accentColor)
                SubInfoPill("${subscription.maxPersonnel} personnel", Color(0xFF6D28D9))
                SubInfoPill("${subscription.moduleCount} modules", Color(0xFF2563EB))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Cliquer pour gérer l'abonnement", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF1D3557), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SubMetaRow(icon: ImageVector, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = EpiloteTextMuted)
        Text(value, fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SubInfoPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
