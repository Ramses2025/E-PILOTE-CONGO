package cg.epilote.backend.admin

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
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
    private val sgClient: AppServicesClient,
    private val platformIdentityRepo: AdminPlatformIdentityRepository,
    private val paymentReceiptRepo: AdminPaymentReceiptRepository,
    private val paymentClaimRepo: AdminPaymentClaimRepository,
    private val auditRepo: AdminAuditLogRepository
) {
    private val log = LoggerFactory.getLogger(AdminController::class.java)
    private val allowedInvoiceStatuses = setOf("draft", "sent", "paid", "overdue", "cancelled")

    private fun Authentication.userId() = principal as String

    @Suppress("UNCHECKED_CAST")
    private fun Authentication.details() = details as? Map<String, String> ?: emptyMap()

    private fun Authentication.email(): String? = details()["email"]
    private fun Authentication.role(): String? = authorities.firstOrNull()?.authority?.removePrefix("ROLE_")

    private fun ipOf(req: HttpServletRequest?): String? {
        if (req == null) return null
        val xff = req.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
        return xff?.takeIf { it.isNotBlank() } ?: req.remoteAddr
    }

    private suspend fun audit(
        action: AuditAction,
        auth: Authentication?,
        req: HttpServletRequest?,
        outcome: AuditOutcome = AuditOutcome.SUCCESS,
        targetType: String? = null,
        targetId: String? = null,
        targetLabel: String? = null,
        message: String? = null,
        metadata: Map<String, Any?> = emptyMap()
    ) {
        auditRepo.record(
            action = action,
            outcome = outcome,
            actorId = auth?.userId(),
            actorEmail = auth?.email(),
            actorRole = auth?.role(),
            targetType = targetType,
            targetId = targetId,
            targetLabel = targetLabel,
            ipAddress = ipOf(req),
            userAgent = req?.getHeader("User-Agent"),
            message = message,
            metadata = metadata
        )
    }

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
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<GroupeResponse> = runBlocking {
        val created = repo.createGroupe(req, auth.userId())
        audit(AuditAction.GROUPE_CREATED, auth, httpReq,
            targetType = "groupe", targetId = created.id, targetLabel = created.nom,
            message = "Groupe '${created.nom}' créé",
            metadata = mapOf("planId" to created.planId, "city" to created.city, "department" to created.department))
        ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping("/api/super-admin/groupes")
    fun listGroupes(): ResponseEntity<List<GroupeResponse>> =
        runBlocking { ResponseEntity.ok(repo.listGroupes()) }

    @PostMapping("/api/super-admin/groupes/{groupeId}/admins")
    fun createAdminGroupe(
        @PathVariable groupeId: String,
        @Valid @RequestBody req: CreateAdminGroupeRequest,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<UserResponse> = runBlocking {
        repo.getGroupeById(groupeId)
            ?: return@runBlocking ResponseEntity.notFound().build()
        val hash = passwordEncoder.encode(req.password)
        val admin = repo.createAdminGroupe(groupeId, req, hash)
        val schoolIds = repo.listEcolesByGroupe(groupeId).map { it.id }
        runCatching { sgClient.provisionUser(admin.id, groupeId, schoolIds, "ADMIN_GROUPE") }
        audit(AuditAction.ADMIN_CREATED, auth, httpReq,
            targetType = "user", targetId = admin.id, targetLabel = admin.email,
            message = "Admin groupe créé : ${admin.email}",
            metadata = mapOf("groupeId" to groupeId, "role" to "ADMIN_GROUPE"))
        ResponseEntity.status(HttpStatus.CREATED).body(admin)
    }

    @GetMapping("/api/super-admin/groupes/{groupeId}/admins")
    fun listAdminGroupes(@PathVariable groupeId: String): ResponseEntity<List<UserResponse>> =
        runBlocking { ResponseEntity.ok(repo.listAdminGroupesByGroupe(groupeId)) }

    @PutMapping("/api/super-admin/groupes/{groupeId}")
    fun updateGroupe(
        @PathVariable groupeId: String,
        @Valid @RequestBody req: UpdateGroupeRequest,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<GroupeResponse> = runBlocking {
        val updated = repo.updateGroupe(groupeId, req)
        if (updated != null) {
            audit(AuditAction.GROUPE_UPDATED, auth, httpReq,
                targetType = "groupe", targetId = groupeId, targetLabel = updated.nom,
                message = "Groupe '${updated.nom}' modifié")
            ResponseEntity.ok(updated)
        } else ResponseEntity.notFound().build()
    }

    @DeleteMapping("/api/super-admin/groupes/{groupeId}")
    fun deleteGroupe(
        @PathVariable groupeId: String,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<Any> = runBlocking {
        val existing = runCatching { repo.getGroupeById(groupeId) }.getOrNull()
        if (repo.deleteGroupe(groupeId)) {
            audit(AuditAction.GROUPE_DELETED, auth, httpReq,
                targetType = "groupe", targetId = groupeId, targetLabel = existing?.nom,
                message = "Groupe ${existing?.nom ?: groupeId} supprimé")
            ResponseEntity.noContent().build<Any>()
        } else ResponseEntity.notFound().build()
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
        @RequestParam statut: String,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<SubscriptionResponse> = runBlocking {
        val updated = subscriptionRepo.updateSubscriptionStatus(subId, statut)
        if (updated != null) {
            audit(AuditAction.SUBSCRIPTION_STATUS_CHANGED, auth, httpReq,
                targetType = "subscription", targetId = subId,
                targetLabel = updated.groupeId,
                message = "Statut abonnement ${subId} → ${updated.statut}",
                metadata = mapOf("newStatus" to updated.statut, "groupeId" to updated.groupeId))
            ResponseEntity.ok(updated)
        } else ResponseEntity.notFound().build()
    }

    // ── Super Admin : Factures Plateforme ─────────────────────────

    @PostMapping("/api/super-admin/invoices")
    fun createInvoice(
        @Valid @RequestBody req: CreateInvoiceRequest,
        auth: Authentication,
        httpReq: HttpServletRequest
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
        val invoice = repo.createInvoice(req)
        audit(AuditAction.INVOICE_CREATED, auth, httpReq,
            targetType = "invoice", targetId = invoice.id, targetLabel = invoice.reference,
            message = "Facture ${invoice.reference} émise pour groupe ${req.groupeId}",
            metadata = mapOf("montantXAF" to invoice.montantXAF, "groupeId" to invoice.groupeId))
        ResponseEntity.status(HttpStatus.CREATED).body(invoice)
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
        @RequestParam(required = false) datePaiement: Long?,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<InvoiceResponse> = runBlocking {
        val normalizedStatus = statut.trim().lowercase()
        if (normalizedStatus !in allowedInvoiceStatuses) {
            return@runBlocking ResponseEntity.badRequest().build()
        }
        val updated = repo.updateInvoiceStatus(invoiceId, normalizedStatus, datePaiement)
        if (updated != null) {
            audit(AuditAction.INVOICE_STATUS_CHANGED, auth, httpReq,
                targetType = "invoice", targetId = invoiceId, targetLabel = updated.reference,
                message = "Facture ${updated.reference} → ${updated.statut}",
                metadata = mapOf("newStatus" to updated.statut))
            ResponseEntity.ok(updated)
        } else ResponseEntity.notFound().build()
    }

    // ── Super Admin : Audit Logs (journal serveur) ────────────────
    //
    // Trace immuable de chaque action mutante. Stocké cloud-only dans la collection
    // `audit_logs` (jamais syncé vers les bases mobiles — conformité RGPD/CADF).

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
                mapOf(
                    "code" to it.code,
                    "category" to it.category.code,
                    "label" to it.label
                )
            }
        )

    // ── Super Admin : Maintenance abonnements ─────────────────────

    /**
     * Liste les abonnements actifs dont l'échéance arrive dans les `days` jours.
     * Sert au badge d'alerte du Dashboard et aux relances avant suspension.
     */
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

    /**
     * Déclenche manuellement le job de suspension des abonnements expirés
     * (même logique que [SubscriptionExpiryScheduler], lançé à la demande).
     * Renvoie la liste des identifiants de groupes suspendus pendant ce run.
     */
    @PostMapping("/api/super-admin/subscriptions/run-expiry-check")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun runExpiryCheck(
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<Map<String, Any>> = runBlocking {
        val suspended = runCatching { subscriptionRepo.suspendExpiredSubscriptions() }
            .getOrElse { emptyList() }
        audit(AuditAction.SCHEDULER_EXPIRY_RUN, auth, httpReq,
            message = "Job d'expiration déclenché manuellement — ${suspended.size} groupe(s) suspendu(s)",
            metadata = mapOf("trigger" to "manual", "suspendedGroupIds" to suspended))
        suspended.forEach { gid ->
            audit(AuditAction.SUBSCRIPTION_AUTO_SUSPENDED, auth, httpReq,
                targetType = "groupe", targetId = gid,
                message = "Abonnement suspendu (échéance dépassée)",
                metadata = mapOf("trigger" to "manual"))
        }
        ResponseEntity.ok(mapOf(
            "suspendedCount" to suspended.size,
            "suspendedGroupIds" to suspended
        ))
    }

    // ── Super Admin : Identité Plateforme (Paramètres) ───────────
    //
    // Les factures étant des documents officiels et contractuels, l'émetteur doit être
    // identifié sur chaque PDF (raison sociale, RCCM, NIU, siège, IBAN…). Le Super Admin
    // remplit ces informations depuis la page Paramètres. Tant que les champs sont vides,
    // les PDF affichent un placeholder explicite.

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
        audit(AuditAction.PLATFORM_IDENTITY_UPDATED, auth, httpReq,
            targetType = "platform", targetId = "config::platform_identity",
            targetLabel = updated.raisonSociale,
            message = "Paramètres plateforme mis à jour")
        ResponseEntity.ok(updated)
    }

    // ── Super Admin : Paiements présentiels & abonnements ────────
    //
    // Workflow métier :
    //   1) Le groupe scolaire se déplace au siège et paie
    //   2) Le Super Admin enregistre le paiement via POST /api/super-admin/payment-receipts
    //      → crée un PaymentReceipt + met l'abonnement actif/renouvelé + émet une facture
    //   3) L'historique par groupe est consultable via GET .../groupes/{id}/payment-receipts
    //   4) Un job planifié suspend les abonnements arrivés à échéance.

    @GetMapping("/api/super-admin/payment-methods")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun listPaymentMethods(): ResponseEntity<List<Map<String, Any>>> =
        ResponseEntity.ok(
            PaymentMethod.values().map {
                mapOf("code" to it.code, "label" to it.label, "enabled" to it.enabled)
            }
        )

    @PostMapping("/api/super-admin/payment-receipts")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun recordPayment(
        @Valid @RequestBody req: RecordPaymentRequest,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<PaymentReceiptResponse> = runBlocking {
        // ── Validation préalable ──────────────────────────────────
        val method = PaymentMethod.fromCode(req.paymentMethod)
            ?: return@runBlocking ResponseEntity.badRequest().build()
        if (!method.enabled) {
            // TODO: Mobile Money — implémentation prévue en phase ultérieure
            // TODO: Carte bancaire — implémentation prévue en phase ultérieure
            return@runBlocking ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
        }
        val subscription = subscriptionRepo.getSubscriptionById(req.subscriptionId)
            ?: return@runBlocking ResponseEntity.badRequest().build()
        if (subscription.groupeId != req.groupeId) {
            return@runBlocking ResponseEntity.badRequest().build()
        }

        val idempotencyKey = req.idempotencyKey?.takeIf { it.isNotBlank() }

        // ── Étape 1 : Acquisition du claim d'idempotence (KV strong-consistent) ──
        // Pose un doc déterministe `payment_claim::<key>` AVANT toute mutation métier.
        // Référence Couchbase : https://docs.couchbase.com/kotlin-sdk/current/howtos/kv-operations.html#insert
        // Un retry concurrent verra le claim et retournera la réponse cachée ou 409.
        if (idempotencyKey != null) {
            when (val outcome = paymentClaimRepo.claim(idempotencyKey)) {
                is ClaimOutcome.Acquired -> Unit // poursuit l'exécution
                is ClaimOutcome.AlreadyDone -> {
                    val cachedReceipt = outcome.receiptId?.let { paymentReceiptRepo.getById(it) }
                    return@runBlocking if (cachedReceipt != null) {
                        ResponseEntity.ok(cachedReceipt)
                    } else {
                        // Claim marqué `done` mais reçu introuvable (ne devrait pas arriver).
                        // On laisse le client retenter — sécurité défensive.
                        ResponseEntity.status(HttpStatus.CONFLICT).build()
                    }
                }
                is ClaimOutcome.InProgress -> {
                    // Un autre appel concurrent est encore en cours d'exécution.
                    // 409 Conflict + Retry-After signale au client de retenter plus tard.
                    return@runBlocking ResponseEntity.status(HttpStatus.CONFLICT)
                        .header("Retry-After", "2")
                        .build()
                }
                is ClaimOutcome.PreviouslyFailed -> {
                    // Cas rare : transformé en Acquired par le repository, ne devrait
                    // pas remonter ici. Garde-fou pour l'exhaustivité des branches.
                    log.warn("ClaimOutcome.PreviouslyFailed inattendu pour key=$idempotencyKey")
                }
            }
        }

        // ── Étape 2 : Mutations métier sous garde compensatoire ──
        // Si l'une des étapes échoue, on marque le claim comme `failed` pour éviter
        // de bloquer le client (un retry humain du Super Admin pourra reprendre
        // depuis zéro avec une nouvelle clé) et on logge l'incident pour
        // investigation manuelle. Pattern compensating documenté :
        //   - https://stripe.com/docs/api/idempotent_requests
        //   - https://docs.spring.io/spring-framework/reference/data-access/transaction.html
        val paidAt = System.currentTimeMillis()
        val receipt: PaymentReceiptResponse = try {
            // 2.a) Activer/renouveler l'abonnement (dateDebut = maintenant, dateFin = +N mois).
            val activated = subscriptionRepo.activateOrRenew(
                subId = req.subscriptionId,
                durationMonths = req.durationMonths
            ) ?: throw IllegalStateException("Activation de l'abonnement impossible (${req.subscriptionId})")

            // 2.b) Émettre la facture directement en statut "paid".
            val invoice = repo.createInvoice(
                CreateInvoiceRequest(
                    groupeId = req.groupeId,
                    subscriptionId = req.subscriptionId,
                    montantXAF = req.montantXAF,
                    dateEcheance = activated.dateFin,
                    notes = listOfNotNull(
                        "Paiement présentiel — mode : ${method.label}",
                        req.externalReference?.let { "Réf. externe : $it" },
                        req.paidBy?.let { "Payé par : $it" },
                        req.notes.takeIf { it.isNotBlank() }
                    ).joinToString("\n"),
                    initialStatus = "paid",
                    datePaiement = paidAt
                )
            )

            // 2.c) Enregistrer le reçu pour historique.
            val rec = paymentReceiptRepo.record(
                groupeId = req.groupeId,
                subscriptionId = req.subscriptionId,
                invoiceId = invoice.id,
                montantXAF = req.montantXAF,
                method = method,
                externalReference = req.externalReference,
                paidBy = req.paidBy,
                receivedBy = auth.userId(),
                notes = req.notes,
                accessStart = activated.dateDebut,
                accessEnd = activated.dateFin,
                idempotencyKey = idempotencyKey
            )

            // 2.d) Audit (best-effort — un échec ici ne doit pas annuler le paiement).
            audit(AuditAction.PAYMENT_RECORDED, auth, httpReq,
                targetType = "subscription", targetId = req.subscriptionId, targetLabel = req.groupeId,
                message = "Paiement ${req.montantXAF} XAF (${method.label}) enregistré pour groupe ${req.groupeId}",
                metadata = mapOf(
                    "montantXAF" to req.montantXAF,
                    "paymentMethod" to method.code,
                    "durationMonths" to req.durationMonths,
                    "invoiceId" to invoice.id,
                    "invoiceReference" to invoice.reference,
                    "accessEnd" to activated.dateFin
                ))
            audit(AuditAction.SUBSCRIPTION_RENEWED, auth, httpReq,
                targetType = "subscription", targetId = req.subscriptionId, targetLabel = req.groupeId,
                message = "Abonnement renouvelé jusqu'au ${java.time.Instant.ofEpochMilli(activated.dateFin)}",
                metadata = mapOf("newEndDate" to activated.dateFin, "durationMonths" to req.durationMonths))
            rec
        } catch (e: Exception) {
            // Échec mid-flow : on marque le claim comme `failed` (best-effort) et on
            // logge l'incident. L'admin peut investiguer via le journal d'audit ;
            // l'abonnement éventuellement renouvelé reste cohérent (le scheduler
            // d'expiration automatique le suspendra à dateFin si pas de paiement
            // ultérieur ne le confirme).
            log.warn("Échec recordPayment (sub=${req.subscriptionId}, key=$idempotencyKey) : ${e.message}", e)
            if (idempotencyKey != null) {
                runCatching { paymentClaimRepo.markFailed(idempotencyKey, e.message) }
            }
            audit(AuditAction.PAYMENT_RECORDED, auth, httpReq,
                outcome = AuditOutcome.FAILURE,
                targetType = "subscription", targetId = req.subscriptionId, targetLabel = req.groupeId,
                message = "Échec enregistrement paiement : ${e.message}",
                metadata = mapOf(
                    "idempotencyKey" to idempotencyKey,
                    "error" to (e.message ?: e::class.simpleName ?: "unknown")
                ))
            return@runBlocking ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }

        // ── Étape 3 : Marquer le claim `done` (réponse cachée pour les retries) ──
        if (idempotencyKey != null) {
            runCatching { paymentClaimRepo.markDone(idempotencyKey, receipt.id) }
        }

        ResponseEntity.status(HttpStatus.CREATED).body(receipt)
    }

    @GetMapping("/api/super-admin/payment-receipts")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun listPaymentReceipts(): ResponseEntity<List<PaymentReceiptResponse>> =
        runBlocking { ResponseEntity.ok(paymentReceiptRepo.listAll()) }

    @GetMapping("/api/super-admin/groupes/{groupeId}/payment-receipts")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun listGroupePaymentReceipts(
        @PathVariable groupeId: String
    ): ResponseEntity<List<PaymentReceiptResponse>> =
        runBlocking { ResponseEntity.ok(paymentReceiptRepo.listByGroupe(groupeId)) }

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
        @Valid @RequestBody req: CreateAdminRequest,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<AdminUserResponse> = runBlocking {
        val hash = passwordEncoder.encode(req.password)
        val admin = repo.createAdmin(req, hash)
        if (admin.role == "ADMIN_GROUPE" && admin.groupId != null) {
            runCatching { sgClient.provisionUser(admin.id, admin.groupId, emptyList(), "ADMIN_GROUPE") }
        }
        audit(AuditAction.ADMIN_CREATED, auth, httpReq,
            targetType = "user", targetId = admin.id, targetLabel = admin.email,
            message = "Admin ${admin.role} créé : ${admin.email}",
            metadata = mapOf("role" to admin.role, "groupId" to admin.groupId))
        ResponseEntity.status(HttpStatus.CREATED).body(admin)
    }

    @PutMapping("/api/super-admin/admins/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun updateAdmin(
        @PathVariable userId: String,
        @Valid @RequestBody req: UpdateAdminRequest,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<AdminUserResponse> = runBlocking {
        val updated = repo.updateAdmin(userId, req)
        if (updated != null) {
            audit(AuditAction.ADMIN_UPDATED, auth, httpReq,
                targetType = "user", targetId = userId, targetLabel = updated.email,
                message = "Admin ${updated.email} modifié")
            ResponseEntity.ok(updated)
        } else ResponseEntity.notFound().build()
    }

    @DeleteMapping("/api/super-admin/admins/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun deleteAdmin(
        @PathVariable userId: String,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<Any> = runBlocking {
        if (repo.deleteAdmin(userId)) {
            runCatching { sgClient.disableUser(userId) }
            audit(AuditAction.ADMIN_DELETED, auth, httpReq,
                targetType = "user", targetId = userId,
                message = "Admin ${userId} supprimé")
            ResponseEntity.noContent().build<Any>()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/api/super-admin/admins/{userId}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun toggleAdminStatus(
        @PathVariable userId: String,
        @Valid @RequestBody req: ToggleAdminStatusRequest,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<AdminUserResponse> = runBlocking {
        val updated = repo.toggleAdminStatus(userId, req.status)
        if (updated != null) {
            audit(AuditAction.ADMIN_UPDATED, auth, httpReq,
                targetType = "user", targetId = userId, targetLabel = updated.email,
                message = "Statut admin ${updated.email} → ${req.status}",
                metadata = mapOf("newStatus" to req.status))
            ResponseEntity.ok(updated)
        } else ResponseEntity.notFound().build()
    }
}
