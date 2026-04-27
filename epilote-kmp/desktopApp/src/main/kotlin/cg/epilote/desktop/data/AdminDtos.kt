package cg.epilote.desktop.data

import kotlinx.serialization.Serializable

// ── Dashboard Stats ─────────────────────────────────────────────────────────

@Serializable
data class DashboardStatsDto(
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

@Serializable
data class ProvinceStatsDto(
    val province: String = "",
    val groupesCount: Long = 0,
    val ecolesCount: Long = 0
)

@Serializable
data class PlanDistributionDto(
    val planId: String = "",
    val planNom: String = "",
    val groupesCount: Long = 0
)

// ── Entités API ─────────────────────────────────────────────────────────────

@Serializable
data class InvoiceApiDto(
    val id: String = "",
    val groupeId: String = "",
    val subscriptionId: String = "",
    val montantXAF: Long = 0,
    val statut: String = "draft",
    val dateEmission: Long = 0,
    val dateEcheance: Long = 0,
    val datePaiement: Long? = null,
    val reference: String = "",
    val notes: String = ""
)

@Serializable
data class CreateInvoiceDto(
    val groupeId: String,
    val subscriptionId: String,
    val montantXAF: Long,
    val dateEcheance: Long,
    val notes: String = ""
)

@Serializable
data class AnnouncementApiDto(
    val id: String = "",
    val titre: String = "",
    val contenu: String = "",
    val cible: String = "all",
    val createdBy: String = "",
    val createdAt: Long = 0
)

@Serializable
data class CreateAnnouncementDto(
    val titre: String,
    val contenu: String,
    val cible: String = "all"
)

@Serializable
data class GroupeApiDto(
    val id: String = "",
    val nom: String = "",
    val slug: String = "",
    val email: String? = null,
    val phone: String? = null,
    val department: String? = null,
    val city: String? = null,
    val address: String? = null,
    val country: String = "Congo",
    val logo: String? = null,
    val description: String? = null,
    val foundedYear: Int? = null,
    val website: String? = null,
    val planId: String = "",
    val ecolesCount: Int = 0,
    val usersCount: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = 0
)

@Serializable
data class PlanApiDto(
    val id: String = "",
    val nom: String = "",
    val type: String = "gratuit",
    val prixXAF: Long = 0,
    val currency: String = "XAF",
    val maxStudents: Int = 0,
    val maxPersonnel: Int = 0,
    val modulesIncluded: List<String> = emptyList(),
    val isActive: Boolean = true
)

@Serializable
data class CreatePlanDto(
    val nom: String,
    val type: String,
    val prixXAF: Long = 0,
    val maxStudents: Int = 100,
    val maxPersonnel: Int = 10,
    val modulesIncluded: List<String> = emptyList(),
    val isActive: Boolean = true
)

@Serializable
data class UpdatePlanDto(
    val nom: String? = null,
    val type: String? = null,
    val prixXAF: Long? = null,
    val maxStudents: Int? = null,
    val maxPersonnel: Int? = null,
    val modulesIncluded: List<String>? = null,
    val isActive: Boolean? = null
)

@Serializable
data class SubscriptionApiDto(
    val id: String = "",
    val groupeId: String = "",
    val planId: String = "",
    val statut: String = "active",
    val dateDebut: Long = 0,
    val dateFin: Long = 0,
    val renouvellementAuto: Boolean = false,
    val createdAt: Long = 0
)

@Serializable
data class CreateSubscriptionDto(
    val groupeId: String,
    val planId: String,
    val renouvellementAuto: Boolean = false
)

@Serializable
data class ModuleApiDto(
    val id: String = "",
    val code: String = "",
    val nom: String = "",
    val categorieCode: String = "",
    val description: String = "",
    val isCore: Boolean = false,
    val requiredPlan: String = "gratuit",
    val isActive: Boolean = true,
    val ordre: Int = 0
)

@Serializable
data class CreateModuleDto(
    val code: String,
    val nom: String,
    val categorieCode: String,
    val description: String = "",
    val requiredPlan: String = "gratuit",
    val isCore: Boolean = false,
    val ordre: Int = 0,
    val requiredPermissions: List<String> = emptyList()
)

@Serializable
data class UpdateModuleDto(
    val nom: String? = null,
    val categorieCode: String? = null,
    val description: String? = null,
    val requiredPlan: String? = null,
    val isCore: Boolean? = null,
    val ordre: Int? = null,
    val isActive: Boolean? = null
)

@Serializable
data class CategorieApiDto(
    val code: String = "",
    val nom: String = "",
    val isCore: Boolean = false,
    val ordre: Int = 0,
    val isActive: Boolean = true
)

@Serializable
data class CreateCategorieDto(
    val code: String,
    val nom: String,
    val isCore: Boolean = false,
    val ordre: Int = 0
)

@Serializable
data class UpdateCategorieDto(
    val nom: String? = null,
    val isCore: Boolean? = null,
    val ordre: Int? = null,
    val isActive: Boolean? = null
)

@Serializable
data class UserApiDto(
    val id: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val schoolId: String? = null,
    val groupId: String = "",
    val profilId: String? = null,
    val role: String = "USER",
    val isActive: Boolean = true,
    val createdAt: Long = 0
)

@Serializable
data class CreateGroupeDto(
    val nom: String,
    val planId: String,
    val email: String? = null,
    val phone: String? = null,
    val department: String? = null,
    val city: String? = null,
    val address: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val foundedYear: Int? = null,
    val website: String? = null,
    val isActive: Boolean? = null
)

@Serializable
data class UpdateGroupeDto(
    val nom: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val department: String? = null,
    val city: String? = null,
    val address: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val foundedYear: Int? = null,
    val website: String? = null,
    val planId: String? = null,
    val isActive: Boolean? = null
)

@Serializable
data class CreateAdminGroupeDto(val password: String, val nom: String, val prenom: String, val email: String)

// ── Admin Users (Super Admin scope) ─────────────────────────────────────────

@Serializable
data class AdminUserApiDto(
    val id: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String? = null,
    val role: String = "USER",
    val status: String = "active",
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val groupId: String? = null,
    val schoolId: String? = null,
    val avatar: String? = null,
    val address: String? = null,
    val birthPlace: String? = null,
    val mustChangePassword: Boolean = false,
    val lastLoginAt: Long? = null,
    val loginAttempts: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

@Serializable
data class CreateAdminUserDto(
    val role: String,
    val password: String,
    val nom: String,
    val prenom: String,
    val email: String,
    val phone: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val address: String? = null,
    val birthPlace: String? = null,
    val avatar: String? = null,
    val groupId: String? = null,
    val mustChangePassword: Boolean = true
)

@Serializable
data class UpdateAdminUserDto(
    val nom: String? = null,
    val prenom: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    val dateOfBirth: String? = null,
    val address: String? = null,
    val birthPlace: String? = null,
    val avatar: String? = null,
    val mustChangePassword: Boolean? = null
)

@Serializable
data class ToggleAdminStatusDto(val status: String)

// ── IA DTOs ─────────────────────────────────────────────────────────────────

@Serializable
data class AIContentRequestDto(
    val titre: String,
    val niveau: String,
    val filiere: String = "",
    val type: String = "COURS",
    val matiere: String = "",
    val dureeMinutes: Int = 60,
    val context: String = "Congo/MEPSA"
)

@Serializable
data class AIContentResponseDto(
    val contenu: String,
    val titre: String,
    val type: String,
    val niveau: String,
    val tokensUtilises: Int = 0,
    val modele: String = "",
    val fallback: Boolean = false
)

@Serializable
data class AIAppreciationRequestDto(
    val eleveNom: String,
    val genre: String = "M",
    val moyenneGenerale: Double,
    val moyenneMin: Double = 0.0,
    val moyenneMax: Double = 20.0,
    val rang: Int = 0,
    val effectif: Int = 0,
    val absences: Int = 0,
    val comportement: String = "correct"
)

@Serializable
data class AIAppreciationResponseDto(
    val appreciation: String,
    val mention: String,
    val conseil: String = "",
    val fallback: Boolean = false
)

// ── Paramètres plateforme (identité juridique) ─────────────────────────────
// Documents miroirs de PlatformIdentity côté backend. Utilisés par la page
// « Paramètres plateforme » (Super Admin) pour configurer les mentions légales
// qui apparaissent sur chaque facture PDF.
@Serializable
data class PlatformIdentityDto(
    val raisonSociale: String = "",
    val rccm: String = "",
    val niu: String = "",
    val siege: String = "",
    val city: String = "",
    val country: String = "Congo",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val logoBase64: String = "",
    val tvaRate: Double = 0.0,
    val tvaExempted: Boolean = true,
    val paymentTerms: String = "",
    val competentCourt: String = "",
    val iban: String = "",
    val bankName: String = "",
    val mtnMomoNumber: String = "",
    val airtelMoneyNumber: String = "",
    val invoiceNumberFormat: String = "FAC-{YYYY}-{NNNNNN}",
    val legalMentions: String = "",
    val updatedAt: Long = 0L
)

@Serializable
data class UpdatePlatformIdentityDto(
    val raisonSociale: String? = null,
    val rccm: String? = null,
    val niu: String? = null,
    val siege: String? = null,
    val city: String? = null,
    val country: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val logoBase64: String? = null,
    val tvaRate: Double? = null,
    val tvaExempted: Boolean? = null,
    val paymentTerms: String? = null,
    val competentCourt: String? = null,
    val iban: String? = null,
    val bankName: String? = null,
    val mtnMomoNumber: String? = null,
    val airtelMoneyNumber: String? = null,
    val invoiceNumberFormat: String? = null,
    val legalMentions: String? = null
)

// ── Paiements présentiels ──────────────────────────────────────────────────
// Un PaymentReceipt est créé à chaque fois que le Super Admin enregistre un
// paiement reçu en espèces/chèque/virement au siège.
@Serializable
data class PaymentReceiptDto(
    val id: String = "",
    val groupeId: String = "",
    val subscriptionId: String = "",
    val invoiceId: String? = null,
    val montantXAF: Long = 0,
    val paymentMethod: String = "",
    val paymentMethodLabel: String = "",
    val externalReference: String? = null,
    val paidBy: String? = null,
    val receivedBy: String = "",
    val accessStart: Long = 0,
    val accessEnd: Long = 0,
    val notes: String = "",
    val receivedAt: Long = 0
)

@Serializable
data class RecordPaymentDto(
    val groupeId: String,
    val subscriptionId: String,
    val montantXAF: Long,
    val paymentMethod: String,
    val durationMonths: Int = 12,
    val externalReference: String? = null,
    val paidBy: String? = null,
    val notes: String = "",
    /**
     * Clé d'idempotence (UUID) générée côté desktop au moment d'ouvrir le dialogue
     * de saisie. Si le clic `Enregistrer` est envoyé deux fois (retry réseau,
     * double-clic, timeout), le backend retourne le même reçu au lieu de créer
     * une facture doublon. Pattern documenté Stripe.
     */
    val idempotencyKey: String? = null
)

@Serializable
data class PaymentMethodDto(
    val code: String = "",
    val label: String = "",
    val enabled: Boolean = false
)
