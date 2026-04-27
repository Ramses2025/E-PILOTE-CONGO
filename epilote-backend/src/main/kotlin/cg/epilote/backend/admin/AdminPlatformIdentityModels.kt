package cg.epilote.backend.admin

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

/**
 * Identité juridique de la plateforme (éditeur), utilisée comme émetteur sur les factures
 * et autres documents officiels.
 *
 * Tous les champs sont optionnels à l'enregistrement — l'utilisateur Super Admin remplit
 * ce formulaire progressivement depuis le dashboard. Tant qu'un champ n'est pas rempli,
 * les PDF affichent un placeholder « À compléter dans Paramètres plateforme » plutôt qu'une
 * chaîne vide, afin que l'édition de la facture reste techniquement possible même sans
 * toutes les informations juridiques.
 *
 * Stocké dans Couchbase : collection `config`, document id `config::platform_identity`,
 * type `config_platform_identity`.
 */
data class PlatformIdentity(
    val raisonSociale: String = "",
    val rccm: String = "",
    val niu: String = "",
    val siege: String = "",
    val city: String = "",
    val country: String = "Congo",
    val phone: String = "",
    @field:Email
    val email: String = "",
    val website: String = "",
    /** Logo encodé en base64 (data URI `data:image/png;base64,...`) — pattern déjà utilisé pour les logos groupes. */
    @field:Size(max = 3_000_000)
    val logoBase64: String = "",
    /** Taux TVA en pourcentage (0 = exonéré). */
    val tvaRate: Double = 0.0,
    val tvaExempted: Boolean = true,
    /** Texte libre (ex. "Paiement à réception en espèces, chèque ou virement"). */
    val paymentTerms: String = "",
    /** Ex. "Tribunal de Commerce de Brazzaville". */
    val competentCourt: String = "",
    val iban: String = "",
    val bankName: String = "",
    /** Numéro MTN Mobile Money plateforme — prêt pour câblage futur. */
    val mtnMomoNumber: String = "",
    /** Numéro Airtel Money plateforme — prêt pour câblage futur. */
    val airtelMoneyNumber: String = "",
    /**
     * Format de numérotation des factures. `{YYYY}` → année courante, `{NNNNNN}` → compteur
     * atomique annuel (remis à 1 chaque année, 6 chiffres avec padding).
     * Par défaut : `FAC-{YYYY}-{NNNNNN}`.
     */
    val invoiceNumberFormat: String = "FAC-{YYYY}-{NNNNNN}",
    /** Mentions légales affichées en pied de facture. */
    val legalMentions: String = "",
    val updatedAt: Long = 0L
)

data class UpdatePlatformIdentityRequest(
    val raisonSociale: String? = null,
    val rccm: String? = null,
    val niu: String? = null,
    val siege: String? = null,
    val city: String? = null,
    val country: String? = null,
    val phone: String? = null,
    @field:Email val email: String? = null,
    val website: String? = null,
    @field:Size(max = 3_000_000) val logoBase64: String? = null,
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
