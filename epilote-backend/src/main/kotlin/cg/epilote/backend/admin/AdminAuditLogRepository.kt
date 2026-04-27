package cg.epilote.backend.admin

import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.query.QueryParameters
import com.couchbase.client.kotlin.query.execute
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Persistance des événements d'audit dans la collection `audit_logs`
 * (cloud-only, jamais répliqué sur les bases mobiles).
 *
 * Toutes les écritures sont **best-effort** : un échec ne doit jamais propager
 * une exception au business call (sinon une coupure réseau Capella ferait
 * échouer une création de groupe). Les erreurs sont loguées mais avalées.
 *
 * Pattern KV upsert + N1QL pagination (LIMIT/OFFSET) :
 *  - https://docs.couchbase.com/kotlin-sdk/current/howtos/kv-operations.html
 *  - https://docs.couchbase.com/kotlin-sdk/current/howtos/n1ql-queries-with-sdk.html
 */
@Repository
class AdminAuditLogRepository(private val bucket: Bucket) {
    private val log = LoggerFactory.getLogger(AdminAuditLogRepository::class.java)
    private val scope by lazy { runBlocking { bucket.defaultScope() } }

    private companion object {
        const val COLLECTION = "audit_logs"
        const val DOC_TYPE = "audit_log"
        const val MAX_PAGE_SIZE = 200
    }

    private fun col(): Collection = runBlocking { scope.collection(COLLECTION) }

    private fun newId(): String = "audit::${UUID.randomUUID()}"

    private fun now(): Long = System.currentTimeMillis()

    /**
     * Enregistre un évènement d'audit. Best-effort : retourne `null` en cas
     * d'échec (collection absente, Capella indisponible, etc.) sans propager
     * l'exception au caller.
     */
    suspend fun record(
        action: AuditAction,
        outcome: AuditOutcome = AuditOutcome.SUCCESS,
        actorId: String? = null,
        actorEmail: String? = null,
        actorRole: String? = null,
        targetType: String? = null,
        targetId: String? = null,
        targetLabel: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null,
        message: String? = null,
        metadata: Map<String, Any?> = emptyMap()
    ): AuditEventResponse? = runCatching {
        val id = newId()
        val timestamp = now()
        val doc = mapOf(
            "type" to DOC_TYPE,
            "timestamp" to timestamp,
            "action" to action.code,
            "actionLabel" to action.label,
            "category" to action.category.code,
            "outcome" to outcome.code,
            "actorId" to actorId,
            "actorEmail" to actorEmail,
            "actorRole" to actorRole,
            "targetType" to targetType,
            "targetId" to targetId,
            "targetLabel" to targetLabel,
            "ipAddress" to ipAddress,
            "userAgent" to userAgent,
            "message" to message,
            "metadata" to metadata
        )
        col().upsert(id, doc)
        AuditEventResponse(
            id = id,
            timestamp = timestamp,
            action = action.code,
            actionLabel = action.label,
            category = action.category.code,
            outcome = outcome.code,
            actorId = actorId,
            actorEmail = actorEmail,
            actorRole = actorRole,
            targetType = targetType,
            targetId = targetId,
            targetLabel = targetLabel,
            ipAddress = ipAddress,
            userAgent = userAgent,
            message = message,
            metadata = metadata
        )
    }.onFailure { e ->
        log.warn("Audit log write failed (action={}, outcome={}): {} — {}",
            action.code, outcome.code, e.javaClass.simpleName, e.message)
    }.getOrNull()

