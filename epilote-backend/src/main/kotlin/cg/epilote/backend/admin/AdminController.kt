package cg.epilote.backend.admin

import jakarta.validation.Valid
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.*
import org.springframework.security.crypto.password.PasswordEncoder

@RestController
class AdminController(
    private val repo: AdminRepository,
    private val categoryRepo: AdminCategoryRepository,
    private val planRepo: AdminPlanRepository,
    private val subscriptionRepo: AdminSubscriptionRepository,
    private val invoicePdfService: AdminInvoicePdfService,
    private val moduleRepo: AdminModuleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val sgClient: AppServicesClient
) {
    private val allowedInvoiceStatuses = setOf("draft", "sent", "paid", "overdue", "cancelled")

    private fun Authentication.userId() = principal as String

    @Suppress("UNCHECKED_CAST")
    private fun Authentication.details() = details as? Map<String, String> ?: emptyMap()

    // ── Dashboard Stats ──────────────────────────────────────────

    @GetMapping("/api/super-admin/dashboard-stats")
    fun getDashboardStats(): ResponseEntity<DashboardStatsResponse> = runBlocking {
        coroutineScope {
            val dGroupes      = async { repo.countGroupes() }
            val dGroupesActifs = async { repo.countGroupesActifs() }
            val dEcoles       = async { repo.countEcoles() }
            val dUsers        = async { repo.countUsers() }
            val dModules      = async { moduleRepo.countModules() }
            val dPlans        = async { planRepo.listPlans() }
            val dCategories   = async { categoryRepo.listCategories() }
            val dTotalSubs    = async { subscriptionRepo.countSubscriptions() }
            val dActiveSubs   = async { subscriptionRepo.countActiveSubscriptions() }
            val dTotalInv     = async { repo.countInvoices() }
            val dRevTotal     = async { repo.revenueTotal() }
            val dRevPaid      = async { repo.revenuePaid() }
            val dInvOverdue   = async { repo.countInvoicesOverdue() }
            val dByProvince   = async { repo.groupesByProvince() }
            val dPlanDist     = async { repo.planDistribution() }
            val dSubsByStatus = async { subscriptionRepo.subscriptionsByStatus() }
            val dRecentG      = async { repo.recentGroupes(5) }
            val dRecentI      = async { repo.recentInvoices(5) }

            awaitAll(dGroupes, dGroupesActifs, dEcoles, dUsers, dModules,
                dPlans, dCategories, dTotalSubs, dActiveSubs, dTotalInv,
                dRevTotal, dRevPaid, dInvOverdue, dByProvince, dPlanDist,
                dSubsByStatus, dRecentG, dRecentI)

            ResponseEntity.ok(
                DashboardStatsResponse(
                    totalGroupes         = dGroupes.await(),
                    totalEcoles          = dEcoles.await(),
                    totalUtilisateurs    = dUsers.await(),
                    totalModules         = dModules.await(),
                    totalPlans           = dPlans.await().size.toLong(),
                    totalCategories      = dCategories.await().size.toLong(),
                    totalSubscriptions   = dTotalSubs.await(),
                    activeSubscriptions  = dActiveSubs.await(),
                    totalInvoices        = dTotalInv.await(),
                    revenueTotal         = dRevTotal.await(),
                    revenuePaid          = dRevPaid.await(),
                    invoicesOverdue      = dInvOverdue.await(),
                    plans                = dPlans.await(),
                    categories           = dCategories.await(),
                    groupesActifs        = dGroupesActifs.await(),
                    groupesByProvince    = dByProvince.await(),
                    planDistribution     = dPlanDist.await(),
                    subscriptionsByStatus = dSubsByStatus.await(),
                    recentGroupes        = dRecentG.await(),
                    recentInvoices       = dRecentI.await()
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

    // ── Super Admin : Groupes ────────────────────────────────────

    @PostMapping("/api/super-admin/groupes")
    fun createGroupe(
        @Valid @RequestBody req: CreateGroupeRequest,
        auth: Authentication
    ): ResponseEntity<GroupeResponse> = runBlocking {
        ResponseEntity.status(HttpStatus.CREATED).body(repo.createGroupe(req, auth.userId()))
    }

    @GetMapping("/api/super-admin/groupes")
    fun listGroupes(): ResponseEntity<List<GroupeResponse>> =
        runBlocking { ResponseEntity.ok(repo.listGroupes()) }

    @PostMapping("/api/super-admin/groupes/{groupeId}/admins")
    fun createAdminGroupe(
        @PathVariable groupeId: String,
        @Valid @RequestBody req: CreateAdminGroupeRequest
    ): ResponseEntity<UserResponse> = runBlocking {
        repo.getGroupeById(groupeId)
            ?: return@runBlocking ResponseEntity.notFound().build()
        val hash = passwordEncoder.encode(req.password)
        val admin = repo.createAdminGroupe(groupeId, req, hash)
        val schoolIds = repo.listEcolesByGroupe(groupeId).map { it.id }
        runCatching { sgClient.provisionUser(admin.id, groupeId, schoolIds, "ADMIN_GROUPE") }
        ResponseEntity.status(HttpStatus.CREATED).body(admin)
    }

    @GetMapping("/api/super-admin/groupes/{groupeId}/admins")
    fun listAdminGroupes(@PathVariable groupeId: String): ResponseEntity<List<UserResponse>> =
        runBlocking { ResponseEntity.ok(repo.listAdminGroupesByGroupe(groupeId)) }

    @PutMapping("/api/super-admin/groupes/{groupeId}")
    fun updateGroupe(
        @PathVariable groupeId: String,
        @Valid @RequestBody req: UpdateGroupeRequest
    ): ResponseEntity<GroupeResponse> = runBlocking {
        repo.updateGroupe(groupeId, req)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @DeleteMapping("/api/super-admin/groupes/{groupeId}")
    fun deleteGroupe(@PathVariable groupeId: String): ResponseEntity<Any> = runBlocking {
        if (repo.deleteGroupe(groupeId)) ResponseEntity.noContent().build<Any>()
        else ResponseEntity.notFound().build()
    }

    // ── Super Admin : Abonnements ────────────────────────────────

    @PostMapping("/api/super-admin/subscriptions")
    fun createSubscription(
        @Valid @RequestBody req: CreateSubscriptionRequest
    ): ResponseEntity<SubscriptionResponse> = runBlocking {
        val plan = planRepo.getPlanById(req.planId)
            ?: return@runBlocking ResponseEntity.badRequest().build()
        subscriptionRepo.createSubscription(req.copy(planId = plan.id))
            ?.let { ResponseEntity.status(HttpStatus.CREATED).body(it) }
            ?: ResponseEntity.badRequest().build()
    }

    @GetMapping("/api/super-admin/subscriptions")
    fun listSubscriptions(): ResponseEntity<List<SubscriptionResponse>> =
        runBlocking { ResponseEntity.ok(subscriptionRepo.listSubscriptions()) }

    @PutMapping("/api/super-admin/subscriptions/{subId}/status")
    fun updateSubscriptionStatus(
        @PathVariable subId: String,
        @RequestParam statut: String
    ): ResponseEntity<SubscriptionResponse> = runBlocking {
        subscriptionRepo.updateSubscriptionStatus(subId, statut)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    // ── Super Admin : Factures Plateforme ─────────────────────────

    @PostMapping("/api/super-admin/invoices")
    fun createInvoice(
        @Valid @RequestBody req: CreateInvoiceRequest
    ): ResponseEntity<InvoiceResponse> = runBlocking {
        val subscription = subscriptionRepo.getSubscriptionById(req.subscriptionId)
            ?: return@runBlocking ResponseEntity.badRequest().build()
        if (subscription.groupeId != req.groupeId) {
            return@runBlocking ResponseEntity.badRequest().build()
        }
        val now = System.currentTimeMillis()
        if (subscription.statut == "cancelled" || subscription.dateFin < now) {
            return@runBlocking ResponseEntity.badRequest().build()
        }
        ResponseEntity.status(HttpStatus.CREATED).body(repo.createInvoice(req))
    }

    @GetMapping("/api/super-admin/invoices")
    fun listInvoices(): ResponseEntity<List<InvoiceResponse>> =
        runBlocking { ResponseEntity.ok(repo.listInvoices()) }

    @GetMapping("/api/super-admin/invoices/{invoiceId}/pdf")
    fun downloadInvoicePdf(
        @PathVariable invoiceId: String
    ): ResponseEntity<ByteArrayResource> = runBlocking {
        val pdf = invoicePdfService.generateInvoicePdf(invoiceId)
            ?: return@runBlocking ResponseEntity.notFound().build()
        val resource = ByteArrayResource(pdf.bytes)
        ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(pdf.fileName).build().toString()
            )
            .contentType(MediaType.APPLICATION_PDF)
            .contentLength(pdf.bytes.size.toLong())
            .body(resource)
    }

    @PutMapping("/api/super-admin/invoices/{invoiceId}/status")
    fun updateInvoiceStatus(
        @PathVariable invoiceId: String,
        @RequestParam statut: String,
        @RequestParam(required = false) datePaiement: Long?
    ): ResponseEntity<InvoiceResponse> = runBlocking {
        val normalizedStatus = statut.trim().lowercase()
        if (normalizedStatus !in allowedInvoiceStatuses) {
            return@runBlocking ResponseEntity.badRequest().build()
        }
        repo.updateInvoiceStatus(invoiceId, normalizedStatus, datePaiement)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
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

    // ── Admin Groupe : Écoles ────────────────────────────────────

    @PostMapping("/api/groupes/{groupeId}/ecoles")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun createEcole(
        @PathVariable groupeId: String,
        @Valid @RequestBody req: CreateEcoleRequest,
        auth: Authentication
    ): ResponseEntity<EcoleResponse> = runBlocking {
        val groupe = repo.listGroupes().find { it.id == groupeId }
            ?: return@runBlocking ResponseEntity.notFound().build()
        val ecole = repo.createEcole(groupeId, req, groupe.planId)
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
        runBlocking { ResponseEntity.ok(repo.getModulesDisponibles(groupeId)) }

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
        @Valid @RequestBody req: CreateUserRequest,
        auth: Authentication
    ): ResponseEntity<UserResponse> = runBlocking {
        val profil = repo.getProfilById(req.profilId)
            ?: return@runBlocking ResponseEntity.badRequest().build<UserResponse>()
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
        @PathVariable groupeId: String,
        @PathVariable userId: String,
        @Valid @RequestBody req: AssignProfilRequest
    ): ResponseEntity<Any> = runBlocking {
        val profil = repo.getProfilById(req.profilId)
            ?: return@runBlocking ResponseEntity.badRequest().body(mapOf("error" to "Profil introuvable"))
        repo.assignProfilToUser(userId, req.profilId, profil.permissions)
        ResponseEntity.ok(mapOf("message" to "Profil assign\u00e9", "profilId" to req.profilId))
    }

    // ── Super Admin : Administrateurs ────────────────────────────

    @GetMapping("/api/super-admin/admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun listAllAdmins(): ResponseEntity<List<AdminUserResponse>> =
        runBlocking { ResponseEntity.ok(repo.listAllAdmins()) }

    @PostMapping("/api/super-admin/admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun createAdmin(
        @Valid @RequestBody req: CreateAdminRequest
    ): ResponseEntity<AdminUserResponse> = runBlocking {
        val hash = passwordEncoder.encode(req.password)
        val admin = repo.createAdmin(req, hash)
        if (admin.role == "ADMIN_GROUPE" && admin.groupId != null) {
            runCatching { sgClient.provisionUser(admin.id, admin.groupId, emptyList(), "ADMIN_GROUPE") }
        }
        ResponseEntity.status(HttpStatus.CREATED).body(admin)
    }

    @PutMapping("/api/super-admin/admins/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun updateAdmin(
        @PathVariable userId: String,
        @Valid @RequestBody req: UpdateAdminRequest
    ): ResponseEntity<AdminUserResponse> = runBlocking {
        repo.updateAdmin(userId, req)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @DeleteMapping("/api/super-admin/admins/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun deleteAdmin(@PathVariable userId: String): ResponseEntity<Any> = runBlocking {
        if (repo.deleteAdmin(userId)) {
            runCatching { sgClient.disableUser(userId) }
            ResponseEntity.noContent().build<Any>()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/api/super-admin/admins/{userId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun toggleAdminStatus(
        @PathVariable userId: String,
        @Valid @RequestBody req: ToggleAdminStatusRequest
    ): ResponseEntity<AdminUserResponse> = runBlocking {
        repo.toggleAdminStatus(userId, req.status)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }
}
