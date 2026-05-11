package cg.epilote.backend.admin

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// ── Groupe Dashboard Stats (ADMIN_GROUPE) ────────────────────────
data class GroupeDashboardStatsResponse(
    val groupeId: String,
    val groupeNom: String,
    val province: String,
    val planId: String,
    val planNom: String,
    val planType: String,
    val prixXAF: Long,
    val abonnementStatut: String,
    val abonnementDateDebut: Long,
    val abonnementDateFin: Long,
    val renouvellementAuto: Boolean,
    val nbEcoles: Long,
    val nbUtilisateurs: Long,
    val nbProfils: Long,
    val nbModulesActifs: Long,
    val modulesActifs: List<String>,
    val ecoles: List<EcoleResponse>,
    val facturationTotaleXAF: Long,
    val montantPayeXAF: Long,
    val montantDuXAF: Long,
    val nbFactures: Long,
    val nbFacturesEnRetard: Long,
    val derniereFacture: InvoiceResponse?
)

// ── Demande abonnement (ADMIN_GROUPE) ─────────────────────────────
data class SubscriptionRequestBody(
    @field:NotBlank val type: String,
    @field:Size(max = 500) val message: String? = null
)

// ── Sidebar dynamique (ADMIN_GROUPE) ─────────────────────────────
data class CategorieWithModulesResponse(
    val code: String,
    val nom: String,
    val ordre: Int,
    val modules: List<ModuleResponse>
)

// ── Timeline factures (ADMIN_GROUPE) ─────────────────────────────
data class MonthlyInvoiceStats(
    val month: String,
    val totalXAF: Long,
    val paidXAF: Long,
    val count: Long
)
