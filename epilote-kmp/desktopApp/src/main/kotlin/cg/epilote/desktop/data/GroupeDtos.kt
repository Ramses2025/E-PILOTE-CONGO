package cg.epilote.desktop.data

import kotlinx.serialization.Serializable

// ── ADMIN_GROUPE — Dashboard Stats ───────────────────────────────────────────

@Serializable
data class GroupeDashboardStatsDto(
    val groupeId: String = "",
    val groupeNom: String = "",
    val province: String = "",
    val planId: String = "",
    val planNom: String = "",
    val planType: String = "gratuit",
    val prixXAF: Long = 0,
    val abonnementStatut: String = "pending",
    val abonnementDateDebut: Long = 0,
    val abonnementDateFin: Long = 0,
    val renouvellementAuto: Boolean = false,
    val nbEcoles: Long = 0,
    val nbUtilisateurs: Long = 0,
    val nbProfils: Long = 0,
    val nbModulesActifs: Long = 0,
    val modulesActifs: List<String> = emptyList(),
    val ecoles: List<EcoleApiDto> = emptyList(),
    val facturationTotaleXAF: Long = 0,
    val montantPayeXAF: Long = 0,
    val montantDuXAF: Long = 0,
    val nbFactures: Long = 0,
    val nbFacturesEnRetard: Long = 0,
    val derniereFacture: InvoiceApiDto? = null
)

// ── ADMIN_GROUPE — Entités ───────────────────────────────────────────────────

@Serializable
data class EcoleApiDto(
    val id: String = "",
    val groupId: String = "",
    val nom: String = "",
    val province: String = "",
    val territoire: String = "",
    val niveaux: List<String> = emptyList(),
    val planId: String = "",
    val createdAt: Long = 0
)

@Serializable
data class ProfilPermissionDto(
    val moduleSlug: String = "",
    val canRead: Boolean = true,
    val canWrite: Boolean = false,
    val canDelete: Boolean = false,
    val canExport: Boolean = false
)

@Serializable
data class ProfilApiDto(
    val id: String = "",
    val groupId: String = "",
    val nom: String = "",
    val permissions: List<ProfilPermissionDto> = emptyList(),
    val createdAt: Long = 0
)

// ── ADMIN_GROUPE — Requêtes ──────────────────────────────────────────────────

@Serializable
data class SubscriptionRequestDto(
    val type: String,
    val message: String? = null
)

@Serializable
data class CreateEcoleDto(
    val nom: String,
    val province: String,
    val territoire: String,
    val niveaux: List<String> = listOf("primaire")
)

@Serializable
data class CreateProfilDto(
    val nom: String,
    val permissions: List<ProfilPermissionDto> = emptyList()
)

@Serializable
data class CreateUserGroupeDto(
    val password: String,
    val nom: String,
    val prenom: String,
    val email: String,
    val schoolId: String,
    val profilId: String,
    val username: String? = null
)

@Serializable
data class AssignProfilDto(val profilId: String)

// ── ADMIN_GROUPE — Categories + Modules dynamiques ──────────────────────────

@Serializable
data class CategorieWithModulesDto(
    val code: String = "",
    val nom: String = "",
    val ordre: Int = 0,
    val modules: List<ModuleApiDto> = emptyList()
)

// ── ADMIN_GROUPE — Timeline factures ────────────────────────────────────────

@Serializable
data class MonthlyInvoiceStatsDto(
    val month: String = "",
    val totalXAF: Long = 0,
    val paidXAF: Long = 0,
    val count: Long = 0
)
