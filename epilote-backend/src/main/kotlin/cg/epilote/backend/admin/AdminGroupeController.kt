package cg.epilote.backend.admin

import cg.epilote.backend.admin.quota.QuotaGuard
import jakarta.validation.Valid
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminGroupeController(
    private val repo: AdminRepository,
    private val categoryRepo: AdminCategoryRepository,
    private val analyticsRepo: AdminGroupeAnalyticsRepository,
    private val kpiRepo: AdminGroupeKpiRepository,
    private val passwordEncoder: PasswordEncoder,
    private val sgClient: AppServicesClient,
    private val communicationRepo: AdminCommunicationRepository,
    private val quotaGuard: QuotaGuard,
    private val subscriptionRepo: AdminSubscriptionRepository
) {
    private fun Authentication.userId() = principal as String

    private fun normalizeDashboardCategoryCode(code: String): String = when (code.trim().lowercase()) {
        "scolarite", "scolarisation" -> "scolarisation"
        "finance", "finances" -> "finance"
        "rh", "personnel", "ressources-humaines", "ressources_humaines" -> "rh"
        else -> code.trim().lowercase()
    }

    private fun isCurrentSubscription(subscription: SubscriptionResponse): Boolean =
        subscription.statut.equals("active", ignoreCase = true) &&
            (subscription.dateFin == 0L || subscription.dateFin >= System.currentTimeMillis())

    private suspend fun resolvePlanIdForGroupe(groupeId: String, fallbackPlanId: String? = null): String? {
        val subscriptionPlanId = subscriptionRepo.getActiveSubscriptionByGroupe(groupeId)
            ?.takeIf { subscription -> isCurrentSubscription(subscription) }
            ?.planId
            ?.takeIf { planId -> planId.isNotBlank() }
        return subscriptionPlanId
            ?: fallbackPlanId?.takeIf { planId -> planId.isNotBlank() }
            ?: repo.getGroupeById(groupeId)?.planId?.takeIf { planId -> planId.isNotBlank() }
    }

    private suspend fun resolveAvailableModules(groupeId: String, fallbackPlanId: String? = null): List<ModuleResponse> {
        val planId = resolvePlanIdForGroupe(groupeId, fallbackPlanId) ?: return emptyList()
        val plan = repo.getPlanById(planId) ?: return emptyList()
        val allowed = plan.modulesIncluded.toSet()
        return repo.listModules()
            .filter { module -> module.isActive && module.code in allowed }
            .sortedBy { module -> module.ordre }
    }

    // ── Admin Groupe : Dashboard Stats ──────────────────────────

    @GetMapping("/api/groupes/{groupeId}/dashboard-stats")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun getGroupeDashboardStats(@PathVariable groupeId: String): ResponseEntity<GroupeDashboardStatsResponse> = runBlocking {
        coroutineScope {
            val groupe = repo.getGroupeById(groupeId)
                ?: return@coroutineScope ResponseEntity.notFound().build()
            val subscription = runCatching { subscriptionRepo.getActiveSubscriptionByGroupe(groupeId) }.getOrNull()
            val activeSubscription = subscription?.takeIf { current -> isCurrentSubscription(current) }
            val resolvedPlanId = resolvePlanIdForGroupe(groupeId, groupe.planId) ?: groupe.planId
            val plan = runCatching { repo.getPlanById(resolvedPlanId) }.getOrNull()
            val dEcoles = async { runCatching { repo.listEcolesByGroupe(groupeId) }.getOrDefault(emptyList()) }
            val dModules = async { runCatching { resolveAvailableModules(groupeId, groupe.planId) }.getOrDefault(emptyList()) }
            val dNbUsers = async { runCatching { repo.countUsersByGroupeId(groupeId) }.getOrDefault(0L) }
            val dNbProfils = async { runCatching { repo.countProfilsByGroupe(groupeId) }.getOrDefault(0L) }
            val dFactTotal = async { runCatching { repo.revenueTotalByGroupe(groupeId) }.getOrDefault(0L) }
            val dFactPaid = async { runCatching { repo.revenuePaidByGroupe(groupeId) }.getOrDefault(0L) }
            val dNbFactures = async { runCatching { repo.countInvoicesByGroupe(groupeId) }.getOrDefault(0L) }
            val dNbOverdue = async { runCatching { repo.countInvoicesOverdueByGroupe(groupeId) }.getOrDefault(0L) }
            val dLastInvoice = async { runCatching { repo.getLatestInvoiceByGroupe(groupeId) }.getOrNull() }
            awaitAll(dEcoles, dModules, dNbUsers, dNbProfils, dFactTotal, dFactPaid, dNbFactures, dNbOverdue, dLastInvoice)
            val ecoles = dEcoles.await()
            val modules = dModules.await()
            val factTotal = dFactTotal.await()
            val factPaid = dFactPaid.await()
            val abonnementStatut = activeSubscription?.statut ?: subscription?.statut ?: "pending"
            val response = GroupeDashboardStatsResponse(
                groupeId = groupeId,
                groupeNom = groupe.nom,
                province = groupe.department ?: "",
                planId = plan?.id ?: resolvedPlanId,
                planNom = plan?.nom ?: resolvedPlanId,
                planType = plan?.type ?: "gratuit",
                prixXAF = plan?.prixXAF ?: 0L,
                abonnementStatut = abonnementStatut,
                abonnementDateDebut = activeSubscription?.dateDebut ?: subscription?.dateDebut ?: 0L,
                abonnementDateFin = activeSubscription?.dateFin ?: subscription?.dateFin ?: 0L,
                renouvellementAuto = activeSubscription?.renouvellementAuto ?: subscription?.renouvellementAuto ?: false,
                nbEcoles = ecoles.size.toLong(),
                nbUtilisateurs = dNbUsers.await(),
                nbProfils = dNbProfils.await(),
                nbModulesActifs = modules.size.toLong(),
                modulesActifs = modules.map { it.nom },
                ecoles = ecoles,
                facturationTotaleXAF = factTotal,
                montantPayeXAF = factPaid,
                montantDuXAF = (factTotal - factPaid).coerceAtLeast(0L),
                nbFactures = dNbFactures.await(),
                nbFacturesEnRetard = dNbOverdue.await(),
                derniereFacture = dLastInvoice.await(),
                quotaEcoles = plan?.maxEcoles ?: 3,
                quotaEleves = plan?.maxStudents ?: 100,
                quotaUtilisateurs = plan?.maxPersonnel ?: 50,
                nbEleves = 0L,
                nbClasses = 0L
            )
            ResponseEntity.ok(response)
        }
    }

    // ── Admin Groupe : Demande abonnement ─────────────────────────

    @PostMapping("/api/groupes/{groupeId}/subscription-request")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun createSubscriptionRequest(
        @PathVariable groupeId: String,
        @Valid @RequestBody req: SubscriptionRequestBody,
        auth: Authentication
    ): ResponseEntity<Map<String, Boolean>> = runBlocking {
        val groupe = repo.getGroupeById(groupeId)
            ?: return@runBlocking ResponseEntity.notFound().build()
        val success = communicationRepo.createSubscriptionRequest(
            groupeId = groupeId,
            groupeNom = groupe.nom,
            requestType = req.type,
            requestMessage = req.message,
            createdBy = auth.userId()
        )
        if (success) ResponseEntity.ok(mapOf("success" to true))
        else ResponseEntity.badRequest().body(mapOf("success" to false))
    }

    // ── Admin Groupe : Écoles ────────────────────────────────────

    @PostMapping("/api/groupes/{groupeId}/ecoles")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun createEcole(
        @PathVariable groupeId: String,
        @Valid @RequestBody req: CreateEcoleRequest
    ): ResponseEntity<EcoleResponse> = runBlocking {
        val groupe = repo.getGroupeById(groupeId)
            ?: return@runBlocking ResponseEntity.notFound().build()
        // Quota guard (correctif #1) : refuse 403 si plan.maxEcoles atteint.
        quotaGuard.assertCanCreateEcole(groupeId)
        val resolvedPlanId = resolvePlanIdForGroupe(groupeId, groupe.planId) ?: groupe.planId
        val ecole = repo.createEcole(groupeId, req, resolvedPlanId)
        val schoolIds = repo.listEcolesByGroupe(groupeId).map { it.id }
        repo.listAdminGroupesByGroupe(groupeId).forEach { admin ->
            runCatching { sgClient.provisionUser(admin.id, groupeId, schoolIds, "ADMIN_GROUPE") }
        }
        ResponseEntity.status(HttpStatus.CREATED).body(ecole)
    }

    @GetMapping("/api/groupes/{groupeId}/ecoles")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun listEcoles(@PathVariable groupeId: String): ResponseEntity<List<EcoleResponse>> =
        runBlocking { ResponseEntity.ok(repo.listEcolesByGroupe(groupeId)) }

    // ── Admin Groupe : Modules disponibles (filtrés par plan) ─────

    @GetMapping("/api/groupes/{groupeId}/modules-disponibles")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun listModulesDisponibles(@PathVariable groupeId: String): ResponseEntity<List<ModuleResponse>> =
        runBlocking { ResponseEntity.ok(resolveAvailableModules(groupeId)) }

    @GetMapping("/api/groupes/{groupeId}/categories-disponibles")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun listCategoriesDisponibles(@PathVariable groupeId: String): ResponseEntity<List<CategorieWithModulesResponse>> = runBlocking {
        val modules = runCatching { resolveAvailableModules(groupeId) }.getOrDefault(emptyList())
            .filter { it.isActive }
        val categories = runCatching { categoryRepo.listCategories() }.getOrDefault(emptyList())
            .filter { it.isActive }

        val categoriesByCode = categories.associateBy { it.code }
        val byCategory = modules.groupBy { it.categorieCode }

        val response = byCategory.mapNotNull { (code, mods) ->
            val cat = categoriesByCode[code]
            val nom = cat?.nom ?: code
            val ordre = cat?.ordre ?: 0
            if (mods.isEmpty()) return@mapNotNull null
            CategorieWithModulesResponse(
                code = normalizeDashboardCategoryCode(code),
                nom = nom,
                ordre = ordre,
                modules = mods.sortedBy { it.ordre }
            )
        }
            .groupBy { category -> category.code }
            .values
            .map { grouped ->
                val first = grouped.minByOrNull { category -> category.ordre } ?: grouped.first()
                first.copy(
                    modules = grouped
                        .flatMap { category -> category.modules }
                        .distinctBy { module -> module.code }
                        .sortedBy { module -> module.ordre }
                )
            }
            .sortedWith(compareBy<CategorieWithModulesResponse> { it.ordre }.thenBy { it.nom.lowercase() })

        ResponseEntity.ok(response)
    }

    // ── Admin Groupe : Profils ───────────────────────────────────

    @PostMapping("/api/groupes/{groupeId}/profils")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun createProfil(
        @PathVariable groupeId: String,
        @Valid @RequestBody req: CreateProfilRequest,
        auth: Authentication
    ): ResponseEntity<ProfilResponse> = runBlocking {
        ResponseEntity.status(HttpStatus.CREATED).body(repo.createProfil(groupeId, req, auth.userId()))
    }

    @GetMapping("/api/groupes/{groupeId}/profils")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun listProfils(@PathVariable groupeId: String): ResponseEntity<List<ProfilResponse>> =
        runBlocking { ResponseEntity.ok(repo.listProfilsByGroupe(groupeId)) }

    // ── Admin Groupe : Utilisateurs ──────────────────────────────

    @PostMapping("/api/groupes/{groupeId}/users")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun createUser(
        @PathVariable groupeId: String,
        @Valid @RequestBody req: CreateUserRequest
    ): ResponseEntity<UserResponse> = runBlocking {
        val profil = repo.getProfilById(req.profilId)
            ?: return@runBlocking ResponseEntity.badRequest().build<UserResponse>()
        // Quota guard (correctif #1) : refuse 403 si plan.maxPersonnel atteint.
        quotaGuard.assertCanCreateUser(groupeId)
        val hash = passwordEncoder.encode(req.password)
        val user = repo.createUser(groupeId, req, hash, profil.permissions)
        runCatching { sgClient.provisionUser(user.id, groupeId, listOf(req.schoolId), "USER") }
        ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    @GetMapping("/api/schools/{schoolId}/users")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun listUsers(@PathVariable schoolId: String): ResponseEntity<List<UserResponse>> =
        runBlocking { ResponseEntity.ok(repo.listUsersByEcole(schoolId)) }

    @PutMapping("/api/groupes/{groupeId}/users/{userId}/assign-profil")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun assignProfil(
        @PathVariable userId: String,
        @Valid @RequestBody req: AssignProfilRequest
    ): ResponseEntity<Any> = runBlocking {
        val profil = repo.getProfilById(req.profilId)
            ?: return@runBlocking ResponseEntity.badRequest().body(mapOf("error" to "Profil introuvable"))
        repo.assignProfilToUser(userId, req.profilId, profil.permissions)
        ResponseEntity.ok(mapOf("message" to "Profil assigné", "profilId" to req.profilId))
    }

    @GetMapping("/api/groupes/{groupeId}/invoice-timeline")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun invoiceTimeline(@PathVariable groupeId: String): ResponseEntity<List<MonthlyInvoiceStats>> = runBlocking {
        ResponseEntity.ok(analyticsRepo.invoiceTimelineByGroupe(groupeId))
    }

    @GetMapping("/api/groupes/{groupeId}/activity-timeline")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun activityTimeline(@PathVariable groupeId: String): ResponseEntity<List<MonthlyActivityStats>> = runBlocking {
        ResponseEntity.ok(analyticsRepo.activityTimelineByGroupe(groupeId))
    }

    @GetMapping("/api/groupes/{groupeId}/notifications")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun getNotifications(@PathVariable groupeId: String): ResponseEntity<List<GroupeNotificationResponse>> = runBlocking {
        val notifications = mutableListOf<GroupeNotificationResponse>()
        val now = System.currentTimeMillis()
        val subscription = runCatching { subscriptionRepo.getActiveSubscriptionByGroupe(groupeId) }.getOrNull()
        if (subscription != null) {
            val daysLeft = ((subscription.dateFin - now) / (24 * 3600 * 1000)).toInt()
            if (daysLeft in 1..30) {
                notifications += GroupeNotificationResponse(
                    id = "notif_plan_expiring",
                    type = "warning",
                    titre = "Abonnement bientôt expiré",
                    message = "Votre abonnement expire dans $daysLeft jour${if (daysLeft > 1) "s" else ""}.",
                    date = now,
                    category = "systeme"
                )
            }
            if (subscription.dateFin in 1..now) {
                notifications += GroupeNotificationResponse(
                    id = "notif_plan_expired",
                    type = "error",
                    titre = "Abonnement expiré",
                    message = "Votre abonnement est expiré. Contactez E-PILOTE Congo.",
                    date = now,
                    category = "systeme"
                )
            }
        }
        val overdueCount = runCatching { repo.countInvoicesOverdueByGroupe(groupeId) }.getOrDefault(0L)
        if (overdueCount > 0) {
            notifications += GroupeNotificationResponse(
                id = "notif_overdue_invoices",
                type = "error",
                titre = "$overdueCount facture${if (overdueCount > 1) "s" else ""} en retard",
                message = "Régularisez votre situation pour éviter une suspension.",
                date = now,
                category = "financier"
            )
        }
        ResponseEntity.ok(notifications)
    }

    // ── Admin Groupe : KPI par catégorie de module ───────────────

    @GetMapping("/api/groupes/{groupeId}/kpi/{category}")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun getModuleKpi(
        @PathVariable groupeId: String,
        @PathVariable category: String
    ): ResponseEntity<ModuleKpiResponse> = runBlocking {
        val normalizedCategory = normalizeDashboardCategoryCode(category)
        val response = when (normalizedCategory) {
            "scolarisation" -> {
                val kpi = runCatching { kpiRepo.scolarisationKpi(groupeId) }.getOrNull()
                ModuleKpiResponse(category = normalizedCategory, scolarisation = kpi)
            }
            "finance" -> {
                val kpi = runCatching { kpiRepo.financeKpi(groupeId) }.getOrNull()
                ModuleKpiResponse(category = normalizedCategory, finance = kpi)
            }
            "rh" -> {
                val kpi = runCatching { kpiRepo.rhKpi(groupeId) }.getOrNull()
                ModuleKpiResponse(category = normalizedCategory, rh = kpi)
            }
            else -> ModuleKpiResponse(category = normalizedCategory)
        }
        ResponseEntity.ok(response)
    }
}
