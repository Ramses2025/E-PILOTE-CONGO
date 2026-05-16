package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.CategorieWithModulesDto
import cg.epilote.desktop.data.GroupeDashboardStatsDto
import cg.epilote.desktop.data.ModuleApiDto
import cg.epilote.desktop.data.ModuleKpiDto
import cg.epilote.desktop.data.ScolarisationKpiDto
import cg.epilote.desktop.data.FinanceKpiDto
import cg.epilote.desktop.data.RhKpiDto
import cg.epilote.desktop.ui.screens.superadmin.KpiCard
import cg.epilote.desktop.ui.screens.superadmin.formatNumber
import cg.epilote.desktop.ui.screens.superadmin.formatXAF

// ── Orchestrateur : rendu dynamique par catégorie active ─────────────────────

@Composable
fun GroupeDynamicModuleKpis(
    categoriesWithModules: List<CategorieWithModulesDto>,
    stats: GroupeDashboardStatsDto,
    moduleKpis: Map<String, ModuleKpiDto> = emptyMap()
) {
    val activeSet = stats.modulesActifs.toSet()
    val activeCategories = filterActiveCategories(categoriesWithModules, activeSet)
    if (activeCategories.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(4.dp, 18.dp)
                    .background(Color(0xFF3B82F6), RoundedCornerShape(2.dp))
            )
            Text(
                "Tableau de bord par module",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
            Text(
                "(${activeCategories.size} catégorie${if (activeCategories.size > 1) "s" else ""} active${if (activeCategories.size > 1) "s" else ""})",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }

        activeCategories.forEach { cat ->
            val descriptor = getCategoryDescriptor(cat.code)
            val activeModules = cat.modules.filter { m -> m.code in activeSet || m.nom in activeSet }
            CategoryKpiSectionCard(
                descriptor = descriptor,
                activeModules = activeModules,
                allModules = cat.modules,
                stats = stats,
                kpi = moduleKpis[descriptor.code]
            )
        }
    }
}

// ── Carte section par catégorie ───────────────────────────────────────────────

@Composable
private fun CategoryKpiSectionCard(
    descriptor: CategoryKpiDescriptor,
    activeModules: List<ModuleApiDto>,
    allModules: List<ModuleApiDto>,
    stats: GroupeDashboardStatsDto,
    kpi: ModuleKpiDto?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CategorySectionHeader(descriptor, activeModules.size, allModules.size)
            HorizontalDivider(color = Color(0xFFF1F5F9))
            when (descriptor.code) {
                "scolarisation" -> ScolarisationModuleKpis(stats, activeModules, kpi?.scolarisation)
                "finance"       -> FinanceModuleKpis(stats, activeModules, kpi?.finance)
                "rh"            -> RhModuleKpis(stats, activeModules, kpi?.rh)
                else            -> GenericModuleKpis(descriptor, activeModules)
            }
            ActiveModuleChips(activeModules, descriptor.accentColor)
        }
    }
}

// ── En-tête de section ────────────────────────────────────────────────────────

@Composable
private fun CategorySectionHeader(
    descriptor: CategoryKpiDescriptor,
    activeCount: Int,
    totalCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp)
                    .background(descriptor.iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(descriptor.icon, null, tint = descriptor.accentColor, modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(descriptor.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                Text(descriptor.description, fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        }
        Surface(shape = RoundedCornerShape(20.dp), color = descriptor.accentColor.copy(alpha = 0.10f)) {
            Text(
                "$activeCount/$totalCount modules actifs",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = descriptor.accentColor
            )
        }
    }
}

// ── KPI : Scolarisation ───────────────────────────────────────────────────────

@Composable
private fun ScolarisationModuleKpis(
    stats: GroupeDashboardStatsDto,
    modules: List<ModuleApiDto>,
    kpi: ScolarisationKpiDto? = null
) {
    val nbEcoles  = kpi?.nbEcoles ?: stats.nbEcoles
    val nbUsers   = kpi?.nbUtilisateurs ?: stats.nbUtilisateurs
    val provinces = kpi?.ecolesByProvince?.size?.toLong()
        ?: stats.ecoles.groupBy { it.province.ifBlank { "Autre" } }.size.toLong()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.School,
            iconBg = Color(0xFFDBEAFE),
            iconTint = Color(0xFF3B82F6),
            label = "Écoles",
            value = formatNumber(nbEcoles),
            trendLabel = "dans votre groupe"
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.People,
            iconBg = Color(0xFFEDE9FE),
            iconTint = Color(0xFF7C3AED),
            label = "Utilisateurs",
            value = formatNumber(nbUsers),
            trendLabel = "tous rôles confondus"
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Tune,
            iconBg = Color(0xFFFCE7F3),
            iconTint = Color(0xFFEC4899),
            label = "Profils d'accès",
            value = formatNumber(stats.nbProfils),
            trendLabel = "profils configurés"
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.LocationOn,
            iconBg = Color(0xFFF0FDF4),
            iconTint = Color(0xFF10B981),
            label = "Départements",
            value = provinces.toString(),
            trendLabel = "départements couverts"
        )
    }
}

