package cg.epilote.desktop.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ── IA DTOs ──────────────────────────────────────────────────────────────────

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

// ── Paramètres plateforme (identité juridique) ────────────────────────────────

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

// ── Paiements présentiels ──────────────────────────────────────────────────────

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
    val idempotencyKey: String? = null,
    val invoiceId: String? = null
)

@Serializable
data class PaymentMethodDto(
    val code: String = "",
    val label: String = "",
    val enabled: Boolean = false
)

// ── Audit Logs (journal serveur cloud-only, RGPD/CADF) ───────────────────────

@Serializable
data class AuditEventApiDto(
    val id: String = "",
    val timestamp: Long = 0L,
    val action: String = "",
    val actionLabel: String = "",
    val category: String = "",
    val outcome: String = "",
    val actorId: String? = null,
    val actorEmail: String? = null,
    val actorRole: String? = null,
    val targetType: String? = null,
    val targetId: String? = null,
    val targetLabel: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val message: String? = null,
    val metadata: Map<String, JsonElement> = emptyMap()
)

@Serializable
data class AuditLogPageDto(
    val items: List<AuditEventApiDto> = emptyList(),
    val total: Long = 0,
    val page: Int = 1,
    val pageSize: Int = 50
)

@Serializable
data class AuditActionDto(
    val code: String = "",
    val category: String = "",
    val label: String = ""
)
