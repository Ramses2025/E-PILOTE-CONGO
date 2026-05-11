package cg.epilote.backend.admin

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
class AdminController(
    private val repo: AdminRepository,
    private val categoryRepo: AdminCategoryRepository,
    private val planRepo: AdminPlanRepository,
    private val subscriptionRepo: AdminSubscriptionRepository,
    private val moduleRepo: AdminModuleRepository,
    private val auditRepo: AdminAuditLogRepository,
    private val platformIdentityRepo: AdminPlatformIdentityRepository,
    private val auditHelper: AdminAuditHelper
) {
    private fun Authentication.userId() = principal as String

    // ── Dashboard Stats ──────────────────────────────────────────

    @GetMapping("/api/super-admin/dashboard-stats")
    fun getDashboardStats(): ResponseEntity<DashboardStatsResponse> = runBlocking {
        coroutineScope {
            val dGroupes       = async { repo.countGroupes() }
            val dGroupesActifs = async { repo.countGroupesActifs() }
            val dEcoles        = async { repo.countEcoles() }
            val dUsers         = async { repo.countUsers() }
            val dModules       = async { moduleRepo.countModules() }
            val dPlans         = async { planRepo.listPlans() }
            val dCategories    = async { categoryRepo.listCategories() }
            val dTotalSubs     = async { subscriptionRepo.countSubscriptions() }
            val dActiveSubs    = async { subscriptionRepo.countActiveSubscriptions() }
            val dTotalInv      = async { repo.countInvoices() }
            val dRevTotal      = async { repo.revenueTotal() }
            val dRevPaid       = async { repo.revenuePaid() }
            val dInvOverdue    = async { repo.countInvoicesOverdue() }
            val dByProvince    = async { repo.groupesByProvince() }
            val dPlanDist      = async { repo.planDistribution() }
            val dSubsByStatus  = async { subscriptionRepo.subscriptionsByStatus() }
            val dRecentG       = async { repo.recentGroupes(5) }
            val dRecentI       = async { repo.recentInvoices(5) }
            awaitAll(dGroupes, dGroupesActifs, dEcoles, dUsers, dModules,
                dPlans, dCategories, dTotalSubs, dActiveSubs, dTotalInv,
                dRevTotal, dRevPaid, dInvOverdue, dByProvince, dPlanDist,
                dSubsByStatus, dRecentG, dRecentI)
            ResponseEntity.ok(
                DashboardStatsResponse(
                    totalGroupes          = dGroupes.await(),
                    totalEcoles           = dEcoles.await(),
                    totalUtilisateurs     = dUsers.await(),
                    totalModules          = dModules.await(),
                    totalPlans            = dPlans.await().size.toLong(),
                    totalCategories       = dCategories.await().size.toLong(),
                    totalSubscriptions    = dTotalSubs.await(),
                    activeSubscriptions   = dActiveSubs.await(),
                    totalInvoices         = dTotalInv.await(),
                    revenueTotal          = dRevTotal.await(),
                    revenuePaid           = dRevPaid.await(),
                    invoicesOverdue       = dInvOverdue.await(),
                    plans                 = dPlans.await(),
                    categories            = dCategories.await(),
                    groupesActifs         = dGroupesActifs.await(),
                    groupesByProvince     = dByProvince.await(),
                    planDistribution      = dPlanDist.await(),
                    subscriptionsByStatus = dSubsByStatus.await(),
                    recentGroupes         = dRecentG.await(),
                    recentInvoices        = dRecentI.await()
                )
            )
        }
    }

    // ── Catégories (CRUD dynamique) ─────────────────────────────

    @GetMapping("/api/super-admin/categories")
    @PreAuthorize("isAuthenticated()")
    fun listCategories(): ResponseEntity<List<CategorieInfo>> =
        runBlocking { ResponseEntity.ok(categoryRepo.listCategories()) }

    @PostMapping("/api/super-admin/categories")
    fun createCategorie(@Valid @RequestBody req: CreateCategorieRequest): ResponseEntity<CategorieInfo> =
        runBlocking { ResponseEntity.status(HttpStatus.CREATED).body(categoryRepo.createCategorie(req)) }

    @PutMapping("/api/super-admin/categories/{code}")
    fun updateCategorie(
        @PathVariable code: String,
        @Valid @RequestBody req: UpdateCategorieRequest
    ): ResponseEntity<CategorieInfo> = runBlocking {
        categoryRepo.updateCategorie(code, req)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    // ── Super Admin : Plans ──────────────────────────────────────

    @PostMapping("/api/super-admin/plans")
    fun createPlan(@Valid @RequestBody req: CreatePlanRequest): ResponseEntity<PlanResponse> =
        runBlocking { ResponseEntity.status(HttpStatus.CREATED).body(planRepo.createPlan(req)) }

    @GetMapping("/api/super-admin/plans")
    fun listPlans(): ResponseEntity<List<PlanResponse>> =
        runBlocking { ResponseEntity.ok(planRepo.listPlans()) }

    @PutMapping("/api/super-admin/plans/{planId}")
    fun updatePlan(
        @PathVariable planId: String,
        @Valid @RequestBody req: UpdatePlanRequest
    ): ResponseEntity<PlanResponse> = runBlocking {
        planRepo.updatePlan(planId, req)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    // ── Super Admin : Modules ────────────────────────────────────

    @PostMapping("/api/super-admin/modules")
    fun createModule(@Valid @RequestBody req: CreateModuleRequest): ResponseEntity<ModuleResponse> =
        runBlocking { ResponseEntity.status(HttpStatus.CREATED).body(moduleRepo.createModule(req)) }

    @GetMapping("/api/super-admin/modules")
    @PreAuthorize("isAuthenticated()")
    fun listModules(): ResponseEntity<List<ModuleResponse>> =
        runBlocking { ResponseEntity.ok(moduleRepo.listModules()) }

    @PutMapping("/api/super-admin/modules/{moduleId}")
    fun updateModule(
        @PathVariable moduleId: String,
        @Valid @RequestBody req: UpdateModuleRequest
    ): ResponseEntity<ModuleResponse> = runBlocking {
        moduleRepo.updateModule(moduleId, req)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    // ── Super Admin : Audit Logs ─────────────────────────────────

    @GetMapping("/api/super-admin/audit-logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun listAuditLogs(
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @RequestParam(required = false, defaultValue = "50") pageSize: Int,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) action: String?,
        @RequestParam(required = false) outcome: String?,
        @RequestParam(required = false) actorId: String?,
        @RequestParam(required = false) targetId: String?,
        @RequestParam(required = false) since: Long?,
        @RequestParam(required = false) until: Long?,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<AuditLogPage> = runBlocking {
        ResponseEntity.ok(
            auditRepo.list(
                page = page, pageSize = pageSize,
                category = category, action = action, outcome = outcome,
                actorId = actorId, targetId = targetId,
                since = since, until = until, search = search
            )
        )
    }

    @GetMapping("/api/super-admin/audit-logs/actions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun listAuditActions(): ResponseEntity<List<Map<String, String>>> =
        ResponseEntity.ok(
            AuditAction.values().map {
                mapOf("code" to it.code, "category" to it.category.code, "label" to it.label)
            }
        )

    // ── Super Admin : Maintenance abonnements ────────────────────

    @GetMapping("/api/super-admin/subscriptions/expiring-soon")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun listExpiringSubscriptions(
        @RequestParam(required = false, defaultValue = "7") days: Int
    ): ResponseEntity<List<SubscriptionResponse>> = runBlocking {
        val safeDays = days.coerceIn(1, 365)
        val now = System.currentTimeMillis()
        val horizon = now + safeDays.toLong() * 24L * 3600L * 1000L
        val expiring = subscriptionRepo.listSubscriptions().filter { sub ->
            sub.statut == "active" && sub.dateFin in (now + 1)..horizon
        }.sortedBy { it.dateFin }
        ResponseEntity.ok(expiring)
    }

    @PostMapping("/api/super-admin/subscriptions/run-expiry-check")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun runExpiryCheck(
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> = runBlocking {
        val suspended = runCatching { subscriptionRepo.suspendExpiredSubscriptions() }.getOrElse { emptyList() }
        auditHelper.audit(AuditAction.SCHEDULER_EXPIRY_RUN, auth, httpReq,
            message = "Job d'expiration déclenché manuellement — ${suspended.size} groupe(s) suspendu(s)",
            metadata = mapOf("trigger" to "manual", "suspendedGroupIds" to suspended))
        suspended.forEach { gid ->
            auditHelper.audit(AuditAction.SUBSCRIPTION_AUTO_SUSPENDED, auth, httpReq,
                targetType = "groupe", targetId = gid,
                message = "Abonnement suspendu (échéance dépassée)",
                metadata = mapOf("trigger" to "manual"))
        }
        ResponseEntity.ok(mapOf("suspendedCount" to suspended.size, "suspendedGroupIds" to suspended))
    }

    // ── Super Admin : Identité Plateforme (Paramètres) ───────────

    @GetMapping("/api/super-admin/platform-identity")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun getPlatformIdentity(): ResponseEntity<PlatformIdentity> =
        runBlocking { ResponseEntity.ok(platformIdentityRepo.read()) }

    @PutMapping("/api/super-admin/platform-identity")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun updatePlatformIdentity(
        @Valid @RequestBody req: UpdatePlatformIdentityRequest,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<PlatformIdentity> = runBlocking {
        val updated = platformIdentityRepo.update(req)
        auditHelper.audit(AuditAction.PLATFORM_IDENTITY_UPDATED, auth, httpReq,
            targetType = "platform", targetId = "config::platform_identity",
            targetLabel = updated.raisonSociale,
            message = "Paramètres plateforme mis à jour")
        ResponseEntity.ok(updated)
    }

    // ── Super Admin : Annonces Globales ──────────────────────────

    @PostMapping("/api/super-admin/announcements")
    fun createAnnouncement(
        @Valid @RequestBody req: CreateAnnouncementRequest,
        auth: Authentication
    ): ResponseEntity<AnnouncementResponse> = runBlocking {
        ResponseEntity.status(HttpStatus.CREATED).body(repo.createAnnouncement(req, auth.userId()))
    }

    @GetMapping("/api/super-admin/announcements")
    fun listAnnouncements(): ResponseEntity<List<AnnouncementResponse>> =
        runBlocking { ResponseEntity.ok(repo.listAnnouncements()) }
}
