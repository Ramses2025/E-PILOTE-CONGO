package cg.epilote.backend.admin

import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import kotlinx.coroutines.runBlocking
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
    private val scope by lazy { runBlocking { bucket.defaultScope() } }

    private companion object {
        const val CONFIG_COLLECTION = "config"
        const val DOC_ID = "config::platform_identity"
        const val DOC_TYPE = "config_platform_identity"
    }

    private fun col(name: String): Collection = runBlocking { scope.collection(name) }

    suspend fun read(): PlatformIdentity {
        val doc = runCatching {
            col(CONFIG_COLLECTION).get(DOC_ID).contentAs<Map<String, Any?>>()
        }.getOrNull() ?: return PlatformIdentity()

        return PlatformIdentity(
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
    }

    suspend fun update(req: UpdatePlatformIdentityRequest): PlatformIdentity {
        val current = read()
        val now = System.currentTimeMillis()
        val next = current.copy(
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
            invoiceNumberFormat = req.invoiceNumberFormat?.takeIf { it.isNotBlank() } ?: current.invoiceNumberFormat,
            legalMentions = req.legalMentions ?: current.legalMentions,
            updatedAt = now
        )
        val payload = mapOf(
            "type" to DOC_TYPE,
            "raisonSociale" to next.raisonSociale,
            "rccm" to next.rccm,
            "niu" to next.niu,
            "siege" to next.siege,
            "city" to next.city,
            "country" to next.country,
            "phone" to next.phone,
            "email" to next.email,
            "website" to next.website,
            "logoBase64" to next.logoBase64,
            "tvaRate" to next.tvaRate,
            "tvaExempted" to next.tvaExempted,
            "paymentTerms" to next.paymentTerms,
            "competentCourt" to next.competentCourt,
            "iban" to next.iban,
            "bankName" to next.bankName,
            "mtnMomoNumber" to next.mtnMomoNumber,
            "airtelMoneyNumber" to next.airtelMoneyNumber,
            "invoiceNumberFormat" to next.invoiceNumberFormat,
            "legalMentions" to next.legalMentions,
            "updatedAt" to now
        )
        col(CONFIG_COLLECTION).upsert(DOC_ID, payload)
        return next
    }
}
