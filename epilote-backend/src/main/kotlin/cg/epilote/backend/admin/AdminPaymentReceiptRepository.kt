package cg.epilote.backend.admin

import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.query.execute
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Historique des paiements présentiels enregistrés pour chaque groupe scolaire.
 *
 * Document `payment_receipt::<uuid>` dans la collection `payment_receipts`.
 * Si la collection n'existe pas dans Capella, l'upsert échoue silencieusement côté
 * serveur (log) — l'infrastructure doit être provisionnée dans le bucket au moment
 * du déploiement. Référence App Services : https://docs.couchbase.com/sync-gateway/current/configuration-properties-legacy.html
 *
 * Le modèle de données contient toujours `groupId` + `schoolId = ""` pour être
 * compatible avec la sync-function existante (channel `grp::<groupId>` côté App
 * Services) — cf. knowledge `note-0aeddffd5adc4727a615f2dba35027ae`.
 */
@Repository
class AdminPaymentReceiptRepository(private val bucket: Bucket) {
    private val scope by lazy { runBlocking { bucket.defaultScope() } }

    private companion object {
        const val COLLECTION = "payment_receipts"
        const val DOC_TYPE = "payment_receipt"
    }

    private fun col(name: String): Collection = runBlocking { scope.collection(name) }

    private fun now(): Long = System.currentTimeMillis()

    private fun newId(): String = "payment_receipt::${UUID.randomUUID()}"

    suspend fun record(
        groupeId: String,
        subscriptionId: String,
        invoiceId: String?,
        montantXAF: Long,
        method: PaymentMethod,
        externalReference: String?,
        paidBy: String?,
        receivedBy: String,
        notes: String,
        accessStart: Long,
        accessEnd: Long
    ): PaymentReceiptResponse {
        val id = newId()
        val receivedAt = now()
        val doc = mapOf(
            "type" to DOC_TYPE,
            "groupId" to groupeId,
            "schoolId" to "",
            "groupeId" to groupeId,
            "subscriptionId" to subscriptionId,
            "invoiceId" to invoiceId,
            "montantXAF" to montantXAF,
            "paymentMethod" to method.code,
            "paymentMethodLabel" to method.label,
            "externalReference" to externalReference,
            "paidBy" to paidBy,
            "receivedBy" to receivedBy,
            "notes" to notes,
            "accessStart" to accessStart,
            "accessEnd" to accessEnd,
            "receivedAt" to receivedAt,
            "createdAt" to receivedAt,
            "updatedAt" to receivedAt
        )
        col(COLLECTION).upsert(id, doc)
        return PaymentReceiptResponse(
            id = id,
            groupeId = groupeId,
            subscriptionId = subscriptionId,
            invoiceId = invoiceId,
            montantXAF = montantXAF,
            paymentMethod = method.code,
            paymentMethodLabel = method.label,
            externalReference = externalReference,
            paidBy = paidBy,
            receivedBy = receivedBy,
            notes = notes,
            accessStart = accessStart,
            accessEnd = accessEnd,
            receivedAt = receivedAt
        )
    }

    suspend fun listByGroupe(groupeId: String): List<PaymentReceiptResponse> = runCatching {
        val result = scope.query(
            "SELECT META().id AS id, * FROM `$COLLECTION` WHERE `type` = '$DOC_TYPE' AND `groupeId` = \$groupeId",
            parameters = com.couchbase.client.kotlin.query.QueryParameters.named("groupeId" to groupeId)
        ).execute()
        result.rows.mapNotNull(::rowToResponse).sortedByDescending { it.receivedAt }
    }.getOrElse { emptyList() }

    suspend fun listAll(): List<PaymentReceiptResponse> = runCatching {
        val result = scope.query(
            "SELECT META().id AS id, * FROM `$COLLECTION` WHERE `type` = '$DOC_TYPE'"
        ).execute()
        result.rows.mapNotNull(::rowToResponse).sortedByDescending { it.receivedAt }
    }.getOrElse { emptyList() }

    private fun rowToResponse(row: com.couchbase.client.kotlin.query.QueryRow): PaymentReceiptResponse? {
        val data = row.contentAs<Map<String, Any?>>()
        val id = data["id"] as? String ?: return null
        @Suppress("UNCHECKED_CAST")
        val inner = (data[COLLECTION] as? Map<String, Any?>) ?: return null
        val methodCode = inner["paymentMethod"] as? String ?: "cash"
        val method = PaymentMethod.fromCode(methodCode) ?: PaymentMethod.CASH
        return PaymentReceiptResponse(
            id = id,
            groupeId = inner["groupeId"] as? String ?: "",
            subscriptionId = inner["subscriptionId"] as? String ?: "",
            invoiceId = inner["invoiceId"] as? String,
            montantXAF = (inner["montantXAF"] as? Number)?.toLong() ?: 0L,
            paymentMethod = methodCode,
            paymentMethodLabel = inner["paymentMethodLabel"] as? String ?: method.label,
            externalReference = inner["externalReference"] as? String,
            paidBy = inner["paidBy"] as? String,
            receivedBy = inner["receivedBy"] as? String ?: "",
            notes = inner["notes"] as? String ?: "",
            accessStart = (inner["accessStart"] as? Number)?.toLong() ?: 0L,
            accessEnd = (inner["accessEnd"] as? Number)?.toLong() ?: 0L,
            receivedAt = (inner["receivedAt"] as? Number)?.toLong() ?: 0L
        )
    }
}