// ── KPI : Finance scolaire ────────────────────────────────────────────────────

@Composable
private fun FinanceModuleKpis(
    stats: GroupeDashboardStatsDto,
    modules: List<ModuleApiDto>,
    kpi: FinanceKpiDto? = null
) {
    val totalXAF  = kpi?.totalXAF ?: stats.facturationTotaleXAF
    val paidXAF   = kpi?.paidXAF ?: stats.montantPayeXAF
    val dueXAF    = kpi?.dueXAF ?: stats.montantDuXAF
    val nbOverdue = kpi?.nbOverdue ?: stats.nbFacturesEnRetard
    val nbFactures = kpi?.nbFactures ?: stats.nbFactures
    val taux      = kpi?.tauxRecouvrement ?: if (totalXAF > 0) ((paidXAF * 100) / totalXAF).toInt() else 0
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Receipt,
            iconBg = Color(0xFFF0F9FF),
            iconTint = Color(0xFF0EA5E9),
            label = "Total facturé",
            value = formatXAF(totalXAF),
            trendLabel = "$nbFactures facture${if (nbFactures > 1) "s" else ""}"
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Payments,
            iconBg = Color(0xFFD1FAE5),
            iconTint = Color(0xFF059669),
            label = "Encaissé",
            value = formatXAF(paidXAF),
            trendLabel = "$taux% du total facturé"
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.HourglassEmpty,
            iconBg = Color(0xFFFEF3C7),
            iconTint = Color(0xFFF59E0B),
            label = "Reste dû",
            value = formatXAF(dueXAF),
            trendLabel = "en attente de paiement"
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = if (nbOverdue > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
            iconBg = if (nbOverdue > 0) Color(0xFFFEE2E2) else Color(0xFFD1FAE5),
            iconTint = if (nbOverdue > 0) Color(0xFFEF4444) else Color(0xFF059669),
            label = "En retard",
            value = formatNumber(nbOverdue),
            trendLabel = if (nbOverdue == 0L) "Tout est à jour ✓" else "À régulariser",
            trendColor = if (nbOverdue > 0) Color(0xFFEF4444) else Color(0xFF059669)
        )
    }
}

// ── KPI : Ressources humaines ─────────────────────────────────────────────────

@Composable
private fun RhModuleKpis(
    stats: GroupeDashboardStatsDto,
    modules: List<ModuleApiDto>,
    kpi: RhKpiDto? = null
) {
    val nbTotal  = kpi?.nbTotal ?: stats.nbUtilisateurs
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.People,
            iconBg = Color(0xFFEDE9FE),
            iconTint = Color(0xFF7C3AED),
            label = "Personnel total",
            value = formatNumber(nbTotal),
            trendLabel = "tous rôles"
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Tune,
            iconBg = Color(0xFFFCE7F3),
            iconTint = Color(0xFFEC4899),
            label = "Profils d'accès",
            value = formatNumber(stats.nbProfils),
            trendLabel = "profils définis"
        )
        Box(Modifier.weight(1f))
        Box(Modifier.weight(1f))
    }
}

// ── Fallback : catégorie sans KPI spécifique ──────────────────────────────────

@Composable
private fun GenericModuleKpis(descriptor: CategoryKpiDescriptor, modules: List<ModuleApiDto>) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(descriptor.icon, null, tint = descriptor.accentColor.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
            Text(
                "Indicateurs ${descriptor.label} disponibles prochainement",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

// ── Chips des modules actifs ──────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveModuleChips(modules: List<ModuleApiDto>, accentColor: Color) {
    if (modules.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        modules.forEach { module ->
            Surface(shape = RoundedCornerShape(20.dp), color = accentColor.copy(alpha = 0.08f)) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(5.dp).background(accentColor, RoundedCornerShape(999.dp)))
                    Text(module.nom, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = accentColor)
                }
            }
        }
    }
}
