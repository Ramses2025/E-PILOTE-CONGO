package cg.epilote.desktop.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// ── Descripteur visuel d'une catégorie KPI ────────────────────────────────────

data class CategoryKpiDescriptor(
    val code: String,
    val label: String,
    val icon: ImageVector,
    val accentColor: Color,
    val iconBg: Color,
    val description: String,
    val requiredPlan: String = "gratuit"
)

// ── Registre : code catégorie → descripteur KPI ───────────────────────────────

fun normalizeDashboardCategoryCode(code: String): String = when (code.trim().lowercase()) {
    "scolarite", "scolarisation" -> "scolarisation"
    "finance", "finances" -> "finance"
    "rh", "personnel", "ressources-humaines", "ressources_humaines" -> "rh"
    else -> code.trim().lowercase()
}

val kpiCategoryRegistry: Map<String, CategoryKpiDescriptor> = mapOf(
    "scolarisation" to CategoryKpiDescriptor(
        code = "scolarisation",
        label = "Scolarisation",
        icon = Icons.Default.School,
        accentColor = Color(0xFF3B82F6),
        iconBg = Color(0xFFDBEAFE),
        description = "Inscriptions, classes et suivi scolaire"
    ),
    "finance" to CategoryKpiDescriptor(
        code = "finance",
        label = "Finance scolaire",
        icon = Icons.Default.Payments,
        accentColor = Color(0xFF059669),
        iconBg = Color(0xFFD1FAE5),
        description = "Frais scolaires, facturation et paiements",
        requiredPlan = "pro"
    ),
    "rh" to CategoryKpiDescriptor(
        code = "rh",
        label = "Ressources humaines",
        icon = Icons.Default.People,
        accentColor = Color(0xFF7C3AED),
        iconBg = Color(0xFFEDE9FE),
        description = "Personnel enseignant et administratif",
        requiredPlan = "pro"
    ),
    "examens" to CategoryKpiDescriptor(
        code = "examens",
        label = "Examens & Notes",
        icon = Icons.AutoMirrored.Filled.Assignment,
        accentColor = Color(0xFFEC4899),
        iconBg = Color(0xFFFCE7F3),
        description = "Évaluations, bulletins et résultats",
        requiredPlan = "premium"
    ),
    "communication" to CategoryKpiDescriptor(
        code = "communication",
        label = "Communication",
        icon = Icons.Default.Forum,
        accentColor = Color(0xFFF59E0B),
        iconBg = Color(0xFFFEF3C7),
        description = "Messagerie, annonces et notifications"
    ),
    "materiel" to CategoryKpiDescriptor(
        code = "materiel",
        label = "Matériels & Ressources",
        icon = Icons.Default.Inventory2,
        accentColor = Color(0xFF64748B),
        iconBg = Color(0xFFF1F5F9),
        description = "Gestion des équipements et fournitures",
        requiredPlan = "premium"
    )
)

// ── Résolution avec fallback générique ───────────────────────────────────────

fun getCategoryDescriptor(code: String): CategoryKpiDescriptor {
    val normalizedCode = normalizeDashboardCategoryCode(code)
    return kpiCategoryRegistry[normalizedCode] ?: CategoryKpiDescriptor(
        code = normalizedCode,
        label = normalizedCode.split("-", "_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
        icon = Icons.Default.Extension,
        accentColor = Color(0xFF6366F1),
        iconBg = Color(0xFFE0E7FF),
        description = "Module $normalizedCode"
    )
}

// ── Filtre : catégories actives pour un ensemble de modules actifs ────────────

fun filterActiveCategories(
    categoriesWithModules: List<cg.epilote.desktop.data.CategorieWithModulesDto>,
    activeModuleSlugs: Set<String>
): List<cg.epilote.desktop.data.CategorieWithModulesDto> =
    categoriesWithModules.filter { cat ->
        cat.modules.any { m -> m.code in activeModuleSlugs || m.nom in activeModuleSlugs }
    }.sortedBy { it.ordre }
