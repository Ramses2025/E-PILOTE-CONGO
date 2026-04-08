package cg.epilote.backend.admin

import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/provisioning")
class ProvisioningController(
    private val adminRepo: AdminRepository,
    private val sgClient: AppServicesClient
) {

    @PostMapping("/sync-user")
    @PreAuthorize("isAuthenticated()")
    fun provisionSyncUser(
        @RequestBody body: ProvisionSyncUserRequest,
        auth: Authentication
    ): ResponseEntity<ProvisionSyncUserResponse> {
        val requestingUserId = auth.principal as String
        if (requestingUserId != body.userId) {
            return ResponseEntity.status(403).build()
        }
        runCatching { sgClient.provisionUser(body.userId, body.schoolId, body.role) }
        return ResponseEntity.ok(ProvisionSyncUserResponse(provisioned = true))
    }

    @GetMapping("/seed/{ecoleId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_SYSTEME','ADMIN_GROUPE')")
    fun getSeedPackage(@PathVariable ecoleId: String): ResponseEntity<SeedPackageResponse> =
        runBlocking {
            val ecoles = adminRepo.listEcolesByGroupe(
                adminRepo.listGroupes().firstOrNull()?.id ?: return@runBlocking ResponseEntity.notFound().build()
            )
            val ecole = ecoles.find { it.id == ecoleId }
                ?: return@runBlocking ResponseEntity.notFound().build<SeedPackageResponse>()

            val users   = adminRepo.listUsersByEcole(ecoleId)
            val modules = adminRepo.listModules()
            val plans   = adminRepo.listPlans()

            ResponseEntity.ok(
                SeedPackageResponse(
                    ecole   = ecole,
                    users   = users.map { it.copy() },
                    modules = modules,
                    plans   = plans,
                    generatedAt = System.currentTimeMillis()
                )
            )
        }
}

data class ProvisionSyncUserRequest(val userId: String, val schoolId: String, val role: String)
data class ProvisionSyncUserResponse(val provisioned: Boolean, val message: String = "")

data class SeedPackageResponse(
    val ecole: EcoleResponse,
    val users: List<UserResponse>,
    val modules: List<ModuleResponse>,
    val plans: List<PlanResponse>,
    val generatedAt: Long
)
