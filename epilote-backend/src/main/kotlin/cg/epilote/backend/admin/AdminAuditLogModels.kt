package cg.epilote.backend.admin

/**
 * Journal d'audit serveur — trace immuable de chaque action mutante exécutée
 * sur la plateforme par un Super Admin (ou par le système : scheduler,
 * job d'expiration, etc.).
 *
 * Stocké dans la collection Couchbase `audit_logs` (cloud-only, jamais
 * synchronisé sur les bases mobiles — voir `epilote-infra/setup-all-in-one.sql`
 * commentaire « audit_logs + ledger_entries = cloud only, pas sur CBLite »).
 *
 * Modèle inspiré du standard CADF / OpenStack pour les évenements d'audit :
 * https://www.dmtf.org/sites/default/files/standards/documents/DSP2038_1.0.0.pdf
 *
 * Référence Couchbase Kotlin SDK (KV upsert + N1QL pagination) :
 *  - https://docs.couchbase.com/kotlin-sdk/current/howtos/kv-operations.html
 *  - https://docs.couchbase.com/kotlin-sdk/current/howtos/n1ql-queries-with-sdk.html#paging
 */
enum class AuditCategory(val code: String) {
    AUTH("auth"),
    GROUPE("groupe"),
    ADMIN("admin"),
    SUBSCRIPTION("subscription"),
    INVOICE("invoice"),
    PAYMENT("payment"),
    PLATFORM("platform"),
    SYSTEM("system");
}

enum class AuditOutcome(val code: String) {
    SUCCESS("success"),
    FAILURE("failure");
}

/**
 * Catalogue centralisé des actions auditées. Toute nouvelle action doit y être
 * ajoutée pour garantir la cohérence des libellés côté UI (filtres, recherche).
 */
enum class AuditAction(val code: String, val category: AuditCategory, val label: String) {
    LOGIN_SUCCESS("auth.login.success", AuditCategory.AUTH, "Connexion réussie"),
    LOGIN_FAILURE("auth.login.failure", AuditCategory.AUTH, "Échec de connexion"),
    PASSWORD_CHANGED("auth.password.changed", AuditCategory.AUTH, "Changement de mot de passe"),
    PASSWORD_RESET("auth.password.reset", AuditCategory.AUTH, "Réinitialisation administrative du mot de passe"),

    GROUPE_CREATED("groupe.created", AuditCategory.GROUPE, "Création d'un groupe scolaire"),
    GROUPE_UPDATED("groupe.updated", AuditCategory.GROUPE, "Modification d'un groupe scolaire"),
    GROUPE_DELETED("groupe.deleted", AuditCategory.GROUPE, "Suppression d'un groupe scolaire"),

    ADMIN_CREATED("admin.created", AuditCategory.ADMIN, "Création d'un administrateur"),
    ADMIN_UPDATED("admin.updated", AuditCategory.ADMIN, "Modification d'un administrateur"),
    ADMIN_DELETED("admin.deleted", AuditCategory.ADMIN, "Suppression d'un administrateur"),

    SUBSCRIPTION_CREATED("subscription.created", AuditCategory.SUBSCRIPTION, "Création d'un abonnement"),
    SUBSCRIPTION_STATUS_CHANGED("subscription.status_changed", AuditCategory.SUBSCRIPTION, "Changement de statut d'abonnement"),
    SUBSCRIPTION_RENEWED("subscription.renewed", AuditCategory.SUBSCRIPTION, "Renouvellement d'abonnement"),
    SUBSCRIPTION_AUTO_SUSPENDED("subscription.auto_suspended", AuditCategory.SUBSCRIPTION, "Suspension automatique (expiration)"),

    INVOICE_CREATED("invoice.created", AuditCategory.INVOICE, "Émission d'une facture"),
    INVOICE_STATUS_CHANGED("invoice.status_changed", AuditCategory.INVOICE, "Changement de statut de facture"),
    INVOICE_PDF_GENERATED("invoice.pdf_generated", AuditCategory.INVOICE, "Téléchargement PDF facture"),

    PAYMENT_RECORDED("payment.recorded", AuditCategory.PAYMENT, "Enregistrement d'un paiement présentiel"),

    PLATFORM_IDENTITY_UPDATED("platform.identity_updated", AuditCategory.PLATFORM, "Mise à jour des paramètres plateforme"),

    SCHEDULER_EXPIRY_RUN("system.scheduler.expiry_run", AuditCategory.SYSTEM, "Exécution du job d'expiration des abonnements");

    companion object {
        fun fromCode(code: String?): AuditAction? = values().firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

/**
 * Représentation publique d'un événement d'audit (renvoyée par l'API).
 *
 * `metadata` est une carte libre (clé → valeur stringifiable) destinée à
 * conserver le contexte additionnel (anciennes/nouvelles valeurs, raison,
 * etc.) sans imposer de schéma rigide.
 */
data class AuditEventResponse(
    val id: String,
    val timestamp: Long,
    val action: String,
    val actionLabel: String,
    val category: String,
    val outcome: String,
    val actorId: String?,
    val actorEmail: String?,
    val actorRole: String?,
    val targetType: String?,
    val targetId: String?,
    val targetLabel: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val message: String?,
    val metadata: Map<String, Any?> = emptyMap()
)

data class AuditLogPage(
    val items: List<AuditEventResponse>,
    val total: Long,
    val page: Int,
    val pageSize: Int
)
