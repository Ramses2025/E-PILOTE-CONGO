package cg.epilote.backend.admin

import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.core.error.CasMismatchException
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

/**
 * Accès en lecture/écriture à l'identité plateforme (émetteur des factures).
 *
 * Document unique `config::platform_identity` dans la collection `config` — même pattern
 * que `config::plans`, `config::modules`, `config::categories`. Aucune nouvelle collection
 * n'est créée.
 *
 * Référence Couchbase SDK Kotlin (officielle) : https://docs.couchbase.com/kotlin-sdk/current/howtos/kv-operations.html
 */
@Repository
class AdminPlatformIdentityRepository(private val bucket: Bucket) {
    private val log = LoggerFactory.getLogger(AdminPlatformIdentityRepository::class.java)
    private val scope = runBlocking { bucket.defaultScope() }

    private companion object {
        const val CONFIG_COLLECTION = "config"
        const val DOC_ID = "config::platform_identity"
        const val DOC_TYPE = "config_platform_identity"
        const val MAX_CAS_RETRIES = 5
        /**
         * Tout format de numérotation de facture doit contenir au moins un bloc `{N+}`
         * (ex. `{N}`, `{NN}`, `{NNNNNN}`) — c'est ce bloc qui reçoit le numéro séquentiel
         * atomique via [AdminInvoiceCounterRepository]. Sans ce bloc, deux factures
         * différentes produiraient la même référence (non-conformité juridique).
         */
        val SEQUENCE_PLACEHOLDER: Regex = Regex("\\{N+}")
    }

    private val collections = java.util.concurrent.ConcurrentHashMap<String, Collection>()
    private fun col(name: String): Collection = collections.getOrPut(name) { runBlocking { scope.collection(name) } }

    suspend fun read(): PlatformIdentity {
        val doc = runCatching {
            col(CONFIG_COLLECTION).get(DOC_ID).contentAs<Map<String, Any?>>()
        }.getOrNull() ?: return PlatformIdentity()
        return mapToPlatformIdentity(doc)
    }

    /**
     * Met à jour l'identité plateforme avec CAS retry (optimistic locking).
     *
     * Pattern officiel Couchbase SDK Kotlin :
     * https://docs.couchbase.com/kotlin-sdk/current/howtos/kv-operations.html
     * (section « Compare and swap (CAS) »)
     *
     * Si le document n'existe pas encore, un upsert initial est effectué.
     */
    suspend fun update(req: UpdatePlatformIdentityRequest): PlatformIdentity {
        val now = System.currentTimeMillis()
        val collection = col(CONFIG_COLLECTION)

        val getResult = runCatching { collection.get(DOC_ID) }.getOrNull()

        if (getResult == null) {
            val next = applyRequest(PlatformIdentity(), req, now)
            collection.upsert(DOC_ID, buildPayload(next))
            return next
        }

        for (attempt in 1..MAX_CAS_RETRIES) {
            val currentGet = if (attempt == 1) getResult else collection.get(DOC_ID)
            val doc = currentGet.contentAs<Map<String, Any?>>()
            val current = mapToPlatformIdentity(doc)
            val next = applyRequest(current, req, now)
            try {
                collection.replace(DOC_ID, buildPayload(next), cas = currentGet.cas)
                return next
            } catch (_: CasMismatchException) {
                log.info("CAS mismatch on platform identity update, retry {}/{}", attempt, MAX_CAS_RETRIES)
            }
        }
        throw IllegalStateException("Failed to update platform identity after $MAX_CAS_RETRIES CAS retries")
    }

    private fun mapToPlatformIdentity(doc: Map<String, Any?>): PlatformIdentity = PlatformIdentity(
        raisonSociale = doc["raisonSociale"] as? String ?: "",
        rccm = doc["rccm"] as? String ?: "",
        niu = doc["niu"] as? String ?: "",
        siege = doc["siege"] as? String ?: "",
        city = doc["city"] as? String ?: "",
        country = doc["country"] as? String ?: "Congo",
        phone = doc["phone"] as? String ?: "",
        email = doc["email"] as? String ?: "",
        website = doc["website"] as? String ?: "",
        logoBase64 = doc["logoBase64"] as? String ?: "",
        tvaRate = (doc["tvaRate"] as? Number)?.toDouble() ?: 0.0,
        tvaExempted = doc["tvaExempted"] as? Boolean ?: true,
        paymentTerms = doc["paymentTerms"] as? String ?: "",
        competentCourt = doc["competentCourt"] as? String ?: "",
        iban = doc["iban"] as? String ?: "",
        bankName = doc["bankName"] as? String ?: "",
        mtnMomoNumber = doc["mtnMomoNumber"] as? String ?: "",
        airtelMoneyNumber = doc["airtelMoneyNumber"] as? String ?: "",
        invoiceNumberFormat = doc["invoiceNumberFormat"] as? String ?: "FAC-{YYYY}-{NNNNNN}",
        legalMentions = doc["legalMentions"] as? String ?: "",
        updatedAt = (doc["updatedAt"] as? Number)?.toLong() ?: 0L
    )

    private fun applyRequest(current: PlatformIdentity, req: UpdatePlatformIdentityRequest, now: Long): PlatformIdentity =
        current.copy(
            raisonSociale = req.raisonSociale ?: current.raisonSociale,
            rccm = req.rccm ?: current.rccm,
            niu = req.niu ?: current.niu,
            siege = req.siege ?: current.siege,
            city = req.city ?: current.city,
            country = req.country ?: current.country,
            phone = req.phone ?: current.phone,
            email = req.email ?: current.email,
            website = req.website ?: current.website,
            logoBase64 = req.logoBase64 ?: current.logoBase64,
            tvaRate = req.tvaRate ?: current.tvaRate,
            tvaExempted = req.tvaExempted ?: current.tvaExempted,
            paymentTerms = req.paymentTerms ?: current.paymentTerms,
            competentCourt = req.competentCourt ?: current.competentCourt,
            iban = req.iban ?: current.iban,
            bankName = req.bankName ?: current.bankName,
            mtnMomoNumber = req.mtnMomoNumber ?: current.mtnMomoNumber,
            airtelMoneyNumber = req.airtelMoneyNumber ?: current.airtelMoneyNumber,
            invoiceNumberFormat = req.invoiceNumberFormat
                ?.takeIf { it.isNotBlank() && SEQUENCE_PLACEHOLDER.containsMatchIn(it) }
                ?: current.invoiceNumberFormat,
            legalMentions = req.legalMentions ?: current.legalMentions,
            updatedAt = now
        )

    private fun buildPayload(p: PlatformIdentity): Map<String, Any?> = mapOf(
        "type" to DOC_TYPE,
        "raisonSociale" to p.raisonSociale,
        "rccm" to p.rccm,
        "niu" to p.niu,
        "siege" to p.siege,
        "city" to p.city,
        "country" to p.country,
        "phone" to p.phone,
        "email" to p.email,
        "website" to p.website,
        "logoBase64" to p.logoBase64,
        "tvaRate" to p.tvaRate,
        "tvaExempted" to p.tvaExempted,
        "paymentTerms" to p.paymentTerms,
        "competentCourt" to p.competentCourt,
        "iban" to p.iban,
        "bankName" to p.bankName,
        "mtnMomoNumber" to p.mtnMomoNumber,
        "airtelMoneyNumber" to p.airtelMoneyNumber,
        "invoiceNumberFormat" to p.invoiceNumberFormat,
        "legalMentions" to p.legalMentions,
        "updatedAt" to p.updatedAt
    )
}
