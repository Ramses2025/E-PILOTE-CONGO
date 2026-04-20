package cg.epilote.desktop

import cg.epilote.desktop.data.CreatePlanDto
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.data.UpdatePlanDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ── Plans CRUD helpers ──────────────────────────────────────────────

internal fun CoroutineScope.createPlanAndRefresh(
    client: DesktopAdminClient,
    dto: CreatePlanDto,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.createPlan(dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Impossible de créer le plan.")
    }
}

internal fun CoroutineScope.updatePlanAndRefresh(
    client: DesktopAdminClient,
    planId: String,
    dto: UpdatePlanDto,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.updatePlan(planId, dto) }
    if (result.isSuccess && result.getOrNull() != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Impossible de mettre à jour le plan.")
    }
}

internal fun CoroutineScope.togglePlanStatusAndRefresh(
    client: DesktopAdminClient,
    planId: String,
    isActive: Boolean,
    refresh: () -> Unit,
    onResult: (Boolean, String?) -> Unit
) = launch {
    val result = runCatching { client.updatePlan(planId, UpdatePlanDto(isActive = isActive)) }
    if (result.isSuccess && result.getOrNull() != null) {
        refresh()
        onResult(true, null)
    } else {
        onResult(false, if (isActive) "Impossible de réactiver le plan." else "Impossible de suspendre le plan.")
    }
}
