package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import cg.epilote.desktop.ui.screens.superadmin.KpiCard
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.AnimatedCardEntrance
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import cg.epilote.desktop.ui.theme.hoverScale

internal fun subscriptionStatusColor(status: String): Color = when (status.lowercase()) {
    "active" -> Color(0xFF059669)
    "suspended" -> Color(0xFFD97706)
    "cancelled" -> Color(0xFFB91C1C)
    else -> Color(0xFF64748B)
}

internal fun subscriptionStatusLabel(status: String): String = when (status.lowercase()) {
    "active" -> "Actif"
    "suspended" -> "Suspendu"
    "cancelled" -> "Annulé"
    else -> status.replaceFirstChar { it.uppercase() }
}

internal fun formatMoneyXaf(amount: Long): String = "% ,d XAF".replace(" ", "").format(amount).replace(",", " ")

@Composable
internal fun SubscriptionKpiRow(
    totalSubscriptions: Int,
    activeSubscriptions: Int,
    suspendedSubscriptions: Int,
    monthlyRevenue: Long,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 980.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Subscriptions, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF2563EB), label = "Abonnements", value = "$totalSubscriptions", trendLabel = "$activeSubscriptions actifs")
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.CheckCircle, iconBg = Color(0xFFD1FAE5), iconTint = Color(0xFF059669), label = "Actifs", value = "$activeSubscriptions", trendLabel = "en service")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Groups, iconBg = Color(0xFFFFEDD5), iconTint = Color(0xFFD97706), label = "Suspendus", value = "$suspendedSubscriptions", trendLabel = "à relancer")
                    KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Paid, iconBg = Color(0xFFEDE9FE), iconTint = Color(0xFF6D28D9), label = "MRR estimé", value = formatMoneyXaf(monthlyRevenue), trendLabel = "sur plans actifs")
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Subscriptions, iconBg = Color(0xFFDBEAFE), iconTint = Color(0xFF2563EB), label = "Abonnements", value = "$totalSubscriptions", trendLabel = "$activeSubscriptions actifs")
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.CheckCircle, iconBg = Color(0xFFD1FAE5), iconTint = Color(0xFF059669), label = "Actifs", value = "$activeSubscriptions", trendLabel = "en service")
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Groups, iconBg = Color(0xFFFFEDD5), iconTint = Color(0xFFD97706), label = "Suspendus", value = "$suspendedSubscriptions", trendLabel = "à relancer")
                KpiCard(modifier = Modifier.weight(1f), icon = Icons.Default.Paid, iconBg = Color(0xFFEDE9FE), iconTint = Color(0xFF6D28D9), label = "MRR estimé", value = formatMoneyXaf(monthlyRevenue), trendLabel = "sur plans actifs")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubscriptionToolbar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterStatus: String,
    onFilterStatusChange: (String) -> Unit,
    filterPlanId: String,
    onFilterPlanIdChange: (String) -> Unit,
    planOptions: List<Pair<String, String>>,
    sortBy: String,
    onSortChange: (String) -> Unit,
    viewMode: String,
    onViewModeChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreateSubscription: () -> Unit,
    totalResults: Int,
    modifier: Modifier = Modifier
) {
    var showPlanMenu by remember { mutableStateOf(false) }
    val selectedPlanLabel = planOptions.firstOrNull { it.first == filterPlanId }?.second ?: "Tous les plans"

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                label = { Text("Rechercher un groupe ou un plan…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFF1F5F9)) {
                    Text("$totalResults résultat(s)", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9)) {
                    Row(Modifier.padding(2.dp)) {
                        IconButton(onClick = { onViewModeChange("card") }, modifier = Modifier.size(32.dp).cursorHand()) {
                            Icon(Icons.Default.ViewModule, "Cartes", Modifier.size(18.dp), tint = if (viewMode == "card") Color(0xFF1D3557) else Color(0xFF94A3B8))
                        }
                        IconButton(onClick = { onViewModeChange("table") }, modifier = Modifier.size(32.dp).cursorHand()) {
                            Icon(Icons.Default.TableChart, "Tableau", Modifier.size(18.dp), tint = if (viewMode == "table") Color(0xFF1D3557) else Color(0xFF94A3B8))
                        }
                    }
                }
                FilledTonalButton(onClick = onRefresh, shape = RoundedCornerShape(10.dp), modifier = Modifier.cursorHand()) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Text("Actualiser", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                }
                FilledTonalButton(
                    onClick = onCreateSubscription,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand(),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1D3557), contentColor = Color.White)
                ) {
                    Icon(Icons.Default.AddCard, null, modifier = Modifier.size(16.dp))
                    Text("Nouvel abonnement", modifier = Modifier.padding(start = 4.dp), fontSize = 12.sp)
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all" to "Tous", "active" to "Actifs", "suspended" to "Suspendus", "cancelled" to "Annulés").forEach { (value, label) ->
                FilterChip(
                    selected = filterStatus == value,
                    onClick = { onFilterStatusChange(value) },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.cursorHand(),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1D3557).copy(alpha = 0.12f), selectedLabelColor = Color(0xFF1D3557))
                )
            }

            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE2E8F0)) {
                Text("│", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, color = Color(0xFF94A3B8))
            }

            Box {
                OutlinedButton(onClick = { showPlanMenu = true }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                    Text(selectedPlanLabel, fontSize = 11.sp, maxLines = 1)
                }
                DropdownMenu(expanded = showPlanMenu, onDismissRequest = { showPlanMenu = false }) {
                    planOptions.forEach { (value, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = {
                            onFilterPlanIdChange(value)
                            showPlanMenu = false
                        })
                    }
                }
            }

            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE2E8F0)) {
                Text("│", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 12.sp, color = Color(0xFF94A3B8))
            }

            listOf("recent" to "Récent", "group" to "Groupe", "amount" to "Montant").forEach { (value, label) ->
                FilterChip(
                    selected = sortBy == value,
                    onClick = { onSortChange(value) },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.cursorHand(),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF059669).copy(alpha = 0.12f), selectedLabelColor = Color(0xFF059669))
                )
            }
        }
    }
}

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
                    repeat(cols - rowSubs.size) {
                        Spacer(Modifier.width(cardWidth))
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionGridCard(
    subscription: SubscriptionDto,
    onClick: () -> Unit
) {
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
                    SubscriptionMetaRow(Icons.Default.Paid, formatMoneyXaf(subscription.prixXAF))
                    SubscriptionMetaRow(Icons.Default.CalendarMonth, "Début ${formatDate(subscription.dateDebut)}")
                    SubscriptionMetaRow(Icons.Default.Autorenew, if (subscription.renouvellementAuto) "Renouvellement auto" else "Renouvellement manuel")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                InfoPill("${subscription.maxStudents} élèves", accentColor)
                InfoPill("${subscription.maxPersonnel} personnel", Color(0xFF6D28D9))
                InfoPill("${subscription.moduleCount} modules", Color(0xFF2563EB))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Cliquer pour gérer l'abonnement", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF1D3557), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SubscriptionMetaRow(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = EpiloteTextMuted)
        Text(value, fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun InfoPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
internal fun SubscriptionTableView(
    subscriptions: List<SubscriptionDto>,
    onViewDetail: (SubscriptionDto) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SubscriptionHeaderCell("Groupe", Modifier.weight(2.2f))
                SubscriptionHeaderCell("Plan", Modifier.weight(1.3f))
                SubscriptionHeaderCell("Montant", Modifier.weight(1.1f))
                SubscriptionHeaderCell("Début", Modifier.weight(1.1f))
                SubscriptionHeaderCell("Renouvellement", Modifier.weight(1.2f))
                SubscriptionHeaderCell("Statut", Modifier.weight(1f))
                SubscriptionHeaderCell("Action", Modifier.weight(0.8f))
            }
            HorizontalDivider(color = Color(0xFFE2E8F0))

            if (subscriptions.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    Text("Aucun abonnement à afficher", color = EpiloteTextMuted)
                }
            }

            subscriptions.forEachIndexed { index, subscription ->
                SubscriptionTableRow(subscription = subscription, onViewDetail = { onViewDetail(subscription) })
                if (index < subscriptions.lastIndex) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    }
}

@Composable
private fun SubscriptionHeaderCell(text: String, modifier: Modifier) {
    Text(text, modifier, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B), maxLines = 1)
}

