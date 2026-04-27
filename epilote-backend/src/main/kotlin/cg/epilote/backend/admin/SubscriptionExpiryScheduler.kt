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
    private val subscriptionRepo: AdminSubscriptionRepository
) {
    private val log = LoggerFactory.getLogger(SubscriptionExpiryScheduler::class.java)

    // Ordre : seconde, minute, heure, jour-mois, mois, jour-semaine
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    fun suspendExpiredSubscriptionsDaily() {
        val suspended = runBlocking { runCatching { subscriptionRepo.suspendExpiredSubscriptions() }.getOrElse { emptyList() } }
        if (suspended.isNotEmpty()) {
            log.info(
                "Abonnements suspendus automatiquement (expiration dépassée) : {} groupe(s) — ids {}",
                suspended.size, suspended
            )
        }
    }
}
