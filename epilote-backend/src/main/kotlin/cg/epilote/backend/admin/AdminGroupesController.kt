package cg.epilote.backend.admin

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
class AdminGroupesController(
    private val repo: AdminRepository,
    private val planRepo: AdminPlanRepository,
    private val subscriptionRepo: AdminSubscriptionRepository,
    private val passwordEncoder: PasswordEncoder,
    private val sgClient: AppServicesClient,
    private val auditHelper: AdminAuditHelper
) {
    private fun Authentication.userId() = principal as String

    // ── Super Admin : Groupes ────────────────────────────────────

    @PostMapping("/api/super-admin/groupes")
    fun createGroupe(
        @Valid @RequestBody req: CreateGroupeRequest,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<GroupeResponse> = runBlocking {
        val created = repo.createGroupe(req, auth.userId())
        val plan = planRepo.getPlanById(req.planId)
        val sub = runCatching {
            subscriptionRepo.createSubscription(
                CreateSubscriptionRequest(groupeId = created.id, planId = req.planId)
            )
        }.getOrNull()
        val invoice = if (sub != null && plan != null) {
            val echeance = System.currentTimeMillis() + 30L * 86_400_000L
            runCatching {
                repo.createInvoice(
                    CreateInvoiceRequest(
                        groupeId = created.id,
                        subscriptionId = sub.id,
                        montantXAF = plan.prixXAF,
                        dateEcheance = echeance,
                        notes = "Facture initiale générée automatiquement à la création du groupe",
                        initialStatus = "draft"
                    )
                )
            }.getOrNull()
        } else null
        val metaExtra = buildMap<String, Any?> {
            put("planId", created.planId)
            put("city", created.city)
            put("department", created.department)
            sub?.let { put("subscriptionId", it.id) }
            invoice?.let { put("invoiceId", it.id) }
        }
        auditHelper.audit(AuditAction.GROUPE_CREATED, auth, httpReq,
            targetType = "groupe", targetId = created.id, targetLabel = created.nom,
            message = "Groupe '${created.nom}' créé — abonnement ${if (sub != null) "provisonné (pending)" else "non créé"}, facture ${if (invoice != null) "émise" else "non créée"}",
            metadata = metaExtra)
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
        auditHelper.audit(AuditAction.ADMIN_CREATED, auth, httpReq,
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
            auditHelper.audit(AuditAction.GROUPE_UPDATED, auth, httpReq,
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
            auditHelper.audit(AuditAction.GROUPE_DELETED, auth, httpReq,
                targetType = "groupe", targetId = groupeId, targetLabel = existing?.nom,
                message = "Groupe ${existing?.nom ?: groupeId} supprimé")
            ResponseEntity.noContent().build<Any>()
        } else ResponseEntity.notFound().build()
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
        auditHelper.audit(AuditAction.ADMIN_CREATED, auth, httpReq,
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
            auditHelper.audit(AuditAction.ADMIN_UPDATED, auth, httpReq,
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
            auditHelper.audit(AuditAction.ADMIN_DELETED, auth, httpReq,
                targetType = "user", targetId = userId,
                message = "Admin $userId supprimé")
            ResponseEntity.noContent().build<Any>()
        } else ResponseEntity.notFound().build()
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
            auditHelper.audit(AuditAction.ADMIN_UPDATED, auth, httpReq,
                targetType = "user", targetId = userId, targetLabel = updated.email,
                message = "Statut admin ${updated.email} → ${req.status}",
                metadata = mapOf("newStatus" to req.status))
            ResponseEntity.ok(updated)
        } else ResponseEntity.notFound().build()
    }
}
