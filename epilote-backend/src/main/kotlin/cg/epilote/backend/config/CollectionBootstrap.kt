package cg.epilote.backend.config

import com.couchbase.client.core.error.CollectionExistsException
import com.couchbase.client.kotlin.Bucket
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Crée de manière idempotente toutes les collections référencées par le backend
 * dans le scope `_default` du bucket applicatif.
 *
 * Conforme à la doc officielle Couchbase Kotlin SDK :
 *  https://docs.couchbase.com/kotlin-sdk/current/howtos/provisioning-cluster-resources.html#collection-management
 *
 * Les collections ajoutées par la PR #2 (`payment_receipts`, `invoice_counters`,
 * `config`) ne sont pas créées côté Capella sur les clusters existants. Sans
 * bootstrap, tout appel `recordPayment` échoue en `UNKNOWN_COLLECTION` / TIMEOUT.
 */
@Component
class CollectionBootstrap(
    private val bucket: Bucket
) {

    private val log = LoggerFactory.getLogger(CollectionBootstrap::class.java)

    /**
     * Liste exhaustive des collections utilisées dans le scope `_default`.
     * Toute nouvelle collection doit être ajoutée ici pour garantir sa présence
     * côté Capella au démarrage.
     */
    private val requiredCollections = listOf(
        // Identité / RBAC
        "users",
        "profils",
        // Tenancy
        "schools",
        "school_groups",  // collection primaire des groupes scolaires (cf. AdminRepository.GROUPS_COLLECTION)
        "groupes",        // legacy : conservé pour compat ascendante avec docs existants
        // Catalogue & abonnements
        "modules",
        "categories",
        "plans",
        "subscriptions",
        // Facturation
        "invoices",
        "invoices_platform",
        "invoice_counters",
        "payment_receipts",
        // Communications
        "announcements",
        "announcements_platform",
        "messages",
        // Configuration et journal
        "config",
        "audit_logs",
    )

    @EventListener(ApplicationReadyEvent::class)
    fun ensureCollections() = runBlocking {
        val mgr = bucket.collections
        for (name in requiredCollections) {
            try {
                mgr.createCollection(scopeName = "_default", collectionName = name)
                log.info("Collection `{}` créée (scope _default).", name)
            } catch (e: CollectionExistsException) {
                log.debug("Collection `{}` déjà présente — OK.", name)
            } catch (e: Exception) {
                // On log mais on ne bloque pas le démarrage : si Capella refuse
                // (quota, permission), les endpoints qui en dépendent lèveront
                // une erreur claire au moment de l'usage.
                log.warn("Impossible de créer la collection `${name}` : ${e.javaClass.simpleName} — ${e.message}")
            }
        }
    }
}
