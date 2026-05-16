package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.GroupeDashboardStatsDto
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.screens.superadmin.formatXAF
import cg.epilote.desktop.ui.theme.cursorHand

@Immutable
internal data class GroupeSubscriptionUiState(
    val status: String,
    val daysUntilExpiry: Int,
    val accentColor: Color,
    val badgeLabel: String,
    val title: String,
    val subtitle: String,
    val expiryLabel: String?,
    val actionLabel: String,
    val shouldSuggestRenewal: Boolean
)

internal fun resolveGroupeSubscriptionUiState(
    stats: GroupeDashboardStatsDto,
    now: Long = System.currentTimeMillis()
): GroupeSubscriptionUiState {
    val status = stats.abonnementStatut.trim().lowercase().ifBlank { "pending" }
    val daysUntilExpiry = if (stats.abonnementDateFin > 0) {
        ((stats.abonnementDateFin - now) / 86_400_000L).toInt()
    } else -1
    val dateIsPast = stats.abonnementDateFin in 1 until now
    val isPending = status == "pending"
    val isGrace = status == "grace"
    val isSuspended = status == "suspended" || status == "cancelled"
    val isExpired = status == "expired" || isSuspended || dateIsPast
    val isExpiring = status == "expiring" || (!isPending && !isExpired && !isGrace && daysUntilExpiry in 0..30)

    val accentColor = when {
        isExpired -> Color(0xFFEF4444)
        isGrace -> Color(0xFFF97316)
        isPending || isExpiring -> Color(0xFFF59E0B)
        else -> Color(0xFF059669)
    }
    val badgeLabel = when {
        isExpired && isSuspended -> "SUSPENDU"
        isExpired -> "EXPIRÉ"
        isGrace -> "GRÂCE"
        isPending -> "EN ATTENTE"
        isExpiring -> "EXPIRE BIENTÔT"
        else -> "ACTIF"
    }
    val title = when {
        isExpired && isSuspended -> "Abonnement du groupe suspendu"
        isExpired -> "Abonnement du groupe expiré"
        isGrace -> "Période de grâce du groupe"
        isPending -> "Paiement initial en attente"
        isExpiring -> "Abonnement du groupe bientôt expiré"
        else -> "Abonnement du groupe actif"
    }
    val subtitle = when {
        isExpired -> "Les droits et quotas du groupe scolaire doivent être régularisés."
        isGrace -> "Accès temporaire limité avant suspension automatique."
        isPending -> "L'accès complet sera activé après paiement du groupe scolaire."
        isExpiring -> "Anticipez le renouvellement pour éviter une interruption."
        else -> "Ces droits et quotas s'appliquent à toutes les écoles et utilisateurs du groupe."
    }
    val expiryLabel = when {
        stats.abonnementDateFin <= 0 -> null
        isExpired -> "Expiré le ${formatDate(stats.abonnementDateFin)}"
        else -> "Valide jusqu'au ${formatDate(stats.abonnementDateFin)}"
    }

    return GroupeSubscriptionUiState(
        status = status,
        daysUntilExpiry = daysUntilExpiry,
        accentColor = accentColor,
        badgeLabel = badgeLabel,
        title = title,
        subtitle = subtitle,
        expiryLabel = expiryLabel,
        actionLabel = if (isPending || isExpired || isGrace || isExpiring) "Renouveler" else "Voir les détails",
        shouldSuggestRenewal = isPending || isExpired || isGrace || isExpiring
    )
}

@Composable
fun GroupePlanBannerCard(stats: GroupeDashboardStatsDto, onClick: () -> Unit) {
    val state = resolveGroupeSubscriptionUiState(stats)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).cursorHand(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        state.accentColor,
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(state.accentColor.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CreditCard, null, tint = state.accentColor, modifier = Modifier.size(26.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                stats.planNom.uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Surface(shape = RoundedCornerShape(6.dp), color = state.accentColor) {
                                Text(
                                    state.badgeLabel,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Text(state.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                        Text(state.subtitle, fontSize = 11.sp, color = Color(0xFF64748B))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                stats.planType.replaceFirstChar { it.uppercase() },
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            if (stats.prixXAF > 0) {
                                Text("•", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                Text(
                                    formatXAF(stats.prixXAF) + "/an",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            Text("•", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                            Text(
                                "${stats.nbEcoles}/${stats.quotaEcoles} écoles",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.expiryLabel?.let { label ->
                        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    if (state.daysUntilExpiry in 0..30 && (state.status == "active" || state.status == "expiring")) {
                        Text(
                            "Expire dans ${state.daysUntilExpiry} jour${if (state.daysUntilExpiry > 1) "s" else ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = state.accentColor
                        )
                    }
                    if (stats.renouvellementAuto) {
                        Text("↻ Renouvellement automatique", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    }
                    OutlinedButton(
                        onClick = onClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = state.accentColor),
                        border = BorderStroke(1.dp, state.accentColor.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.cursorHand()
                    ) {
                        Text(state.actionLabel, fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
