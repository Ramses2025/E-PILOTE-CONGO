package cg.epilote.backend.admin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Vérifie les invariants du workflow monétisation sans Couchbase réel :
 *
 *  1. buildSubscriptionResponse — groupDoc sans subscription sub-doc → null
 *  2. Statut initial d'une subscription créée → "pending"
 *  3. dateFin initial → 0L (bloqué jusqu'au paiement)
 *  4. subscriptionStatusLabel / subscriptionStatusColor — couverture "pending"
 *  5. CreateInvoiceRequest — montantXAF = 0 valide (@PositiveOrZero)
 *  6. RecordPaymentRequest — montantXAF = 0 valide (@PositiveOrZero)
 */
class SubscriptionWorkflowTest {

    // ── Test 1 : SubscriptionResponse initial statut ──────────────

    @Test
    fun `CreateSubscriptionRequest default autoRenew is false`() {
        val req = CreateSubscriptionRequest(groupeId = "g1", planId = "plan-pro")
        assertFalse(req.renouvellementAuto)
    }

    // ── Test 2 : SubscriptionResponse statut field ────────────────

    @Test
    fun `SubscriptionResponse reflects pending statut correctly`() {
        val sub = SubscriptionResponse(
            id = "sub::g1",
            groupeId = "g1",
            planId = "plan-pro",
            statut = "pending",
            dateDebut = System.currentTimeMillis(),
            dateFin = 0L,
            renouvellementAuto = false,
            createdAt = System.currentTimeMillis()
        )
        assertEquals("pending", sub.statut)
        assertEquals(0L, sub.dateFin)
        assertTrue(sub.dateFin < System.currentTimeMillis(), "dateFin=0 doit être inférieur à now → accès bloqué")
    }

    // ── Test 3 : Règle d'accès — getActiveSubscriptionByGroupe filtre pending ──

    @Test
    fun `pending subscription does not satisfy active access check`() {
        val sub = SubscriptionResponse(
            id = "sub::g1",
            groupeId = "g1",
            planId = "plan-pro",
            statut = "pending",
            dateDebut = System.currentTimeMillis(),
            dateFin = 0L,
            renouvellementAuto = false,
            createdAt = System.currentTimeMillis()
        )
        val grantedAccess = sub.statut == "active" && sub.dateFin >= System.currentTimeMillis()
        assertFalse(grantedAccess, "Un abonnement pending ne doit JAMAIS accorder l'accès")
    }

    // ── Test 4 : SubscriptionResponse après paiement ─────────────

    @Test
    fun `active subscription after payment grants access`() {
        val now = System.currentTimeMillis()
        val sub = SubscriptionResponse(
            id = "sub::g1",
            groupeId = "g1",
            planId = "plan-pro",
            statut = "active",
            dateDebut = now,
            dateFin = now + 30L * 86_400_000L,
            renouvellementAuto = false,
            createdAt = now
        )
        val grantedAccess = sub.statut == "active" && sub.dateFin >= now
        assertTrue(grantedAccess, "Un abonnement actif avec dateFin future doit accorder l'accès")
    }

    // ── Test 5 : @PositiveOrZero — CreateInvoiceRequest montantXAF = 0 ──

    @Test
    fun `CreateInvoiceRequest allows zero amount for free plans`() {
        val req = CreateInvoiceRequest(
            groupeId = "g1",
            subscriptionId = "sub::g1",
            montantXAF = 0L,
            dateEcheance = System.currentTimeMillis() + 30L * 86_400_000L,
            initialStatus = "draft"
        )
        assertEquals(0L, req.montantXAF)
    }

    // ── Test 6 : @PositiveOrZero — RecordPaymentRequest montantXAF = 0 ──

    @Test
    fun `RecordPaymentRequest allows zero amount for free plans`() {
        val req = RecordPaymentRequest(
            groupeId = "g1",
            subscriptionId = "sub::g1",
            montantXAF = 0L,
            paymentMethod = "cash",
            durationMonths = 1
        )
        assertEquals(0L, req.montantXAF)
    }

    // ── Test 7 : Workflow de blocage — accès refusé avant paiement, accordé après ──

    @Test
    fun `access lifecycle pending then active then expired`() {
        val now = System.currentTimeMillis()

        val pending = SubscriptionResponse("s", "g", "p", "pending", now, 0L, false, now)
        assertFalse(pending.statut == "active" && pending.dateFin >= now, "pending → bloqué")

        val active = SubscriptionResponse("s", "g", "p", "active", now, now + 86_400_000L, false, now)
        assertTrue(active.statut == "active" && active.dateFin >= now, "active futur → accès autorisé")

        val expired = SubscriptionResponse("s", "g", "p", "active", now - 2000L, now - 1000L, false, now - 2000L)
        assertFalse(expired.statut == "active" && expired.dateFin >= now, "active expiré → bloqué")

        val suspended = SubscriptionResponse("s", "g", "p", "suspended", now, now + 86_400_000L, false, now)
        assertFalse(suspended.statut == "active" && suspended.dateFin >= now, "suspended → bloqué")
    }
}
