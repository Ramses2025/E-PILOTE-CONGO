package cg.epilote.backend.admin

// ── Briques communes ─────────────────────────────────────────────
data class LabelCountDto(val label: String, val count: Long)

// ── Scolarisation KPI ────────────────────────────────────────────
data class ScolarisationKpiResponse(
    val nbEcoles: Long,
    val nbUtilisateurs: Long,
    val ecolesByProvince: List<LabelCountDto>,
    val ecolesByNiveau: List<LabelCountDto>,
    val usersPerEcole: List<LabelCountDto>
)

// ── Finance KPI ──────────────────────────────────────────────────
data class FinanceStatutDto(
    val statut: String,
    val count: Long,
    val totalXAF: Long
)

data class FinanceKpiResponse(
    val totalXAF: Long,
    val paidXAF: Long,
    val dueXAF: Long,
    val nbFactures: Long,
    val nbOverdue: Long,
    val tauxRecouvrement: Int,
    val byStatut: List<FinanceStatutDto>
)

// ── RH KPI ───────────────────────────────────────────────────────
data class RhKpiResponse(
    val nbTotal: Long,
    val byRole: List<LabelCountDto>,
    val usersPerEcole: List<LabelCountDto>
)

// ── Réponse unifiée par module ────────────────────────────────────
data class ModuleKpiResponse(
    val category: String,
    val scolarisation: ScolarisationKpiResponse? = null,
    val finance: FinanceKpiResponse? = null,
    val rh: RhKpiResponse? = null
)
