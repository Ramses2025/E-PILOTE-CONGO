package cg.epilote.backend.admin

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

/**
 * Reçu de paiement manuel enregistré par le Super Admin lorsqu'un groupe scolaire
 * se déplace physiquement au siège et règle son abonnement.
 *
 * Modes de paiement supportés :
 *  - `cash`           : espèces (présentiel)
 *  - `check`          : chèque
 *  - `bank_transfer`  : virement bancaire (réception confirmée manuellement)
 *  - `mobile_money`   : // TODO: Mobile Money — câblage futur, option exposée mais non implémentée
 *  - `card`           : // TODO: Carte bancaire — câblage futur, option exposée mais non implémentée
 */
enum class PaymentMethod(val code: String, val enabled: Boolean, val label: String) {
    CASH("cash", true, "Espèces"),
    CHECK("check", true, "Chèque"),
    BANK_TRANSFER("bank_transfer", true, "Virement bancaire"),

    // TODO: Mobile Money — infrastructure prête, implémentation à venir
    MOBILE_MONEY("mobile_money", false, "Mobile Money (à venir)"),

    // TODO: Carte bancaire — infrastructure prête, implémentation à venir
    CARD("card", false, "Carte bancaire (à venir)");

    companion object {
        fun fromCode(code: String?): PaymentMethod? =
            values().firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

data class RecordPaymentRequest(
    @field:NotBlank val groupeId: String,
    @field:NotBlank val subscriptionId: String,
    @field:Positive val montantXAF: Long,
    /** Code du mode de paiement (voir [PaymentMethod]). */
    @field:NotBlank val paymentMethod: String,
    /**
     * Durée d'abonnement à activer après paiement, en mois.
     * Par défaut 12 (abonnement annuel — business rule actuelle).
     */
    @field:Positive val durationMonths: Int = 12,
    /** Optionnel : référence externe (n° de chèque, référence virement, ticket MoMo…). */
    val externalReference: String? = null,
    /** Nom de la personne qui a réglé au nom du groupe (ex. directeur). */
    val paidBy: String? = null,
    val notes: String = ""
)

data class PaymentReceiptResponse(
    val id: String,
    val groupeId: String,
    val subscriptionId: String,
    val invoiceId: String?,
    val montantXAF: Long,
    val paymentMethod: String,
    val paymentMethodLabel: String,
    val externalReference: String?,
    val paidBy: String?,
    val receivedBy: String,
    val notes: String,
    val accessStart: Long,
    val accessEnd: Long,
    val receivedAt: Long
)
