package cg.epilote.desktop

import cg.epilote.desktop.data.CreateAdminUserDto
import cg.epilote.desktop.data.CreateCategorieDto
import cg.epilote.desktop.data.CreateGroupeDto
import cg.epilote.desktop.data.CreateModuleDto
import cg.epilote.desktop.data.DashboardStatsDto
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.data.UpdateCategorieDto
import cg.epilote.desktop.data.UpdateAdminUserDto
import cg.epilote.desktop.data.UpdateGroupeDto
import cg.epilote.desktop.data.UpdateModuleDto
import cg.epilote.desktop.ui.screens.AdminUserDto
import cg.epilote.desktop.ui.screens.CategorieDto
import cg.epilote.desktop.ui.screens.GroupeDto
import cg.epilote.desktop.ui.screens.ModuleDto
import cg.epilote.desktop.ui.screens.PlanDto
import cg.epilote.desktop.ui.screens.UserDto
import cg.epilote.desktop.ui.screens.superadmin.AdminStats
import cg.epilote.desktop.ui.screens.superadmin.toAdminStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal data class AdminDesktopSnapshot(
    val dashboardStats: DashboardStatsDto = DashboardStatsDto(),
    val adminStats: AdminStats = AdminStats(),
    val groupes: List<GroupeDto> = emptyList(),
    val adminGroupAdmins: Map<String, List<UserDto>> = emptyMap(),
    val adminUsers: List<AdminUserDto> = emptyList(),
    val plans: List<PlanDto> = emptyList(),
    val modules: List<ModuleDto> = emptyList(),
    val categories: List<CategorieDto> = emptyList()
)

internal suspend fun DesktopAdminClient.loadAdminSnapshot(): AdminDesktopSnapshot {
    val stats = runCatching { getDashboardStats() }.getOrNull() ?: DashboardStatsDto()
    val groupesApi = runCatching { listGroupes() }.getOrNull().orEmpty()
    val groupes = groupesApi.map {
        GroupeDto(
            id = it.id,
            nom = it.nom,
            slug = it.slug,
            email = it.email,
            phone = it.phone,
            department = it.department,
            city = it.city,
            address = it.address,
            country = it.country,
            logo = it.logo,
            description = it.description,
            foundedYear = it.foundedYear,
            website = it.website,
            planId = it.planId,
            ecolesCount = it.ecolesCount,
            usersCount = it.usersCount,
            isActive = it.isActive,
            createdAt = it.createdAt
        )
    }
    val plans = runCatching { listPlans() }.getOrNull()?.map {
        PlanDto(id = it.id, nom = it.nom, type = it.type, prixXAF = it.prixXAF,
            currency = it.currency, maxStudents = it.maxStudents, maxPersonnel = it.maxPersonnel,
            modulesIncluded = it.modulesIncluded, isActive = it.isActive)
    }.orEmpty()
    val modules = runCatching { listModules() }.getOrNull()?.map {
        ModuleDto(it.id, it.code, it.nom, it.categorieCode, it.description, it.isCore, it.requiredPlan, it.isActive, it.ordre)
    }.orEmpty()
    val categories = runCatching { listCategories() }.getOrNull()?.map {
        CategorieDto(
            code = it.code,
            nom = it.nom,
            isCore = it.isCore,
            ordre = it.ordre,
            isActive = it.isActive
        )
    }.orEmpty()
    val allAdminsApi = runCatching { listAllAdmins() }.getOrNull().orEmpty()
    val adminsByGroup = groupesApi.associate { groupe ->
        groupe.id to allAdminsApi
            .asSequence()
            .filter { it.role.equals("ADMIN_GROUPE", ignoreCase = true) && it.groupId == groupe.id }
            .map {
                UserDto(
                    id = it.id,
                    username = it.username,
                    firstName = it.firstName,
                    lastName = it.lastName,
                    email = it.email,
                    schoolId = it.schoolId ?: "",
                    groupId = it.groupId ?: "",
                    profilId = "",
                    role = it.role,
                    isActive = it.isActive,
                    createdAt = it.createdAt
                )
            }
            .toList()
    }
    val adminUsers = allAdminsApi.map {
        AdminUserDto(
            id = it.id,
            username = it.username,
            firstName = it.firstName,
            lastName = it.lastName,
            email = it.email,
            phone = it.phone,
            role = it.role,
            status = it.status,
            gender = it.gender,
            dateOfBirth = it.dateOfBirth,
            groupId = it.groupId,
            schoolId = it.schoolId,
            avatar = it.avatar,
            address = it.address,
            birthPlace = it.birthPlace,
            mustChangePassword = it.mustChangePassword,
            lastLoginAt = it.lastLoginAt,
            loginAttempts = it.loginAttempts,
            isActive = it.isActive,
            createdAt = it.createdAt,
            updatedAt = it.updatedAt
        )
    }

    return AdminDesktopSnapshot(
        dashboardStats = stats,
        adminStats = stats.toAdminStats(),
        groupes = groupes,
        adminGroupAdmins = adminsByGroup,
        adminUsers = adminUsers,
        plans = plans,
        modules = modules,
        categories = categories
    )
}

