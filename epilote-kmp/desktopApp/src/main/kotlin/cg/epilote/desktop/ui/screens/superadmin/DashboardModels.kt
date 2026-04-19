package cg.epilote.desktop.ui.screens.superadmin

import androidx.compose.ui.graphics.Color
import cg.epilote.desktop.data.GroupeApiDto
import cg.epilote.desktop.data.InvoiceApiDto
import cg.epilote.desktop.data.PlanDistributionDto
import cg.epilote.desktop.data.ProvinceStatsDto
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ── Statistiques du tableau de bord Super Admin ─────────────────────────────

data class AdminStats(
    val totalGroupes: Long = 0,
    val totalEcoles: Long = 0,
    val totalUtilisateurs: Long = 0,
    val totalModules: Long = 0,
    val totalPlans: Long = 0,
    val totalCategories: Long = 0,
    val totalSubscriptions: Long = 0,
    val activeSubscriptions: Long = 0,
    val totalInvoices: Long = 0,
    val revenueTotal: Long = 0,
    val revenuePaid: Long = 0,
    val invoicesOverdue: Long = 0,
    val groupesActifs: Long = 0,
    val groupesByProvince: List<ProvinceStatsDto> = emptyList(),
    val planDistribution: List<PlanDistributionDto> = emptyList(),
    val subscriptionsByStatus: Map<String, Long> = emptyMap(),
    val recentGroupes: List<GroupeApiDto> = emptyList(),
    val recentInvoices: List<InvoiceApiDto> = emptyList()
)

// ── Départements du Congo ───────────────────────────────────────────────────

data class DepartmentInfo(
    val nom: String,
    val chefLieu: String,
    val color: Color,
    val groupesCount: Int = 0,
    val ecolesCount: Int = 0
)

enum class DepartmentTier(val label: String, val color: Color) {
    HIGH("Élevé", Color(0xFF10B981)),
    MEDIUM("Moyen", Color(0xFFF59E0B)),
    LOW("Faible", Color(0xFFEF4444)),
    NONE("Aucun", Color(0xFFEC4899))
}

val congoDepartments = listOf(
    DepartmentInfo("Sangha", "Ouesso", Color(0xFF10B981)),
    DepartmentInfo("Likouala", "Impfondo", Color(0xFF10B981)),
    DepartmentInfo("Cuvette-Ouest", "Ewo", Color(0xFFEF4444)),
    DepartmentInfo("Cuvette", "Owando", Color(0xFFEF4444)),
    DepartmentInfo("Congo-Oubangui", "Impfondo", Color(0xFFEF4444)),
    DepartmentInfo("Lékoumou", "Sibiti", Color(0xFFF59E0B)),
    DepartmentInfo("Nkéni-Alima", "Gamboma", Color(0xFFF59E0B)),
    DepartmentInfo("Plateaux", "Djambala", Color(0xFFF59E0B)),
    DepartmentInfo("Niari", "Dolisie", Color(0xFFF59E0B)),
    DepartmentInfo("Bouenza", "Madingou", Color(0xFF6366F1)),
    DepartmentInfo("Djoué-Léfini", "Ngabé", Color(0xFF6366F1)),
    DepartmentInfo("Pool", "Kinkala", Color(0xFF6366F1)),
    DepartmentInfo("Brazzaville", "Capitale", Color(0xFF6366F1)),
    DepartmentInfo("Kouilou", "Hinda", Color(0xFFEF4444)),
    DepartmentInfo("Pointe-Noire", "Pointe-Noire", Color(0xFF6366F1))
)

// ── Conversion DTO → Modèle ─────────────────────────────────────────────────

fun cg.epilote.desktop.data.DashboardStatsDto.toAdminStats() = AdminStats(
    totalGroupes = totalGroupes, totalEcoles = totalEcoles,
    totalUtilisateurs = totalUtilisateurs, totalModules = totalModules,
    totalPlans = totalPlans, totalCategories = totalCategories,
    totalSubscriptions = totalSubscriptions, activeSubscriptions = activeSubscriptions,
    totalInvoices = totalInvoices, revenueTotal = revenueTotal,
    revenuePaid = revenuePaid, invoicesOverdue = invoicesOverdue,
    groupesActifs = groupesActifs,
    groupesByProvince = groupesByProvince, planDistribution = planDistribution,
    subscriptionsByStatus = subscriptionsByStatus,
    recentGroupes = recentGroupes, recentInvoices = recentInvoices
)

// ── Formatage utilitaire ────────────────────────────────────────────────────

private val xafFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.FRANCE)

fun formatXAF(amount: Long): String = "${xafFormat.format(amount)} FCFA"

fun formatNumber(value: Long): String = xafFormat.format(value)

fun formatDate(ts: Long): String =
    if (ts > 0) SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(ts)) else "—"

fun formatDateLong(): String =
    SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE).format(Date())
