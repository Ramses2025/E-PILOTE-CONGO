package cg.epilote.backend.admin

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

// ── Dashboard Stats ─────────────────────────────────────────────
data class DashboardStatsResponse(
    val totalGroupes: Long,
    val totalEcoles: Long,
    val totalUtilisateurs: Long,
    val totalModules: Long,
    val totalPlans: Long,
    val totalCategories: Long,
    val plans: List<PlanResponse>,
    val categories: List<CategorieInfo>
)

// ── Catégories (constantes — non gérables en CRUD) ─────────────

data class CategorieInfo(val code: String, val nom: String, val isCore: Boolean, val ordre: Int)

object CategorieConstants {
    val ALL = listOf(
        CategorieInfo("scolarite",     "Scolarité",     isCore = true,  ordre = 0),
        CategorieInfo("pedagogie",     "Pédagogie",     isCore = true,  ordre = 1),
        CategorieInfo("finances",      "Finances",      isCore = false, ordre = 2),
        CategorieInfo("personnel",     "Personnel",     isCore = false, ordre = 3),
        CategorieInfo("vie-scolaire",  "Vie Scolaire",  isCore = false, ordre = 4),
        CategorieInfo("communication", "Communication", isCore = false, ordre = 5)
    )
    val CODES: Set<String> = ALL.map { it.code }.toSet()
}

// ── Exception métier ─────────────────────────────────────────────

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidModuleForPlanException(slugs: List<String>) :
    RuntimeException("Modules hors plan : ${slugs.joinToString(", ")}")

// ── Groupe Scolaire ─────────────────────────────────────────────
data class CreateGroupeRequest(
    @field:NotBlank val nom: String,
    @field:NotBlank val province: String,
    @field:NotBlank val planId: String
)

data class GroupeResponse(
    val id: String,
    val nom: String,
    val province: String,
    val planId: String,
    val ecolesCount: Int,
    val createdAt: Long
)

// ── École ────────────────────────────────────────────────────────
data class CreateEcoleRequest(
    @field:NotBlank val nom: String,
    @field:NotBlank val province: String,
    @field:NotBlank val territoire: String,
    val niveaux: List<String> = listOf("primaire")
)

data class EcoleResponse(
    val id: String,
    val groupeId: String,
    val nom: String,
    val province: String,
    val territoire: String,
    val niveaux: List<String>,
    val planId: String,
    val createdAt: Long
)

// ── Utilisateur ──────────────────────────────────────────────────
data class CreateUserRequest(
    @field:NotBlank val username: String,
    @field:NotBlank @field:Size(min = 8) val password: String,
    @field:NotBlank val nom: String,
    @field:NotBlank val prenom: String,
    @field:Email val email: String,
    @field:NotBlank val ecoleId: String,
    @field:NotBlank val profilId: String
)

data class CreateAdminGroupeRequest(
    @field:NotBlank val username: String,
    @field:NotBlank @field:Size(min = 8) val password: String,
    @field:NotBlank val nom: String,
    @field:NotBlank val prenom: String,
    @field:Email val email: String
)

data class UserResponse(
    val id: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val ecoleId: String?,
    val groupeId: String,
    val profilId: String?,
    val role: String,
    val isActive: Boolean,
    val createdAt: Long
)

data class AssignProfilRequest(
    @field:NotBlank val profilId: String
)

// ── Profil d'accès ───────────────────────────────────────────────
data class ProfilPermission(
    @field:NotBlank val moduleSlug: String,
    val canRead: Boolean = true,
    val canWrite: Boolean = false,
    val canDelete: Boolean = false,
    val canExport: Boolean = false
)

data class CreateProfilRequest(
    @field:NotBlank val nom: String,
    val permissions: List<ProfilPermission> = emptyList()
)

data class ProfilResponse(
    val id: String,
    val groupeId: String,
    val nom: String,
    val permissions: List<ProfilPermission>,
    val isDefault: Boolean,
    val createdAt: Long
)

// ── Module ───────────────────────────────────────────────────────
data class CreateModuleRequest(
    @field:NotBlank val code: String,
    @field:NotBlank val nom: String,
    @field:NotBlank val categorieCode: String,
    val description: String = "",
    val requiredPermissions: List<String> = emptyList()
)

data class ModuleResponse(
    val id: String,
    val code: String,
    val nom: String,
    val categorieCode: String,
    val description: String,
    val isCore: Boolean,
    val requiredPlan: String,
    val isActive: Boolean
)

// ── Plan ─────────────────────────────────────────────────────────
data class CreatePlanRequest(
    @field:NotBlank val nom: String,
    val maxEcoles: Int = 100,
    val maxUtilisateurs: Int = 1000,
    val modulesIncluded: List<String> = emptyList(),
    val categoriesIncluded: List<String> = emptyList(),
    val dureeJours: Int = 365
)

data class PlanResponse(
    val id: String,
    val nom: String,
    val maxEcoles: Int,
    val maxUtilisateurs: Int,
    val modulesIncluded: List<String>,
    val categoriesIncluded: List<String>,
    val dureeJours: Int
)
