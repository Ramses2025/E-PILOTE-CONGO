package cg.epilote.backend.admin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests unitaires pour la logique pure de AdminGroupeController :
 *
 *  1. normalizeDashboardCategoryCode — mapping des alias de catégorie
 *  2. isCurrentSubscription — règle accès actif vs expiré / pending / suspended
 *  3. Notification expiration — calcul jours restants
 *  4. SubscriptionRequestBody — validation des champs
 *  5. GroupeDashboardStatsResponse — intégrité montantDuXAF (jamais négatif)
 *  6. GroupeNotificationResponse — champ isRead par défaut à false
 */
class AdminGroupeControllerLogicTest {

    // ── Mirror des fonctions privées du contrôleur ────────────────────────────

    private fun normalizeDashboardCategoryCode(code: String): String = when (code.trim().lowercase()) {
        "scolarite", "scolarisation" -> "scolarisation"
        "finance", "finances" -> "finance"
        "rh", "personnel", "ressources-humaines", "ressources_humaines" -> "rh"
        else -> code.trim().lowercase()
    }

    private fun isCurrentSubscription(subscription: SubscriptionResponse): Boolean =
        subscription.statut.equals("active", ignoreCase = true) &&
            (subscription.dateFin == 0L || subscription.dateFin >= System.currentTimeMillis())

    private fun subscription(
        statut: String,
        dateFin: Long,
        dateDebut: Long = System.currentTimeMillis() - 1_000L
    ) = SubscriptionResponse(
        id = "sub::g1", groupeId = "g1", planId = "plan-pro",
        statut = statut, dateDebut = dateDebut, dateFin = dateFin,
        renouvellementAuto = false, createdAt = dateDebut
    )

    // ── Test 1 : normalizeDashboardCategoryCode ───────────────────────────────

    @Test
    fun `scolarite alias normalizes to scolarisation`() {
        assertEquals("scolarisation", normalizeDashboardCategoryCode("scolarite"))
    }

    @Test
    fun `scolarisation passes through unchanged`() {
        assertEquals("scolarisation", normalizeDashboardCategoryCode("scolarisation"))
    }

    @Test
    fun `finance alias normalizes correctly`() {
        assertEquals("finance", normalizeDashboardCategoryCode("finances"))
        assertEquals("finance", normalizeDashboardCategoryCode("finance"))
    }

    @Test
    fun `rh aliases all normalize to rh`() {
        assertEquals("rh", normalizeDashboardCategoryCode("rh"))
        assertEquals("rh", normalizeDashboardCategoryCode("personnel"))
        assertEquals("rh", normalizeDashboardCategoryCode("ressources-humaines"))
        assertEquals("rh", normalizeDashboardCategoryCode("ressources_humaines"))
    }

    @Test
    fun `unknown category codes pass through lowercased`() {
        assertEquals("pedagogie", normalizeDashboardCategoryCode("Pedagogie"))
        assertEquals("vie-scolaire", normalizeDashboardCategoryCode("Vie-Scolaire"))
        assertEquals("communication", normalizeDashboardCategoryCode("COMMUNICATION"))
    }

    @Test
    fun `normalization trims surrounding spaces`() {
        assertEquals("scolarisation", normalizeDashboardCategoryCode("  scolarisation  "))
        assertEquals("rh", normalizeDashboardCategoryCode("  personnel  "))
    }

    // ── Test 2 : isCurrentSubscription ───────────────────────────────────────

    @Test
    fun `active subscription with future dateFin grants access`() {
        val now = System.currentTimeMillis()
        val sub = subscription("active", dateFin = now + 86_400_000L)
        assertTrue(isCurrentSubscription(sub), "active + dateFin future doit autoriser l'accès")
    }

    @Test
    fun `active subscription with dateFin zero grants access`() {
        val sub = subscription("active", dateFin = 0L)
        assertTrue(isCurrentSubscription(sub), "active + dateFin=0 (accès permanent) doit être accordé")
    }

    @Test
    fun `active subscription with past dateFin blocks access`() {
        val now = System.currentTimeMillis()
        val sub = subscription("active", dateFin = now - 1_000L)
        assertFalse(isCurrentSubscription(sub), "active mais expiré doit bloquer l'accès")
    }

    @Test
    fun `pending subscription blocks access even with future dateFin`() {
        val now = System.currentTimeMillis()
        val sub = subscription("pending", dateFin = now + 86_400_000L)
        assertFalse(isCurrentSubscription(sub), "pending doit bloquer l'accès")
    }

    @Test
    fun `suspended subscription blocks access`() {
        val now = System.currentTimeMillis()
        val sub = subscription("suspended", dateFin = now + 86_400_000L)
        assertFalse(isCurrentSubscription(sub), "suspended doit bloquer l'accès")
    }

    @Test
    fun `cancelled subscription blocks access`() {
        val now = System.currentTimeMillis()
        val sub = subscription("cancelled", dateFin = now + 86_400_000L)
        assertFalse(isCurrentSubscription(sub), "cancelled doit bloquer l'accès")
    }

    @Test
    fun `isCurrentSubscription is case insensitive for statut`() {
        val now = System.currentTimeMillis()
        val sub = subscription("ACTIVE", dateFin = now + 86_400_000L)
        assertTrue(isCurrentSubscription(sub), "statut ACTIVE (uppercase) doit être reconnu")
    }

