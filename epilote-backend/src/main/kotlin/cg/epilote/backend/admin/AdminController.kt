package cg.epilote.backend.admin

import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.*
import org.springframework.security.crypto.password.PasswordEncoder

@RestController
class AdminController(
    private val repo: AdminRepository,
    private val passwordEncoder: PasswordEncoder,
    private val sgClient: AppServicesClient
) {

    private fun Authentication.userId() = principal as String

    @Suppress("UNCHECKED_CAST")
    private fun Authentication.details() = details as? Map<String, String> ?: emptyMap()

    // ── Dashboard Stats ──────────────────────────────────────────

    @GetMapping("/api/super-admin/dashboard-stats")
    fun getDashboardStats(): ResponseEntity<DashboardStatsResponse> = runBlocking {
        val groupes  = repo.countGroupes()
        val ecoles   = repo.countEcoles()
        val users    = repo.countUsers()
        val modules  = repo.countModules()
        val plans    = repo.listPlans()
        val categories = CategorieConstants.ALL
        ResponseEntity.ok(
            DashboardStatsResponse(
                totalGroupes    = groupes,
                totalEcoles     = ecoles,
                totalUtilisateurs = users,
                totalModules    = modules,
                totalPlans      = plans.size.toLong(),
                totalCategories = categories.size.toLong(),
                plans           = plans,
                categories      = categories
            )
        )
    }

    // ── Catégories (constantes) ─────────────────────────────────

    @GetMapping("/api/super-admin/categories")
    @PreAuthorize("isAuthenticated()")
    fun listCategories(): ResponseEntity<List<CategorieInfo>> =
        ResponseEntity.ok(CategorieConstants.ALL)

    // ── Super Admin : Plans ──────────────────────────────────────

    @PostMapping("/api/super-admin/plans")
    fun createPlan(@Valid @RequestBody req: CreatePlanRequest): ResponseEntity<PlanResponse> =
        runBlocking { ResponseEntity.status(HttpStatus.CREATED).body(repo.createPlan(req)) }

    @GetMapping("/api/super-admin/plans")
    fun listPlans(): ResponseEntity<List<PlanResponse>> =
        runBlocking { ResponseEntity.ok(repo.listPlans()) }

    // ── Super Admin : Modules ────────────────────────────────────

    @PostMapping("/api/super-admin/modules")
    fun createModule(@Valid @RequestBody req: CreateModuleRequest): ResponseEntity<ModuleResponse> =
        runBlocking { ResponseEntity.status(HttpStatus.CREATED).body(repo.createModule(req)) }

    @GetMapping("/api/super-admin/modules")
    @PreAuthorize("isAuthenticated()")
    fun listModules(): ResponseEntity<List<ModuleResponse>> =
        runBlocking { ResponseEntity.ok(repo.listModules()) }

    // ── Super Admin / Admin Systeme : Groupes ────────────────────

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
        val groupe = repo.getGroupeById(groupeId)
            ?: return@runBlocking ResponseEntity.notFound().build()
        val hash = passwordEncoder.encode(req.password)
        val admin = repo.createAdminGroupe(groupeId, req, hash)
        val ecoleIds = repo.listEcolesByGroupe(groupeId).map { it.id }
        runCatching { sgClient.provisionUser(admin.id, groupeId, ecoleIds, "ADMIN_GROUPE") }
        ResponseEntity.status(HttpStatus.CREATED).body(admin)
    }

    @GetMapping("/api/super-admin/groupes/{groupeId}/admins")
    fun listAdminGroupes(@PathVariable groupeId: String): ResponseEntity<List<UserResponse>> =
        runBlocking { ResponseEntity.ok(repo.listAdminGroupesByGroupe(groupeId)) }

    // ── Admin Groupe : Écoles ────────────────────────────────────

    @PostMapping("/api/groupes/{groupeId}/ecoles")
    @PreAuthorize("hasAnyRole('ADMIN_SYSTEME','ADMIN_GROUPE')")
    fun createEcole(
        @PathVariable groupeId: String,
        @Valid @RequestBody req: CreateEcoleRequest,
        auth: Authentication
    ): ResponseEntity<EcoleResponse> = runBlocking {
        val groupe = repo.listGroupes().find { it.id == groupeId }
            ?: return@runBlocking ResponseEntity.notFound().build()
        val ecole = repo.createEcole(groupeId, req, groupe.planId)
        val ecoleIds = repo.listEcolesByGroupe(groupeId).map { it.id }
        repo.listAdminGroupesByGroupe(groupeId).forEach { admin ->
            runCatching { sgClient.provisionUser(admin.id, groupeId, ecoleIds, "ADMIN_GROUPE") }
        }
        ResponseEntity.status(HttpStatus.CREATED).body(ecole)
    }

    @GetMapping("/api/groupes/{groupeId}/ecoles")
    @PreAuthorize("hasAnyRole('ADMIN_SYSTEME','ADMIN_GROUPE')")
    fun listEcoles(@PathVariable groupeId: String): ResponseEntity<List<EcoleResponse>> =
        runBlocking { ResponseEntity.ok(repo.listEcolesByGroupe(groupeId)) }

    // ── Admin Groupe : Modules disponibles (filtrés par plan) ─────

    @GetMapping("/api/groupes/{groupeId}/modules-disponibles")
    @PreAuthorize("hasAnyRole('ADMIN_SYSTEME','ADMIN_GROUPE')")
    fun listModulesDisponibles(@PathVariable groupeId: String): ResponseEntity<List<ModuleResponse>> =
        runBlocking { ResponseEntity.ok(repo.getModulesDisponibles(groupeId)) }

    // ── Admin Groupe : Profils ───────────────────────────────────

    @PostMapping("/api/groupes/{groupeId}/profils")
    @PreAuthorize("hasAnyRole('ADMIN_SYSTEME','ADMIN_GROUPE')")
    fun createProfil(
        @PathVariable groupeId: String,
        @Valid @RequestBody req: CreateProfilRequest,
        auth: Authentication
    ): ResponseEntity<ProfilResponse> = runBlocking {
        ResponseEntity.status(HttpStatus.CREATED).body(repo.createProfil(groupeId, req, auth.userId()))
    }

    @GetMapping("/api/groupes/{groupeId}/profils")
    @PreAuthorize("hasAnyRole('ADMIN_SYSTEME','ADMIN_GROUPE')")
    fun listProfils(@PathVariable groupeId: String): ResponseEntity<List<ProfilResponse>> =
        runBlocking { ResponseEntity.ok(repo.listProfilsByGroupe(groupeId)) }

    // ── Admin Groupe : Utilisateurs ──────────────────────────────

    @PostMapping("/api/groupes/{groupeId}/users")
    @PreAuthorize("hasAnyRole('ADMIN_SYSTEME','ADMIN_GROUPE')")
    fun createUser(
        @PathVariable groupeId: String,
        @Valid @RequestBody req: CreateUserRequest,
        auth: Authentication
    ): ResponseEntity<UserResponse> = runBlocking {
        val profil = repo.getProfilById(req.profilId)
            ?: return@runBlocking ResponseEntity.badRequest().build<UserResponse>()
        val hash = passwordEncoder.encode(req.password)
        val user = repo.createUser(groupeId, req, hash, profil.permissions)
        runCatching { sgClient.provisionUser(user.id, groupeId, listOf(req.ecoleId), "USER") }
        ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    @GetMapping("/api/ecoles/{ecoleId}/users")
    @PreAuthorize("hasAnyRole('ADMIN_SYSTEME','ADMIN_GROUPE')")
    fun listUsers(@PathVariable ecoleId: String): ResponseEntity<List<UserResponse>> =
        runBlocking { ResponseEntity.ok(repo.listUsersByEcole(ecoleId)) }

}
