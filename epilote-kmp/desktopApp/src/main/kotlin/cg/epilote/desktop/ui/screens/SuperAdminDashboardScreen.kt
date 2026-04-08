package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.*
import cg.epilote.shared.domain.model.UserSession

data class AdminStats(
    val totalGroupes: Long = 0,
    val totalEcoles: Long = 0,
    val totalUtilisateurs: Long = 0,
    val totalModules: Long = 0,
    val totalPlans: Long = 0,
    val totalCategories: Long = 0
)

@Composable
fun SuperAdminDashboardScreen(
    session: UserSession,
    stats: AdminStats,
    isLoading: Boolean,
    onNavigateGroupes: () -> Unit,
    onNavigatePlans: () -> Unit,
    onNavigateModules: () -> Unit,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── En-tête ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Tableau de bord Super Admin",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Bienvenue, ${session.firstName} ${session.lastName} — Gestion globale E-PILOTE CONGO",
                    fontSize = 14.sp,
                    color = EpiloteTextMuted
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Actualiser")
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        }

        // ── Cartes statistiques (périmètre Super Admin) ─────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AdminStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Business,
                iconBg = Color(0xFFE3F7EF),
                iconColor = Color(0xFF2A9D8F),
                value = "${stats.totalGroupes}",
                label = "Groupes Scolaires",
                onClick = onNavigateGroupes
            )
            AdminStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CreditCard,
                iconBg = Color(0xFFE8E3FF),
                iconColor = Color(0xFF6C5CE7),
                value = "${stats.totalPlans}",
                label = "Plans",
                onClick = onNavigatePlans
            )
            AdminStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Extension,
                iconBg = Color(0xFFFFEDEB),
                iconColor = Color(0xFFE63946),
                value = "${stats.totalModules}",
                label = "Modules",
                onClick = onNavigateModules
            )
            AdminStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Category,
                iconBg = Color(0xFFE3F7EF),
                iconColor = Color(0xFF00875A),
                value = "${stats.totalCategories}",
                label = "Catégories",
                onClick = onNavigateModules
            )
        }

        // ── Info : écoles & utilisateurs = Admin Groupe ──────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF0F7FF)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = Color(0xFF1D3557), modifier = Modifier.size(20.dp))
                Column {
                    Text("Écoles et Utilisateurs", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF1D3557))
                    Text(
                        "La gestion des écoles, utilisateurs et profils d'accès est assurée par l'Admin de chaque Groupe Scolaire.",
                        fontSize = 12.sp, color = Color(0xFF64748B)
                    )
                }
            }
        }

        // ── Actions rapides ──────────────────────────────────────
        Text(
            "Actions rapides",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AdminActionCard(
                modifier = Modifier.weight(1f),
                title = "Nouveau Groupe Scolaire",
                description = "Créer un groupe et assigner un plan d'abonnement",
                icon = Icons.Default.AddBusiness,
                color = Color(0xFF2A9D8F),
                onClick = onNavigateGroupes
            )
            AdminActionCard(
                modifier = Modifier.weight(1f),
                title = "Définir les Plans",
                description = "Configurer les plans d'abonnement et leur couverture métier",
                icon = Icons.Default.CreditCard,
                color = Color(0xFF1D3557),
                onClick = onNavigatePlans
            )
            AdminActionCard(
                modifier = Modifier.weight(1f),
                title = "Référentiel Modules",
                description = "Gérer les modules et catégories de la plateforme",
                icon = Icons.Default.Extension,
                color = Color(0xFF6C5CE7),
                onClick = onNavigateModules
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AdminActionCard(
                modifier = Modifier.weight(1f),
                title = "Catégories Métiers",
                description = "Structurer les domaines fonctionnels de la plateforme",
                icon = Icons.Default.Category,
                color = Color(0xFFE9C46A),
                onClick = onNavigateModules
            )
            AdminActionCard(
                modifier = Modifier.weight(1f),
                title = "Vue globale Plateforme",
                description = "Suivre la croissance des groupes et la couverture fonctionnelle",
                icon = Icons.Default.Extension,
                color = Color(0xFFE63946),
                onClick = onNavigateModules
            )
            Spacer(Modifier.weight(1f))
        }

        // ── Référentiel des catégories ───────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Référentiel — 6 Catégories", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        Triple("Scolarité", Color(0xFF2A9D8F), true),
                        Triple("Pédagogie", Color(0xFF1D3557), true),
                        Triple("Finances", Color(0xFFE9C46A), false),
                        Triple("Personnel", Color(0xFFE63946), false),
                        Triple("Vie Scolaire", Color(0xFF6C5CE7), false),
                        Triple("Communication", Color(0xFF00875A), false)
                    ).forEach { (name, color, isCore) ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = color.copy(alpha = 0.08f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(color, CircleShape)
                                )
                                Text(
                                    name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = color
                                )
                                if (isCore) {
                                    Text(
                                        "CORE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = color.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Plans d'abonnement ───────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Plans d'abonnement", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PlanSummaryCard(
                        modifier = Modifier.weight(1f),
                        name = "Gratuit",
                        price = "0 XAF",
                        modules = "8 modules",
                        color = Color(0xFF2A9D8F)
                    )
                    PlanSummaryCard(
                        modifier = Modifier.weight(1f),
                        name = "Premium",
                        price = "150 000 XAF",
                        modules = "26 modules",
                        color = Color(0xFF1D3557)
                    )
                    PlanSummaryCard(
                        modifier = Modifier.weight(1f),
                        name = "Pro",
                        price = "350 000 XAF",
                        modules = "30 modules",
                        color = Color(0xFFE9C46A)
                    )
                }
            }
        }

        // ── Informations système ─────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D3557)),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Système E-PILOTE CONGO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Version 1.0.0 — Backend Spring Boot + Couchbase Capella", color = Color(0xFFDCE3EA), fontSize = 12.sp)
                    Text("IA : Mistral / GPT-4o — Sync : Capella App Services", color = Color(0xFFDCE3EA), fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF2A9D8F), CircleShape))
                        Text("Backend actif", color = Color(0xFF2A9D8F), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFE9C46A), CircleShape))
                        Text("IA disponible", color = Color(0xFFE9C46A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    value: String,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(value, fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = EpiloteTextMuted)
            }
        }
    }
}

@Composable
private fun AdminActionCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
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
                modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(description, fontSize = 11.sp, color = EpiloteTextMuted, maxLines = 2)
            }
        }
    }
}

@Composable
private fun PlanSummaryCard(
    modifier: Modifier = Modifier,
    name: String,
    price: String,
    modules: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Text(price, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(modules, fontSize = 11.sp, color = EpiloteTextMuted)
        }
    }
}
