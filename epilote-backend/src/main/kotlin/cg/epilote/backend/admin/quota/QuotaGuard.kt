package cg.epilote.backend.admin.quota

import cg.epilote.backend.admin.AdminPlanRepository
import cg.epilote.backend.admin.AdminRepository
import cg.epilote.backend.admin.AdminSubscriptionRepository
import cg.epilote.backend.admin.PlanResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

/**
 * Garde-fou central pour les quotas d'abonnement (correctif #1 de l'audit).
 *
 * Vérifie que la création d'une nouvelle ressource (école, utilisateur) respecte
 * les limites du plan en cours pour le groupe. Lève une [ResponseStatusException]
 * 403 FORBIDDEN si le quota est atteint.
 *
 * Source de vérité de l'abonnement : [AdminSubscriptionRepository] (canonique,
 * stocke le doc dans `school_groups[id].subscription`). On n'utilise PAS
 * `AdminRepository.getActiveSubscriptionByGroupe` qui lit la collection legacy
 * `subscriptions` (code mort, plus aucun writer).
 *
 * Référence Spring (gestion d'erreur HTTP idiomatique) :
 * https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-exceptionhandler.html
 */
@Service
class QuotaGuard(
    private val planRepo: AdminPlanRepository,
    private val subscriptionRepo: AdminSubscriptionRepository,
    private val repo: AdminRepository
) {
    /**
     * Vérifie qu'une nouvelle école peut être créée pour le groupe.
     * Lève 403 FORBIDDEN si le quota `plan.maxEcoles` est atteint.
     */
    suspend fun assertCanCreateEcole(groupeId: String) {
        val plan = resolvePlan(groupeId)
        val current = repo.listEcolesByGroupe(groupeId).size
        if (current >= plan.maxEcoles) deny("écoles", current, plan.maxEcoles, plan.nom)
    }

    /**
     * Vérifie qu'un nouvel utilisateur (personnel) peut être créé pour le groupe.
     * Lève 403 FORBIDDEN si le quota `plan.maxPersonnel` est atteint.
     */
    suspend fun assertCanCreateUser(groupeId: String) {
        val plan = resolvePlan(groupeId)
        val current = repo.countUsersByGroupeId(groupeId).toInt()
        if (current >= plan.maxPersonnel) deny("personnel", current, plan.maxPersonnel, plan.nom)
    }

    /**
     * Résout le plan effectif du groupe via l'abonnement actif.
     * Lève 403 si pas d'abonnement actif (cas atypique : le gate `AuthService`
     * empêche déjà la connexion d'un Admin Groupe sans abonnement actif, mais
     * on protège en défense en profondeur).
     */
    private suspend fun resolvePlan(groupeId: String): PlanResponse {
        val sub = subscriptionRepo.getActiveSubscriptionByGroupe(groupeId)
            ?: throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Aucun abonnement actif pour ce groupe — opération refusée."
            )
        return planRepo.getPlanById(sub.planId)
            ?: throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Plan ${sub.planId} introuvable pour le groupe $groupeId."
            )
    }

    private fun deny(resource: String, current: Int, max: Int, planNom: String): Nothing =
        throw ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Quota $resource atteint ($current/$max) sur le plan « $planNom ». " +
                "Mettez à niveau votre abonnement pour continuer."
        )
}