@Composable
private fun SubscriptionTableRow(subscription: SubscriptionDto, onViewDetail: () -> Unit) {
    val statusColor = subscriptionStatusColor(subscription.statut)
    val accentColor = planColorById(subscription.planId)

    Surface(onClick = onViewDetail, modifier = Modifier.fillMaxWidth().cursorHand().hoverScale(1.004f), color = Color.Transparent) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(2.2f)) {
                Text(subscription.groupeNom, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subscription.groupeSlug.ifBlank { subscription.groupeId }, fontSize = 11.sp, color = EpiloteTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.weight(1.3f)) {
                Surface(shape = RoundedCornerShape(6.dp), color = accentColor.copy(alpha = 0.12f)) {
                    Text(subscription.planNom, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
                }
            }
            Text(formatMoneyXaf(subscription.prixXAF), modifier = Modifier.weight(1.1f), fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1)
            Text(formatDate(subscription.dateDebut), modifier = Modifier.weight(1.1f), fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1)
            Text(if (subscription.renouvellementAuto) "Auto" else "Manuel", modifier = Modifier.weight(1.2f), fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1)
            Box(Modifier.weight(1f)) {
                Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.10f)) {
                    Text(subscriptionStatusLabel(subscription.statut), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                }
            }
            IconButton(onClick = onViewDetail, modifier = Modifier.weight(0.8f).size(30.dp).cursorHand()) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp), tint = Color(0xFF1D3557))
            }
        }
    }
}
