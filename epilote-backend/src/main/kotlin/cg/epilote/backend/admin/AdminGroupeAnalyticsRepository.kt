package cg.epilote.backend.admin

import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.query.execute
import org.springframework.stereotype.Repository

@Repository
class AdminGroupeAnalyticsRepository(
    private val bucket: Bucket
) {
    private val scope = kotlinx.coroutines.runBlocking { bucket.defaultScope() }

    private companion object {
        const val INVOICES_COLLECTION = "invoices"
        const val LEGACY_INVOICES_COLLECTION = "invoices_platform"
    }

    suspend fun invoiceTimelineByGroupe(groupeId: String): List<MonthlyInvoiceStats> {
        val params = com.couchbase.client.kotlin.query.QueryParameters.named("gid" to groupeId)

        fun Map<String, MonthlyInvoiceStats>.mergeWith(other: Map<String, MonthlyInvoiceStats>): Map<String, MonthlyInvoiceStats> {
            val merged = this.toMutableMap()
            other.forEach { (month, v) ->
                val cur = merged[month]
                merged[month] = if (cur == null) {
                    v
                } else {
                    MonthlyInvoiceStats(
                        month = month,
                        totalXAF = cur.totalXAF + v.totalXAF,
                        paidXAF = cur.paidXAF + v.paidXAF,
                        count = cur.count + v.count
                    )
                }
            }
            return merged
        }

        suspend fun queryTimeline(collectionName: String): Map<String, MonthlyInvoiceStats> {
            val stmt = "SELECT SUBSTR(MILLIS_TO_STR(IFMISSINGORNULL(`dateEmission`, `createdAt`, 0)), 0, 7) AS month, " +
                "SUM(IFMISSINGORNULL(`montantXAF`, 0)) AS totalXAF, " +
                "SUM(CASE WHEN LOWER(`statut`) = 'paid' THEN IFMISSINGORNULL(`montantXAF`, 0) ELSE 0 END) AS paidXAF, " +
                "COUNT(*) AS cnt " +
                "FROM `$collectionName` " +
                "WHERE `type` IN ['invoice_platform', 'invoice'] AND `groupeId` = \$gid " +
                "GROUP BY SUBSTR(MILLIS_TO_STR(IFMISSINGORNULL(`dateEmission`, `createdAt`, 0)), 0, 7) " +
                "ORDER BY month"
            val result = scope.query(stmt, parameters = params).execute()

            val rows = result.rows.mapNotNull { row ->
                val data = row.contentAs<Map<String, Any?>>()
                val month = data["month"] as? String ?: return@mapNotNull null
                if (month.isBlank()) return@mapNotNull null
                val total = (data["totalXAF"] as? Number)?.toLong() ?: 0L
                val paid = (data["paidXAF"] as? Number)?.toLong() ?: 0L
                val cnt = (data["cnt"] as? Number)?.toLong() ?: 0L
                month to MonthlyInvoiceStats(month = month, totalXAF = total, paidXAF = paid, count = cnt)
            }
            return rows.toMap()
        }

        val primary = runCatching { queryTimeline(INVOICES_COLLECTION) }.getOrDefault(emptyMap())
        val legacy = runCatching { queryTimeline(LEGACY_INVOICES_COLLECTION) }.getOrDefault(emptyMap())

        return primary.mergeWith(legacy).values.sortedBy { it.month }
    }
}
