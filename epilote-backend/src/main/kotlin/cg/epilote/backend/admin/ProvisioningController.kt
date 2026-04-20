package cg.epilote.backend.admin

import com.fasterxml.jackson.annotation.JsonAlias
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/provisioning")
class ProvisioningController(
    private val adminRepo: AdminRepository,
    private val planRepo: AdminPlanRepository,
    private val sgClient: AppServicesClient
) {

    @PostMapping("/sync-user")
    @PreAuthorize("isAuthenticated()")
    fun provisionSyncUser(
        @RequestBody body: ProvisionSyncUserRequest,
        auth: Authentication
    ): ResponseEntity<ProvisionSyncUserResponse> = runBlocking {
        val requestingUserId = auth.principal as String
        if (requestingUserId != body.userId) {
            return@runBlocking ResponseEntity.status(403).build<ProvisionSyncUserResponse>()
        }

        // For ADMIN_GROUPE, resolve all school IDs from the group's schools
        // to avoid channel erasure on re-login (client doesn't know all schoolIds)
        val effectiveSchoolIds = if (body.role == "ADMIN_GROUPE" && !body.groupId.isNullOrBlank()) {
            adminRepo.listEcolesByGroupe(body.groupId).map { it.id }
        } else {
            body.schoolIds
        }

        val syncToken = runCatching {
            sgClient.provisionUser(body.userId, body.groupId, effectiveSchoolIds, body.role)
        }.getOrNull()
        if (syncToken != null) {
            ResponseEntity.ok(ProvisionSyncUserResponse(provisioned = true, syncToken = syncToken))
        } else {
            ResponseEntity.ok(ProvisionSyncUserResponse(provisioned = false, message = "Provisioning failed"))
        }
    }

    @GetMapping("/seed/{schoolId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN_GROUPE')")
    fun getSeedPackage(@PathVariable schoolId: String): ResponseEntity<SeedPackageResponse> =
        runBlocking {
            val ecoles = adminRepo.listEcolesByGroupe(
                adminRepo.listGroupes().firstOrNull()?.id ?: return@runBlocking ResponseEntity.notFound().build()
            )
            val ecole = ecoles.find { it.id == schoolId }
                ?: return@runBlocking ResponseEntity.notFound().build<SeedPackageResponse>()

            val users   = adminRepo.listUsersByEcole(schoolId)
            val modules = adminRepo.listModules()
            val plans   = planRepo.listPlans()

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

data class ProvisionSyncUserRequest(
    val userId: String,
    @JsonAlias("groupeId")
    val groupId: String? = null,
    val schoolIds: List<String> = emptyList(),
    val role: String
)
data class ProvisionSyncUserResponse(val provisioned: Boolean, val message: String = "", val syncToken: String = "")

data class SeedPackageResponse(
    val ecole: EcoleResponse,
    val users: List<UserResponse>,
    val modules: List<ModuleResponse>,
    val plans: List<PlanResponse>,
    val generatedAt: Long
)