internal fun CoroutineScope.createGroupeAndRefresh(
    client: DesktopAdminClient,
    dto: CreateGroupeDto,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.createGroupe(dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Erreur lors de la création")
    }
}

internal fun CoroutineScope.updateGroupeAndRefresh(
    client: DesktopAdminClient,
    groupId: String,
    dto: UpdateGroupeDto,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.updateGroupe(groupId, dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Erreur lors de la mise à jour")
    }
}

internal fun CoroutineScope.deleteGroupeAndRefresh(
    client: DesktopAdminClient,
    groupId: String,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val success = client.deleteGroupe(groupId)
    if (success) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, "Impossible de supprimer le groupe.")
    }
}

internal fun CoroutineScope.toggleGroupeStatusAndRefresh(
    client: DesktopAdminClient,
    groupId: String,
    isActive: Boolean,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = client.updateGroupe(groupId, UpdateGroupeDto(isActive = isActive))
    if (result != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, if (isActive) "Impossible de réactiver le groupe." else "Impossible de suspendre le groupe.")
    }
}

internal fun CoroutineScope.createAdminGroupeAndRefresh(
    client: DesktopAdminClient,
    groupId: String,
    password: String,
    nom: String,
    prenom: String,
    email: String,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = client.createAdminGroupe(groupId, password, nom, prenom, email)
    if (result != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, "Impossible de créer l'administrateur du groupe.")
    }
}

internal fun CoroutineScope.createCategoryAndRefresh(
    client: DesktopAdminClient,
    dto: CreateCategorieDto,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.createCategorie(dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Impossible de créer la catégorie.")
    }
}

internal fun CoroutineScope.updateCategoryAndRefresh(
    client: DesktopAdminClient,
    code: String,
    dto: UpdateCategorieDto,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.updateCategorie(code, dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Impossible de mettre à jour la catégorie.")
    }
}

internal fun CoroutineScope.toggleCategoryStatusAndRefresh(
    client: DesktopAdminClient,
    code: String,
    isActive: Boolean,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.updateCategorie(code, UpdateCategorieDto(isActive = isActive)) }
    if (result.isSuccess && result.getOrNull() != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, if (isActive) "Impossible de réactiver la catégorie." else "Impossible de suspendre la catégorie.")
    }
}

internal fun CoroutineScope.createModuleAndRefresh(
    client: DesktopAdminClient,
    dto: CreateModuleDto,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.createModule(dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Impossible de créer le module.")
    }
}

internal fun CoroutineScope.updateModuleAndRefresh(
    client: DesktopAdminClient,
    moduleId: String,
    dto: UpdateModuleDto,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.updateModule(moduleId, dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Impossible de mettre à jour le module.")
    }
}

internal fun CoroutineScope.toggleModuleStatusAndRefresh(
    client: DesktopAdminClient,
    moduleId: String,
    isActive: Boolean,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.updateModule(moduleId, UpdateModuleDto(isActive = isActive)) }
    if (result.isSuccess && result.getOrNull() != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, if (isActive) "Impossible de réactiver le module." else "Impossible de suspendre le module.")
    }
}

internal fun CoroutineScope.createAdminAndRefresh(
    client: DesktopAdminClient,
    dto: CreateAdminUserDto,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = client.createAdminUser(dto)
    if (result != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, "Impossible de créer l'administrateur.")
    }
}

internal fun CoroutineScope.updateAdminAndRefresh(
    client: DesktopAdminClient,
    userId: String,
    dto: UpdateAdminUserDto,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = client.updateAdminUser(userId, dto)
    if (result != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, "Impossible de mettre à jour l'administrateur.")
    }
}

internal fun CoroutineScope.deleteAdminAndRefresh(
    client: DesktopAdminClient,
    userId: String,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val success = client.deleteAdminUser(userId)
    if (success) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, "Impossible de supprimer l'administrateur.")
    }
}

internal fun CoroutineScope.toggleAdminStatusAndRefresh(
    client: DesktopAdminClient,
    userId: String,
    status: String,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = client.toggleAdminStatus(userId, status)
    if (result != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, "Impossible de modifier le statut de l'administrateur.")
    }
}
