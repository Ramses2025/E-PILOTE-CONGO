package cg.epilote.backend.admin

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
    private val passwordEncoder: PasswordEncoder,
    private val sgClient: AppServicesClient,
    private val communicationRepo: AdminCommunicationRepository
) {
    private fun Authentication.userId() = principal as String

    // ── Admin Groupe : Dashboard Stats ──────────────────────────

    @GetMapping("/api/groupes/{groupeId}/dashboard-stats")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun getGroupeDashboardStats(@PathVariable groupeId: String): ResponseEntity<GroupeDashboardStatsResponse> = runBlocking {
        coroutineScope {
            val groupe = repo.getGroupeById(groupeId)
                ?: return@coroutineScope ResponseEntity.notFound().build()
            val plan = runCatching { repo.getPlanById(groupe.planId) }.getOrNull()
            val subscription = runCatching { repo.getActiveSubscriptionByGroupe(groupeId) }.getOrNull()
            val dEcoles = async { runCatching { repo.listEcolesByGroupe(groupeId) }.getOrDefault(emptyList()) }
            val dModules = async { runCatching { repo.getModulesDisponibles(groupeId) }.getOrDefault(emptyList()) }
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
            val abonnementStatut = subscription?.statut ?: "pending"
            val response = GroupeDashboardStatsResponse(
                groupeId = groupeId,
                groupeNom = groupe.nom,
                province = groupe.department ?: "",
                planId = plan?.id ?: groupe.planId,
                planNom = plan?.nom ?: groupe.planId,
                planType = plan?.type ?: "gratuit",
                prixXAF = plan?.prixXAF ?: 0L,
                abonnementStatut = abonnementStatut,
                abonnementDateDebut = subscription?.dateDebut ?: 0L,
                abonnementDateFin = subscription?.dateFin ?: 0L,
                renouvellementAuto = subscription?.renouvellementAuto ?: false,
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
                derniereFacture = dLastInvoice.await()
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

    @GetMapping("/api/groupes/{groupeId}/categories-disponibles")
    @PreAuthorize("hasRole('ADMIN_GROUPE')")
    fun listCategoriesDisponibles(@PathVariable groupeId: String): ResponseEntity<List<CategorieWithModulesResponse>> = runBlocking {
        val modules = runCatching { repo.getModulesDisponibles(groupeId) }.getOrDefault(emptyList())
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
                code = code,
                nom = nom,
                ordre = ordre,
                modules = mods.sortedBy { it.ordre }
            )
        }.sortedWith(compareBy<CategorieWithModulesResponse> { it.ordre }.thenBy { it.nom.lowercase() })

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
}