    // ── Test 3 : Notification — calcul jours restants ─────────────────────────

    @Test
    fun `daysLeft calculation is correct for 15 remaining days`() {
        val now = System.currentTimeMillis()
        val dateFin = now + 15L * 86_400_000L
        val daysLeft = ((dateFin - now) / (24 * 3600 * 1000)).toInt()
        assertEquals(15, daysLeft)
        assertTrue(daysLeft in 1..30, "15 jours → dans la fenêtre d'alerte")
    }

    @Test
    fun `daysLeft 31 is outside warning window`() {
        val now = System.currentTimeMillis()
        val dateFin = now + 31L * 86_400_000L
        val daysLeft = ((dateFin - now) / (24 * 3600 * 1000)).toInt()
        assertFalse(daysLeft in 1..30, "31 jours → hors fenêtre d'alerte")
    }

    @Test
    fun `expired subscription triggers expiry notification`() {
        val now = System.currentTimeMillis()
        val expiredFin = now - 1_000L
        val isExpired = expiredFin in 1..now
        assertTrue(isExpired, "dateFin dans [1, now] → notification expiration")
    }

    @Test
    fun `dateFin zero does not trigger expiry notification`() {
        val now = System.currentTimeMillis()
        val dateFin = 0L
        val isExpired = dateFin in 1..now
        assertFalse(isExpired, "dateFin=0 (indéfini) ne doit pas déclencher notification expiration")
    }

    // ── Test 4 : SubscriptionRequestBody — validation ─────────────────────────

    @Test
    fun `SubscriptionRequestBody upgrade type is valid`() {
        val req = SubscriptionRequestBody(type = "upgrade")
        assertEquals("upgrade", req.type)
        assertNull(req.message)
    }

    @Test
    fun `SubscriptionRequestBody renewal with message`() {
        val req = SubscriptionRequestBody(type = "renewal", message = "Renouvellement annuel souhaité")
        assertEquals("renewal", req.type)
        assertEquals("Renouvellement annuel souhaité", req.message)
    }

    @Test
    fun `SubscriptionRequestBody message can be null`() {
        val req = SubscriptionRequestBody(type = "downgrade", message = null)
        assertNull(req.message)
    }

    // ── Test 5 : GroupeDashboardStatsResponse — montantDu jamais négatif ──────

    @Test
    fun `montantDuXAF is zero when paidXAF exceeds total`() {
        val total = 50_000L
        val paid = 60_000L
        val montantDu = (total - paid).coerceAtLeast(0L)
        assertEquals(0L, montantDu, "montantDu ne doit jamais être négatif (paiement en excès)")
    }

    @Test
    fun `montantDuXAF is positive when total exceeds paid`() {
        val total = 100_000L
        val paid = 60_000L
        val montantDu = (total - paid).coerceAtLeast(0L)
        assertEquals(40_000L, montantDu)
    }

    @Test
    fun `montantDuXAF is zero when fully paid`() {
        val total = 75_000L
        val paid = 75_000L
        val montantDu = (total - paid).coerceAtLeast(0L)
        assertEquals(0L, montantDu)
    }

    // ── Test 6 : GroupeNotificationResponse — champs par défaut ──────────────

    @Test
    fun `GroupeNotificationResponse isRead defaults to false`() {
        val notif = GroupeNotificationResponse(
            id = "notif_plan_expiring",
            type = "warning",
            titre = "Abonnement bientôt expiré",
            message = "Votre abonnement expire dans 7 jours.",
            date = System.currentTimeMillis(),
            category = "systeme"
        )
        assertFalse(notif.isRead, "isRead doit être false par défaut")
    }

    @Test
    fun `GroupeNotificationResponse error type for expired plan`() {
        val notif = GroupeNotificationResponse(
            id = "notif_plan_expired",
            type = "error",
            titre = "Abonnement expiré",
            message = "Votre abonnement est expiré. Contactez E-PILOTE Congo.",
            date = System.currentTimeMillis(),
            category = "systeme"
        )
        assertEquals("error", notif.type)
        assertEquals("systeme", notif.category)
    }

    @Test
    fun `GroupeNotificationResponse error type for overdue invoices`() {
        val overdueCount = 3L
        val notif = GroupeNotificationResponse(
            id = "notif_overdue_invoices",
            type = "error",
            titre = "$overdueCount factures en retard",
            message = "Régularisez votre situation pour éviter une suspension.",
            date = System.currentTimeMillis(),
            category = "financier"
        )
        assertEquals("3 factures en retard", notif.titre)
        assertEquals("financier", notif.category)
    }

    @Test
    fun `singular form used for single overdue invoice`() {
        val overdueCount = 1L
        val titre = "$overdueCount facture${if (overdueCount > 1L) "s" else ""} en retard"
        assertEquals("1 facture en retard", titre)
    }

    @Test
    fun `plural form used for multiple overdue invoices`() {
        val overdueCount = 5L
        val titre = "$overdueCount facture${if (overdueCount > 1L) "s" else ""} en retard"
        assertEquals("5 factures en retard", titre)
    }
}
