package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import cg.epilote.desktop.ui.screens.superadmin.KpiCard
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.EpiloteTextMuted

@Composable
internal fun SupportKpiRow(
    criticalCount: Int,
    billingCount: Int,
    securityCount: Int,
    groupsAtRiskCount: Int,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 980.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.HealthAndSafety,
                        iconBg = Color(0xFFFEE2E2),
                        iconTint = Color(0xFFDC2626),
                        label = "Critiques",
                        value = "$criticalCount",
                        trendLabel = "à traiter immédiatement"
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.SupportAgent,
                        iconBg = Color(0xFFEDE9FE),
                        iconTint = Color(0xFF6D28D9),
                        label = "Facturation",
                        value = "$billingCount",
                        trendLabel = "cas à suivre"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AdminPanelSettings,
                        iconBg = Color(0xFFDBEAFE),
                        iconTint = Color(0xFF2563EB),
                        label = "Sécurité",
                        value = "$securityCount",
                        trendLabel = "comptes à vérifier"
                    )
                    KpiCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Groups,
                        iconBg = Color(0xFFD1FAE5),
                        iconTint = Color(0xFF059669),
                        label = "Groupes à risque",
                        value = "$groupsAtRiskCount",
                        trendLabel = "couverture support"
                    )
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.HealthAndSafety,
                    iconBg = Color(0xFFFEE2E2),
                    iconTint = Color(0xFFDC2626),
                    label = "Critiques",
                    value = "$criticalCount",
                    trendLabel = "à traiter immédiatement"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.SupportAgent,
                    iconBg = Color(0xFFEDE9FE),
                    iconTint = Color(0xFF6D28D9),
                    label = "Facturation",
                    value = "$billingCount",
                    trendLabel = "cas à suivre"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AdminPanelSettings,
                    iconBg = Color(0xFFDBEAFE),
                    iconTint = Color(0xFF2563EB),
                    label = "Sécurité",
                    value = "$securityCount",
                    trendLabel = "comptes à vérifier"
                )
                KpiCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Groups,
                    iconBg = Color(0xFFD1FAE5),
                    iconTint = Color(0xFF059669),
                    label = "Groupes à risque",
                    value = "$groupsAtRiskCount",
                    trendLabel = "couverture support"
                )
            }
        }
    }
}

@Composable
internal fun SupportCoverageCard(
    totalGroups: Int,
    groupsCovered: Int,
    totalAdmins: Int,
    activeAdmins: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Couverture du support", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("Surveillance des groupes actifs et du parc administrateur.", fontSize = 12.sp, color = EpiloteTextMuted)
            }
            SupportCoverageMetric("Groupes couverts", "$groupsCovered / $totalGroups")
            SupportCoverageMetric("Admins actifs", "$activeAdmins / $totalAdmins")
        }
    }
}

@Composable
private fun SupportCoverageMetric(label: String, value: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF8FAFC)) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, fontSize = 11.sp, color = EpiloteTextMuted)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D3557))
        }
    }
}

@Composable
internal fun SupportInsightPanel(
    modifier: Modifier = Modifier,
    criticalItems: List<AdminNotificationItem>,
    billingItems: List<AdminNotificationItem>,
    securityItems: List<AdminNotificationItem>,
    coverageItems: List<AdminNotificationItem>,
    onOpen: (AdminNotificationItem) -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Priorisation du support", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            SupportBucket("Escalades critiques", criticalItems, Color(0xFFDC2626), onOpen)
            SupportBucket("Facturation", billingItems, Color(0xFF7C3AED), onOpen)
            SupportBucket("Sécurité des accès", securityItems, Color(0xFF2563EB), onOpen)
            SupportBucket("Couverture groupes", coverageItems, Color(0xFF059669), onOpen)
        }
    }
}

@Composable
private fun SupportBucket(
    title: String,
    items: List<AdminNotificationItem>,
    accent: Color,
    onOpen: (AdminNotificationItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent)
        if (items.isEmpty()) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC)) {
                Text("Aucun élément prioritaire.", modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), fontSize = 11.sp, color = EpiloteTextMuted)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    Surface(onClick = { onOpen(item) }, shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.targetLabel, fontSize = 11.sp, color = EpiloteTextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(formatDate(item.createdAt), fontSize = 10.sp, color = EpiloteTextMuted)
                                Surface(shape = RoundedCornerShape(999.dp), color = accent.copy(alpha = 0.10f)) {
                                    Text(notificationSeverityLabel(item.severity), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, color = accent, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
