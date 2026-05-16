package cg.epilote.backend.admin

import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.query.QueryParameters
import com.couchbase.client.kotlin.query.execute
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Repository

@Repository
class AdminGroupeKpiRepository(
    private val bucket: Bucket,
    private val adminRepository: AdminRepository
) {

    private val scope = runBlocking { bucket.defaultScope() }

    // ── Scolarisation ─────────────────────────────────────────────

    suspend fun scolarisationKpi(groupeId: String): ScolarisationKpiResponse {
        val params = QueryParameters.named("gid" to groupeId)

        val byProvince = runCatching { queryLabelCount(
            "SELECT IFMISSINGORNULL(`province`, 'Inconnue') AS label, COUNT(*) AS cnt " +
            "FROM `schools` WHERE `type` = 'school' AND (`groupId` = \$gid OR `groupeId` = \$gid) " +
            "GROUP BY IFMISSINGORNULL(`province`, 'Inconnue') ORDER BY cnt DESC",
            params
        ) }.getOrDefault(emptyList())

        val byNiveau = runCatching { queryLabelCount(
            "SELECT n AS label, COUNT(*) AS cnt FROM `schools` UNNEST `niveaux` AS n " +
            "WHERE `type` = 'school' AND (`groupId` = \$gid OR `groupeId` = \$gid) " +
            "GROUP BY n ORDER BY cnt DESC",
            params
        ) }.getOrDefault(emptyList())

        val usersPerEcole = runCatching { queryLabelCount(
            "SELECT IFMISSINGORNULL(`schoolId`, 'N/A') AS label, COUNT(*) AS cnt " +
            "FROM `users` WHERE `type` = 'user' AND (`groupId` = \$gid OR `groupeId` = \$gid) " +
            "AND `schoolId` IS NOT MISSING AND `schoolId` != '' " +
            "GROUP BY IFMISSINGORNULL(`schoolId`, 'N/A') ORDER BY cnt DESC LIMIT 20",
            params
        ) }.getOrDefault(emptyList())

        val nbEcoles = byProvince.sumOf { it.count }
        val nbUsers  = usersPerEcole.sumOf { it.count }

        return ScolarisationKpiResponse(
            nbEcoles        = nbEcoles,
            nbUtilisateurs  = nbUsers,
            ecolesByProvince = byProvince,
            ecolesByNiveau  = byNiveau,
            usersPerEcole   = usersPerEcole
        )
    }

    // ── Finance ───────────────────────────────────────────────────

    suspend fun financeKpi(groupeId: String): FinanceKpiResponse {
        val params = QueryParameters.named("gid" to groupeId)
        val totalXAF  = runCatching { adminRepository.revenueTotalByGroupe(groupeId) }.getOrDefault(0L)
        val paidXAF   = runCatching { adminRepository.revenuePaidByGroupe(groupeId) }.getOrDefault(0L)
        val nb        = runCatching { adminRepository.countInvoicesByGroupe(groupeId) }.getOrDefault(0L)
        val nbOverdue = runCatching { adminRepository.countInvoicesOverdueByGroupe(groupeId) }.getOrDefault(0L)
        val taux      = if (totalXAF > 0) ((paidXAF * 100) / totalXAF).toInt() else 0

        val byStatut = runCatching { queryFinanceByStatut(params) }.getOrDefault(emptyList())

        return FinanceKpiResponse(
            totalXAF         = totalXAF,
            paidXAF          = paidXAF,
            dueXAF           = (totalXAF - paidXAF).coerceAtLeast(0L),
            nbFactures       = nb,
            nbOverdue        = nbOverdue,
            tauxRecouvrement = taux,
            byStatut         = byStatut
        )
    }

    private suspend fun queryFinanceByStatut(params: QueryParameters): List<FinanceStatutDto> {
        val stmt =
            "SELECT LOWER(IFMISSINGORNULL(`statut`,'inconnu')) AS statut, COUNT(*) AS cnt, " +
            "SUM(IFMISSINGORNULL(`montantXAF`,0)) AS totalXAF " +
            "FROM `invoices` WHERE `type` IN ['invoice','invoice_platform'] AND `groupeId` = \$gid " +
            "GROUP BY LOWER(IFMISSINGORNULL(`statut`,'inconnu'))"
        val result = scope.query(stmt, parameters = params).execute()
        return result.rows.mapNotNull { row ->
            val d = row.contentAs<Map<String, Any?>>()
            FinanceStatutDto(
                statut   = d["statut"]   as? String ?: "inconnu",
                count    = (d["cnt"]     as? Number)?.toLong() ?: 0L,
                totalXAF = (d["totalXAF"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    // ── RH ────────────────────────────────────────────────────────

    suspend fun rhKpi(groupeId: String): RhKpiResponse {
        val params = QueryParameters.named("gid" to groupeId)

        val byRole = runCatching { queryLabelCount(
            "SELECT IFMISSINGORNULL(`role`, 'USER') AS label, COUNT(*) AS cnt " +
            "FROM `users` WHERE `type` = 'user' AND (`groupId` = \$gid OR `groupeId` = \$gid) " +
            "GROUP BY IFMISSINGORNULL(`role`, 'USER') ORDER BY cnt DESC",
            params
        ) }.getOrDefault(emptyList())

        val perEcole = runCatching { queryLabelCount(
            "SELECT IFMISSINGORNULL(`schoolId`, 'N/A') AS label, COUNT(*) AS cnt " +
            "FROM `users` WHERE `type` = 'user' AND (`groupId` = \$gid OR `groupeId` = \$gid) " +
            "AND `schoolId` IS NOT MISSING AND `schoolId` != '' " +
            "GROUP BY IFMISSINGORNULL(`schoolId`, 'N/A') ORDER BY cnt DESC LIMIT 20",
            params
        ) }.getOrDefault(emptyList())

        return RhKpiResponse(
            nbTotal      = byRole.sumOf { it.count },
            byRole       = byRole,
            usersPerEcole = perEcole
        )
    }

    // ── Utilitaire commun ─────────────────────────────────────────

    private suspend fun queryLabelCount(stmt: String, params: QueryParameters): List<LabelCountDto> {
        val result = scope.query(stmt, parameters = params).execute()
        return result.rows.mapNotNull { row ->
            val d = row.contentAs<Map<String, Any?>>()
            val label = d["label"] as? String ?: return@mapNotNull null
            val cnt   = (d["cnt"] as? Number)?.toLong() ?: 0L
            LabelCountDto(label = label, count = cnt)
        }
    }
}
