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
    val totalSubscriptions: Long = 0,
    val activeSubscriptions: Long = 0,
    val totalInvoices: Long = 0,
    val revenueTotal: Long = 0,
    val revenuePaid: Long = 0,
    val invoicesOverdue: Long = 0,
    val plans: List<PlanResponse>,
    val categories: List<CategorieInfo>,
    val groupesByProvince: List<ProvinceStats> = emptyList(),
    val planDistribution: List<PlanDistribution> = emptyList(),
    val subscriptionsByStatus: Map<String, Long> = emptyMap(),
    val recentGroupes: List<GroupeResponse> = emptyList(),
    val recentInvoices: List<InvoiceResponse> = emptyList()
)

data class ProvinceStats(
    val province: String,
    val groupesCount: Long,
    val ecolesCount: Long
)

data class PlanDistribution(
    val planId: String,
    val planNom: String,
    val groupesCount: Long
)

// ── Catégories (CRUD dynamique) ─────────────────────────────────

data class CategorieInfo(val code: String, val nom: String, val isCore: Boolean, val ordre: Int, val isActive: Boolean = true)

data class CreateCategorieRequest(
    @field:NotBlank val code: String,
    @field:NotBlank val nom: String,
    val isCore: Boolean = false,
    val ordre: Int = 0
)

data class UpdateCategorieRequest(
    val nom: String? = null,
    val isCore: Boolean? = null,
    val ordre: Int? = null,
    val isActive: Boolean? = null
)

// ── Exceptions métier ────────────────────────────────────────────

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidModuleForPlanException(slugs: List<String>) :
    RuntimeException("Modules hors plan : ${slugs.joinToString(", ")}")

@ResponseStatus(HttpStatus.CONFLICT)
class PlanLimitExceededException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.FORBIDDEN)
class SubscriptionExpiredException(message: String) : RuntimeException(message)

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
    val groupId: String,
    val nom: String,
    val province: String,
    val territoire: String,
    val niveaux: List<String>,
    val planId: String,
    val createdAt: Long
)

// ── Utilisateur ──────────────────────────────────────────────────
data class CreateUserRequest(
    val username: String? = null,
    @field:NotBlank @field:Size(min = 8) val password: String,
    @field:NotBlank val nom: String,
    @field:NotBlank val prenom: String,
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val schoolId: String,
    @field:NotBlank val profilId: String
)

data class CreateAdminGroupeRequest(
    val username: String? = null,
    @field:NotBlank @field:Size(min = 8) val password: String,
    @field:NotBlank val nom: String,
    @field:NotBlank val prenom: String,
    @field:NotBlank @field:Email val email: String
)

data class UserResponse(
    val id: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val schoolId: String?,
    val groupId: String,
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
    val groupId: String,
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
    val prixXAF: Long = 0,
    val maxEcoles: Int = 100,
    val maxUtilisateurs: Int = 1000,
    val modulesIncluded: List<String> = emptyList(),
    val categoriesIncluded: List<String> = emptyList(),
    val dureeJours: Int = 365
)

data class UpdatePlanRequest(
    val nom: String? = null,
    val prixXAF: Long? = null,
    val maxEcoles: Int? = null,
    val maxUtilisateurs: Int? = null,
    val modulesIncluded: List<String>? = null,
    val categoriesIncluded: List<String>? = null,
    val dureeJours: Int? = null,
    val isActive: Boolean? = null
)

data class PlanResponse(
    val id: String,
    val nom: String,
    val prixXAF: Long,
    val maxEcoles: Int,
    val maxUtilisateurs: Int,
    val modulesIncluded: List<String>,
    val categoriesIncluded: List<String>,
    val dureeJours: Int,
    val isActive: Boolean = true
)

// ── Module (update) ──────────────────────────────────────────────
data class UpdateModuleRequest(
    val nom: String? = null,
    val categorieCode: String? = null,
    val description: String? = null,
    val isActive: Boolean? = null
)

// ── Groupe (update / lifecycle) ──────────────────────────────────
data class UpdateGroupeRequest(
    val nom: String? = null,
    val province: String? = null,
    val planId: String? = null,
    val isActive: Boolean? = null
)

// ── Abonnement ───────────────────────────────────────────────────
data class CreateSubscriptionRequest(
    @field:NotBlank val groupeId: String,
    @field:NotBlank val planId: String,
    val renouvellementAuto: Boolean = false
)

data class SubscriptionResponse(
    val id: String,
    val groupeId: String,
    val planId: String,
    val statut: String,
    val dateDebut: Long,
    val dateFin: Long,
    val renouvellementAuto: Boolean,
    val createdAt: Long
)

// ── Facture Plateforme ───────────────────────────────────────────
data class CreateInvoiceRequest(
    @field:NotBlank val groupeId: String,
    @field:NotBlank val subscriptionId: String,
    val montantXAF: Long,
    val dateEcheance: Long,
    val notes: String = ""
)

data class InvoiceResponse(
    val id: String,
    val groupeId: String,
    val subscriptionId: String,
    val montantXAF: Long,
    val statut: String,
    val dateEmission: Long,
    val dateEcheance: Long,
    val datePaiement: Long?,
    val reference: String,
    val notes: String
)

// ── Annonce Globale ──────────────────────────────────────────────
data class CreateAnnouncementRequest(
    @field:NotBlank val titre: String,
    @field:NotBlank val contenu: String,
    val cible: String = "all"
)

data class AnnouncementResponse(
    val id: String,
    val titre: String,
    val contenu: String,
    val cible: String,
    val createdBy: String,
    val createdAt: Long
)
