package cg.epilote.desktop

import cg.epilote.desktop.data.AdminDataRepository
import cg.epilote.desktop.data.CreateAdminUserDto
import cg.epilote.desktop.data.CreateCategorieDto
import cg.epilote.desktop.data.CreateGroupeDto
import cg.epilote.desktop.data.CreateModuleDto
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.data.UpdateCategorieDto
import cg.epilote.desktop.data.UpdateAdminUserDto
import cg.epilote.desktop.data.UpdateGroupeDto
import cg.epilote.desktop.data.UpdateModuleDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun CoroutineScope.createGroupeAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    dto: CreateGroupeDto,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.createGroupe(dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        adminRepo.upsertGroupe(result.getOrNull()!!)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Erreur lors de la création")
    }
}

internal fun CoroutineScope.updateGroupeAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    groupId: String,
    dto: UpdateGroupeDto,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.updateGroupe(groupId, dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        adminRepo.upsertGroupe(result.getOrNull()!!)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Erreur lors de la mise à jour")
    }
}

internal fun CoroutineScope.deleteGroupeAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    groupId: String,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val success = client.deleteGroupe(groupId)
    if (success) {
        adminRepo.removeGroupe(groupId)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, "Impossible de supprimer le groupe.")
    }
}

internal fun CoroutineScope.toggleGroupeStatusAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    groupId: String,
    isActive: Boolean,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = client.updateGroupe(groupId, UpdateGroupeDto(isActive = isActive))
    if (result != null) {
        adminRepo.upsertGroupe(result)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, if (isActive) "Impossible de réactiver le groupe." else "Impossible de suspendre le groupe.")
    }
}

internal fun CoroutineScope.createAdminGroupeAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    groupId: String,
    password: String,
    nom: String,
    prenom: String,
    email: String,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = client.createAdminGroupe(groupId, password, nom, prenom, email)
    if (result != null) {
        adminRepo.upsertAdminGroupe(groupId, result)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, "Impossible de créer l'administrateur du groupe.")
    }
}

internal fun CoroutineScope.createCategoryAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    dto: CreateCategorieDto,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.createCategorie(dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        adminRepo.upsertCategory(result.getOrNull()!!)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Impossible de créer la catégorie.")
    }
}

internal fun CoroutineScope.updateCategoryAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    code: String,
    dto: UpdateCategorieDto,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.updateCategorie(code, dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        adminRepo.upsertCategory(result.getOrNull()!!)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Impossible de mettre à jour la catégorie.")
    }
}

internal fun CoroutineScope.toggleCategoryStatusAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    code: String,
    isActive: Boolean,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.updateCategorie(code, UpdateCategorieDto(isActive = isActive)) }
    if (result.isSuccess && result.getOrNull() != null) {
        adminRepo.upsertCategory(result.getOrNull()!!)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, if (isActive) "Impossible de réactiver la catégorie." else "Impossible de suspendre la catégorie.")
    }
}

internal fun CoroutineScope.createModuleAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    dto: CreateModuleDto,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.createModule(dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        adminRepo.upsertModule(result.getOrNull()!!)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Impossible de créer le module.")
    }
}

internal fun CoroutineScope.updateModuleAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    moduleId: String,
    dto: UpdateModuleDto,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.updateModule(moduleId, dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        adminRepo.upsertModule(result.getOrNull()!!)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Impossible de mettre à jour le module.")
    }
}

internal fun CoroutineScope.toggleModuleStatusAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    moduleId: String,
    isActive: Boolean,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.updateModule(moduleId, UpdateModuleDto(isActive = isActive)) }
    if (result.isSuccess && result.getOrNull() != null) {
        adminRepo.upsertModule(result.getOrNull()!!)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, if (isActive) "Impossible de réactiver le module." else "Impossible de suspendre le module.")
    }
}

internal fun CoroutineScope.createAdminAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    dto: CreateAdminUserDto,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = client.createAdminUser(dto)
    if (result != null) {
        adminRepo.upsertAdmin(result)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, "Impossible de créer l'administrateur.")
    }
}

internal fun CoroutineScope.updateAdminAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    userId: String,
    dto: UpdateAdminUserDto,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = client.updateAdminUser(userId, dto)
    if (result != null) {
        adminRepo.upsertAdmin(result)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, "Impossible de mettre à jour l'administrateur.")
    }
}

internal fun CoroutineScope.deleteAdminAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    userId: String,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val success = client.deleteAdminUser(userId)
    if (success) {
        adminRepo.removeAdmin(userId)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, "Impossible de supprimer l'administrateur.")
    }
}

internal fun CoroutineScope.toggleAdminStatusAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    userId: String,
    status: String,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = client.toggleAdminStatus(userId, status)
    if (result != null) {
        adminRepo.upsertAdmin(result)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, "Impossible de modifier le statut de l'administrateur.")
    }
}
