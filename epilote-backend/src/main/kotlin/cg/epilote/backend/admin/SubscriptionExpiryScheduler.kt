package cg.epilote.backend.admin

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Job Spring planifié : chaque nuit à 02:00 UTC, parcourt les abonnements actifs,
 * suspend ceux arrivés à échéance (renouvellement manuel non enregistré).
 *
 * Référence officielle Spring Scheduling :
 * https://docs.spring.io/spring-framework/reference/integration/scheduling.html
 */
@Component
class SubscriptionExpiryScheduler(
    private val subscriptionRepo: AdminSubscriptionRepository,
    private val auditRepo: AdminAuditLogRepository
) {
    private val log = LoggerFactory.getLogger(SubscriptionExpiryScheduler::class.java)

    // Ordre : seconde, minute, heure, jour-mois, mois, jour-semaine
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    fun suspendExpiredSubscriptionsDaily() {
        runBlocking {
            val suspended = runCatching { subscriptionRepo.suspendExpiredSubscriptions() }
                .getOrElse { emptyList() }
            if (suspended.isNotEmpty()) {
                log.info(
                    "Abonnements suspendus automatiquement (expiration dépassée) : {} groupe(s) — ids {}",
                    suspended.size, suspended
                )
            }
            auditRepo.record(
                action = AuditAction.SCHEDULER_EXPIRY_RUN,
                actorId = "system",
                actorEmail = "scheduler@epilote",
                actorRole = "SYSTEM",
                message = "Run automatique du job d'expiration — ${suspended.size} groupe(s) suspendu(s)",
                metadata = mapOf("trigger" to "scheduled", "suspendedGroupIds" to suspended)
            )
            suspended.forEach { gid ->
                auditRepo.record(
                    action = AuditAction.SUBSCRIPTION_AUTO_SUSPENDED,
                    actorId = "system",
                    actorEmail = "scheduler@epilote",
                    actorRole = "SYSTEM",
                    targetType = "groupe",
                    targetId = gid,
                    message = "Abonnement suspendu automatiquement (échéance dépassée)",
                    metadata = mapOf("trigger" to "scheduled")
                )
            }
        }
    }
}
