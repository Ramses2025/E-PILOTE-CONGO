package cg.epilote.desktop.ui.screens.superadmin

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.GroupeApiDto
import cg.epilote.desktop.data.InvoiceApiDto
import cg.epilote.desktop.ui.theme.EpiloteTextMuted

// ── Section Alertes ─────────────────────────────────────────────────────────

@Composable
fun AlertsSection(overdueCount: Long, onNavigateNotifications: () -> Unit) {
    val pulseAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer { alpha = pulseAlpha }
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFEF4444).copy(alpha = 0.25f), Color.Transparent)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Alertes critiques", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFEF4444))
                Text(
                    "$overdueCount facture(s) impayée(s) nécessitent une relance immédiate",
                    fontSize = 12.sp, color = Color(0xFF991B1B)
                )
            }
            Surface(onClick = onNavigateNotifications, shape = RoundedCornerShape(10.dp), color = Color(0xFFEF4444), modifier = Modifier.hoverable(MutableInteractionSource())) {
                Text(
                    "Voir les notifications", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ── Section Activité récente ────────────────────────────────────────────────

@Composable
fun RecentActivityRow(
    stats: AdminStats,
    onNavigateGroupes: () -> Unit,
    onNavigateInvoices: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        RecentGroupesCard(
            groupes = stats.recentGroupes,
            modifier = Modifier.weight(1f),
            onNavigateGroupes = onNavigateGroupes
        )
        RecentInvoicesCard(
            invoices = stats.recentInvoices,
            modifier = Modifier.weight(1f),
            onNavigateInvoices = onNavigateInvoices
        )
    }
}

@Composable
private fun RecentGroupesCard(
    groupes: List<GroupeApiDto>,
    modifier: Modifier = Modifier,
    onNavigateGroupes: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).background(Color(0xFF2A9D8F).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Business, null, tint = Color(0xFF2A9D8F), modifier = Modifier.size(16.dp)) }
                    Text("Derniers groupes créés", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                }
                TextButton(onClick = onNavigateGroupes) {
                    Text("Voir tout", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2A9D8F))
                }
            }
            if (groupes.isEmpty()) {
                EmptyListPlaceholder(Icons.Default.FolderOpen, "Aucune création récente")
            } else {
                groupes.forEach { g ->
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF8FAFC), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).background(Color(0xFF2A9D8F).copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(g.nom.firstOrNull()?.uppercase() ?: "G", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2A9D8F))
                                }
                                Column {
                                    Text(g.nom, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                    Text(g.department ?: "", fontSize = 11.sp, color = EpiloteTextMuted)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${g.ecolesCount} école(s)", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(formatDate(g.createdAt), fontSize = 10.sp, color = EpiloteTextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentInvoicesCard(
    invoices: List<InvoiceApiDto>,
    modifier: Modifier = Modifier,
    onNavigateInvoices: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).background(Color(0xFFF59E0B).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Receipt, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp)) }
                    Text("Dernières factures", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                }
                TextButton(onClick = onNavigateInvoices) {
                    Text("Voir tout", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2A9D8F))
                }
            }
            if (invoices.isEmpty()) {
                EmptyListPlaceholder(Icons.Default.Receipt, "Aucune facture récente")
            } else {
                invoices.forEach { inv ->
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF8FAFC), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(inv.reference, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                Text(formatDate(inv.dateEmission), fontSize = 11.sp, color = EpiloteTextMuted)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatXAF(inv.montantXAF), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                InvoiceStatusBadge(inv.statut)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceStatusBadge(statut: String) {
    val (bg, fg, text) = when (statut) {
        "paid"      -> Triple(Color(0xFFD1FAE5), Color(0xFF059669), "Payée")
        "sent"      -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), "Envoyée")
        "overdue"   -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "En retard")
        "cancelled" -> Triple(Color(0xFFF1F5F9), Color(0xFF94A3B8), "Annulée")
        else        -> Triple(Color(0xFFF8FAFC), Color(0xFF64748B), "Brouillon")
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun EmptyListPlaceholder(icon: ImageVector, message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(message, fontSize = 12.sp, color = EpiloteTextMuted)
        }
    }
}

// ── Section Actions rapides ─────────────────────────────────────────────────

@Composable
fun QuickActionsRow(
    onNavigateGroupes: () -> Unit,
    onNavigatePlans: () -> Unit,
    onNavigateModules: () -> Unit,
    onNavigateAnnouncements: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Actions rapides", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            ActionCard(Modifier.weight(1f), "Nouveau Groupe", "Créer un groupe scolaire", Icons.Default.AddBusiness, Color(0xFF2A9D8F), onNavigateGroupes)
            ActionCard(Modifier.weight(1f), "Gérer les Plans", "Configurer les abonnements", Icons.Default.CreditCard, Color(0xFF1E40AF), onNavigatePlans)
            ActionCard(Modifier.weight(1f), "Modules & Catégories", "Référentiel fonctionnel", Icons.Default.Extension, Color(0xFF8B5CF6), onNavigateModules)
            ActionCard(Modifier.weight(1f), "Annonces globales", "Diffuser aux groupes", Icons.Default.Campaign, Color(0xFFF59E0B), onNavigateAnnouncements)
        }
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(if (isHovered) 1.04f else 1f, tween(150))

    Card(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .hoverable(interactionSource)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = if (isHovered) 0.12f else 0.06f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).background(color.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)) }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF0F172A))
                Text(description, fontSize = 10.sp, color = EpiloteTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ── Pied système ────────────────────────────────────────────────────────────

@Composable
fun SystemFooter() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("E-PILOTE CONGO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("v1.0.0", color = Color(0xFF64748B), fontSize = 11.sp)
                Text("•", color = Color(0xFF64748B), fontSize = 11.sp)
                Text("Spring Boot + Couchbase Capella", color = Color(0xFF64748B), fontSize = 11.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PulsatingDot(Color(0xFF10B981), "Backend")
                PulsatingDot(Color(0xFFF59E0B), "IA")
            }
        }
    }
}

@Composable
private fun PulsatingDot(color: Color, label: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(10.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale; alpha = pulseAlpha }
                    .background(color, CircleShape)
            )
            Box(modifier = Modifier.size(7.dp).background(color, CircleShape))
        }
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
