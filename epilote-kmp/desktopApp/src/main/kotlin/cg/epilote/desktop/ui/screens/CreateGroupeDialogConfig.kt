package cg.epilote.desktop.ui.screens

import androidx.compose.ui.graphics.Color
import java.net.URI
import java.time.Year

internal val DEPARTMENTS_CONGO = listOf(
    "Brazzaville", "Pointe-Noire", "Bouenza", "Cuvette", "Cuvette-Ouest",
    "Kouilou", "Lékoumou", "Likouala", "Niari", "Plateaux", "Pool", "Sangha"
)

internal val CITIES_BY_DEPARTMENT = mapOf(
    "Brazzaville" to listOf("Brazzaville"),
    "Pointe-Noire" to listOf("Pointe-Noire"),
    "Bouenza" to listOf("Madingou", "Nkayi", "Loudima", "Mouyondzi"),
    "Cuvette" to listOf("Owando", "Makoua", "Boundji", "Oyo"),
    "Cuvette-Ouest" to listOf("Ewo", "Kellé", "Mbama"),
    "Kouilou" to listOf("Loango", "Hinda", "Madingo-Kayes"),
    "Lékoumou" to listOf("Sibiti", "Zanaga", "Komono"),
    "Likouala" to listOf("Impfondo", "Dongou", "Épéna"),
    "Niari" to listOf("Dolisie", "Mossendjo", "Kibangou"),
    "Plateaux" to listOf("Djambala", "Gamboma", "Lékana"),
    "Pool" to listOf("Kinkala", "Boko", "Mindouli", "Kindamba"),
    "Sangha" to listOf("Ouesso", "Sembe", "Souanké")
)

internal val DEFAULT_PLAN_OPTIONS = listOf(
    GroupePlanOption("plan::gratuit", "Gratuit", "Démarrage", Color(0xFF6B7280)),
    GroupePlanOption("plan::premium", "Premium", "Croissance", Color(0xFFE9C46A)),
    GroupePlanOption("plan::pro", "Pro", "Établissements multi-sites", Color(0xFF6C5CE7)),
    GroupePlanOption("plan::institutionnel", "Institutionnel", "Grand compte", Color(0xFFE76F51))
)

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

internal fun validateEmail(email: String): String? {
    if (email.isBlank()) return null
    return if (EMAIL_REGEX.matches(email)) null else "Adresse email invalide."
}

internal fun normalizeWebsite(website: String): String? {
    val trimmed = website.trim()
    if (trimmed.isBlank()) return null
    return if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

internal fun validateWebsite(website: String?): String? {
    if (website.isNullOrBlank()) return null
    val isValid = runCatching {
        val uri = URI(website)
        val scheme = uri.scheme?.lowercase()
        (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
    return if (isValid) null else "Adresse web invalide."
}

internal fun validateFoundedYear(foundedYear: String): String? {
    val foundedYearValue = foundedYear.toIntOrNull()
    return when {
        foundedYear.isBlank() -> null
        foundedYear.length != 4 -> "Utilisez une année sur 4 chiffres."
        foundedYearValue == null -> "Année invalide."
        foundedYearValue !in 1900..Year.now().value -> "Année hors plage."
        else -> null
    }
}

internal fun List<PlanDto>.toPlanOptions(): List<GroupePlanOption> =
    filter { it.id.isNotBlank() && it.isActive }
        .map { plan ->
            GroupePlanOption(
                id = plan.id,
                label = plan.nom.ifBlank { plan.id.substringAfterLast("::").replaceFirstChar { it.uppercase() } },
                details = when {
                    plan.maxStudents > 0 && plan.maxPersonnel > 0 -> "${plan.maxStudents} élèves · ${plan.maxPersonnel} personnel · ${plan.prixXAF} XAF"
                    plan.maxStudents > 0 -> "${plan.maxStudents} élèves · ${plan.prixXAF} XAF"
                    plan.prixXAF > 0 -> "${plan.prixXAF} XAF"
                    else -> plan.type.replaceFirstChar { it.uppercase() }
                },
                color = when {
                    plan.id.contains("institution", true) || plan.nom.contains("institution", true) -> Color(0xFFE76F51)
                    plan.id.contains("pro", true) || plan.nom.contains("pro", true) -> Color(0xFF6C5CE7)
                    plan.id.contains("premium", true) || plan.nom.contains("premium", true) -> Color(0xFFE9C46A)
                    else -> Color(0xFF2A9D8F)
                }
            )
        }
        .ifEmpty { DEFAULT_PLAN_OPTIONS }
