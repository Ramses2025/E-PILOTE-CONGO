package cg.epilote.backend.admin.quota

import cg.epilote.backend.admin.AdminPlanRepository
import cg.epilote.backend.admin.AdminRepository
import cg.epilote.backend.admin.AdminSubscriptionRepository
import cg.epilote.backend.admin.EcoleResponse
import cg.epilote.backend.admin.PlanResponse
import cg.epilote.backend.admin.SubscriptionResponse
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * Tests unitaires du QuotaGuard (correctif #1 de l'audit abonnements).
 *
 * Couvre :
 *  - Refus 403 quand le quota d'écoles est atteint (current >= max)
 *  - Refus 403 quand le quota de personnel est atteint
 *  - Acceptation quand le quota n'est pas atteint
 *  - Refus 403 quand aucun abonnement actif n'est associé au groupe
 *  - Erreur 500 quand le plan référencé est introuvable
 *  - Message d'erreur explicite avec le nom du plan et les compteurs
 */
class QuotaGuardTest {

    private val planRepo: AdminPlanRepository = mock()
    private val subscriptionRepo: AdminSubscriptionRepository = mock()
    private val repo: AdminRepository = mock()
    private val guard = QuotaGuard(planRepo, subscriptionRepo, repo)

    private fun activeSub(planId: String = "plan::gratuit") = SubscriptionResponse(
        id = "sub::g1",
        groupeId = "g1",
        planId = planId,
        statut = "active",
        dateDebut = 0L,
        dateFin = System.currentTimeMillis() + 86_400_000L,
        renouvellementAuto = false,
        createdAt = 0L
    )

    private fun plan(maxEcoles: Int = 3, maxPersonnel: Int = 10, nom: String = "Gratuit") = PlanResponse(
        id = "plan::gratuit",
        nom = nom,
        type = "gratuit",
        prixXAF = 0L,
        maxEcoles = maxEcoles,
        maxStudents = 100,
        maxPersonnel = maxPersonnel,
        modulesIncluded = emptyList()
    )

    private fun ecole(id: String = "school::x") = EcoleResponse(
        id = id, groupId = "g1", nom = "Test", province = "", territoire = "",
        niveaux = emptyList(), planId = "plan::gratuit", createdAt = 0L
    )

    // ── Quota écoles ──────────────────────────────────────────────

    @Test
    fun `assertCanCreateEcole rejects 403 when quota reached`() = runTest {
        whenever(subscriptionRepo.getActiveSubscriptionByGroupe("g1")).thenReturn(activeSub())
        whenever(planRepo.getPlanById("plan::gratuit")).thenReturn(plan(maxEcoles = 3))
        whenever(repo.listEcolesByGroupe("g1"))
            .thenReturn(listOf(ecole("a"), ecole("b"), ecole("c")))

        val ex = assertThrows(ResponseStatusException::class.java) {
            kotlinx.coroutines.runBlocking { guard.assertCanCreateEcole("g1") }
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
        assert(ex.reason!!.contains("écoles")) { "Message doit mentionner 'écoles' : ${ex.reason}" }
        assert(ex.reason!!.contains("3/3")) { "Message doit afficher les compteurs : ${ex.reason}" }
        assert(ex.reason!!.contains("Gratuit")) { "Message doit nommer le plan : ${ex.reason}" }
    }

    @Test
    fun `assertCanCreateEcole accepts when below quota`() = runTest {
        whenever(subscriptionRepo.getActiveSubscriptionByGroupe("g1")).thenReturn(activeSub())
        whenever(planRepo.getPlanById("plan::gratuit")).thenReturn(plan(maxEcoles = 3))
        whenever(repo.listEcolesByGroupe("g1")).thenReturn(listOf(ecole("a")))

        assertDoesNotThrow {
            kotlinx.coroutines.runBlocking { guard.assertCanCreateEcole("g1") }
        }
    }

    @Test
    fun `assertCanCreateEcole accepts when exactly one slot remains`() = runTest {
        whenever(subscriptionRepo.getActiveSubscriptionByGroupe("g1")).thenReturn(activeSub())
        whenever(planRepo.getPlanById("plan::gratuit")).thenReturn(plan(maxEcoles = 3))
        whenever(repo.listEcolesByGroupe("g1")).thenReturn(listOf(ecole("a"), ecole("b")))

        assertDoesNotThrow {
            kotlinx.coroutines.runBlocking { guard.assertCanCreateEcole("g1") }
        }
    }

    // ── Quota personnel ───────────────────────────────────────────

    @Test
    fun `assertCanCreateUser rejects 403 when quota reached`() = runTest {
        whenever(subscriptionRepo.getActiveSubscriptionByGroupe("g1")).thenReturn(activeSub())
        whenever(planRepo.getPlanById("plan::gratuit")).thenReturn(plan(maxPersonnel = 10))
        whenever(repo.countUsersByGroupeId("g1")).thenReturn(10L)

        val ex = assertThrows(ResponseStatusException::class.java) {
            kotlinx.coroutines.runBlocking { guard.assertCanCreateUser("g1") }
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
        assert(ex.reason!!.contains("personnel")) { "Message doit mentionner 'personnel' : ${ex.reason}" }
        assert(ex.reason!!.contains("10/10"))
    }

    @Test
    fun `assertCanCreateUser accepts when below quota`() = runTest {
        whenever(subscriptionRepo.getActiveSubscriptionByGroupe("g1")).thenReturn(activeSub())
        whenever(planRepo.getPlanById("plan::gratuit")).thenReturn(plan(maxPersonnel = 10))
        whenever(repo.countUsersByGroupeId("g1")).thenReturn(5L)

        assertDoesNotThrow {
            kotlinx.coroutines.runBlocking { guard.assertCanCreateUser("g1") }
        }
    }

    // ── Sécurité — pas d'abonnement / plan introuvable ────────────

    @Test
    fun `assertCanCreateEcole rejects 403 when no active subscription`() = runTest {
        whenever(subscriptionRepo.getActiveSubscriptionByGroupe("g1")).thenReturn(null)

        val ex = assertThrows(ResponseStatusException::class.java) {
            kotlinx.coroutines.runBlocking { guard.assertCanCreateEcole("g1") }
        }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
        assert(ex.reason!!.contains("Aucun abonnement actif"))
    }

    @Test
    fun `assertCanCreateUser raises 500 when plan is missing`() = runTest {
        whenever(subscriptionRepo.getActiveSubscriptionByGroupe("g1")).thenReturn(activeSub("plan::ghost"))
        whenever(planRepo.getPlanById("plan::ghost")).thenReturn(null)

        val ex = assertThrows(ResponseStatusException::class.java) {
            kotlinx.coroutines.runBlocking { guard.assertCanCreateUser("g1") }
        }
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.statusCode)
        assert(ex.reason!!.contains("plan::ghost"))
    }

}
