package cg.epilote.desktop

import cg.epilote.desktop.data.AdminDataRepository
import cg.epilote.desktop.data.CreatePlanDto
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.data.UpdatePlanDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ── Plans CRUD helpers ──────────────────────────────────────────────

internal fun CoroutineScope.createPlanAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    dto: CreatePlanDto,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.createPlan(dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        adminRepo.upsertPlan(result.getOrNull()!!)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Impossible de créer le plan.")
    }
}

internal fun CoroutineScope.updatePlanAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    planId: String,
    dto: UpdatePlanDto,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.updatePlan(planId, dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        adminRepo.upsertPlan(result.getOrNull()!!)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Impossible de mettre à jour le plan.")
    }
}

internal fun CoroutineScope.togglePlanStatusAndRefresh(
    client: DesktopAdminClient,
    adminRepo: AdminDataRepository,
    planId: String,
    isActive: Boolean,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.updatePlan(planId, UpdatePlanDto(isActive = isActive)) }
    if (result.isSuccess && result.getOrNull() != null) {
        adminRepo.upsertPlan(result.getOrNull()!!)
        adminRepo.refreshDashboardStatsAsync()
        onResult(true, null)
    } else {
        onResult(false, if (isActive) "Impossible de réactiver le plan." else "Impossible de suspendre le plan.")
    }
}