    /**
     * Liste paginée triée par timestamp décroissant. `page` est 1-based.
     * Filtres optionnels :
     *   - `category` : `auth`, `groupe`, `admin`, `subscription`, `invoice`, `payment`, `platform`, `system`
     *   - `action`   : code complet (ex. `groupe.created`)
     *   - `outcome`  : `success` | `failure`
     *   - `actorId`  : id utilisateur
     *   - `targetId` : id ressource cible
     *   - `since` / `until` : bornes timestamp ms (epoch)
     */
    suspend fun list(
        page: Int = 1,
        pageSize: Int = 50,
        category: String? = null,
        action: String? = null,
        outcome: String? = null,
        actorId: String? = null,
        targetId: String? = null,
        since: Long? = null,
        until: Long? = null,
        search: String? = null
    ): AuditLogPage {
        val safePage = page.coerceAtLeast(1)
        val safeSize = pageSize.coerceIn(1, MAX_PAGE_SIZE)
        val offset = (safePage - 1) * safeSize

        val filters = mutableListOf("`type` = '$DOC_TYPE'")
        val params = mutableMapOf<String, Any>()
        if (!category.isNullOrBlank()) { filters += "`category` = \$category"; params["category"] = category }
        if (!action.isNullOrBlank())   { filters += "`action` = \$action";     params["action"]   = action }
        if (!outcome.isNullOrBlank())  { filters += "`outcome` = \$outcome";   params["outcome"]  = outcome }
        if (!actorId.isNullOrBlank())  { filters += "`actorId` = \$actorId";   params["actorId"]  = actorId }
        if (!targetId.isNullOrBlank()) { filters += "`targetId` = \$targetId"; params["targetId"] = targetId }
        if (since != null)             { filters += "`timestamp` >= \$since";  params["since"]    = since }
        if (until != null)             { filters += "`timestamp` <= \$until";  params["until"]    = until }
        if (!search.isNullOrBlank()) {
            filters += "(LOWER(IFMISSINGORNULL(`message`,'')) LIKE \$search OR LOWER(IFMISSINGORNULL(`actorEmail`,'')) LIKE \$search OR LOWER(IFMISSINGORNULL(`targetLabel`,'')) LIKE \$search)"
            params["search"] = "%${search.lowercase()}%"
        }
        val where = filters.joinToString(" AND ")

        val total = runCatching {
            val q = scope.query(
                "SELECT RAW COUNT(*) FROM `$COLLECTION` WHERE $where",
                parameters = QueryParameters.named(params)
            ).execute()
            (q.rows.firstOrNull()?.contentAs<Number>())?.toLong() ?: 0L
        }.getOrElse { 0L }

        val rows = runCatching {
            val q = scope.query(
                "SELECT META().id AS id, * FROM `$COLLECTION` WHERE $where ORDER BY `timestamp` DESC LIMIT \$limit OFFSET \$offset",
                parameters = QueryParameters.named(params + mapOf("limit" to safeSize, "offset" to offset))
            ).execute()
            q.rows.mapNotNull(::rowToResponse)
        }.getOrElse { emptyList() }

        return AuditLogPage(items = rows, total = total, page = safePage, pageSize = safeSize)
    }

    private fun rowToResponse(row: com.couchbase.client.kotlin.query.QueryRow): AuditEventResponse? = runCatching {
        @Suppress("UNCHECKED_CAST")
        val raw = row.contentAs<Map<String, Any?>>()
        val id = raw["id"] as? String ?: return@runCatching null
        val inner = raw[COLLECTION] as? Map<String, Any?> ?: raw
        AuditEventResponse(
            id = id,
            timestamp = (inner["timestamp"] as? Number)?.toLong() ?: 0L,
            action = inner["action"] as? String ?: "",
            actionLabel = inner["actionLabel"] as? String ?: "",
            category = inner["category"] as? String ?: "",
            outcome = inner["outcome"] as? String ?: AuditOutcome.SUCCESS.code,
            actorId = inner["actorId"] as? String,
            actorEmail = inner["actorEmail"] as? String,
            actorRole = inner["actorRole"] as? String,
            targetType = inner["targetType"] as? String,
            targetId = inner["targetId"] as? String,
            targetLabel = inner["targetLabel"] as? String,
            ipAddress = inner["ipAddress"] as? String,
            userAgent = inner["userAgent"] as? String,
            message = inner["message"] as? String,
            metadata = @Suppress("UNCHECKED_CAST") ((inner["metadata"] as? Map<String, Any?>) ?: emptyMap())
        )
    }.getOrNull()
}
