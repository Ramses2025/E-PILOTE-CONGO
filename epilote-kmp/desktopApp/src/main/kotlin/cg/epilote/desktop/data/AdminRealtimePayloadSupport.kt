package cg.epilote.desktop.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

private val realtimePayloadJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

internal fun AdminDataRepository.applyRealtimePayload(event: AdminRealtimeEventDto): Boolean {
    val payload = event.payload ?: return false

    val applied = when (event.entityType) {
        "groupe" -> applyGroupePayload(event, payload)
        "plan" -> applyPlanPayload(payload)
        "module" -> applyModulePayload(payload)
        "category" -> applyCategoryPayload(payload)
        "admin" -> applyAdminPayload(event, payload)
        else -> false
    }

    if (applied) {
        refreshDashboardStatsAsync()
    }
    return applied
}

private fun AdminDataRepository.applyGroupePayload(
    event: AdminRealtimeEventDto,
    payload: JsonObject
): Boolean {
    if (event.action == "deleted") {
        val entityId = event.entityId ?: return false
        removeGroupe(entityId)
        return true
    }

    val dto = runCatching {
        realtimePayloadJson.decodeFromJsonElement(GroupeApiDto.serializer(), payload)
    }.getOrNull() ?: return false

    upsertGroupe(dto)
    return true
}

private fun AdminDataRepository.applyPlanPayload(payload: JsonObject): Boolean {
    val dto = runCatching {
        realtimePayloadJson.decodeFromJsonElement(PlanApiDto.serializer(), payload)
    }.getOrNull() ?: return false

    upsertPlan(dto)
    return true
}

private fun AdminDataRepository.applyModulePayload(payload: JsonObject): Boolean {
    val dto = runCatching {
        realtimePayloadJson.decodeFromJsonElement(ModuleApiDto.serializer(), payload)
    }.getOrNull() ?: return false

    upsertModule(dto)
    return true
}

private fun AdminDataRepository.applyCategoryPayload(payload: JsonObject): Boolean {
    val dto = runCatching {
        realtimePayloadJson.decodeFromJsonElement(CategorieApiDto.serializer(), payload)
    }.getOrNull() ?: return false

    upsertCategory(dto)
    return true
}

private fun AdminDataRepository.applyAdminPayload(
    event: AdminRealtimeEventDto,
    payload: JsonObject
): Boolean {
    if (event.action == "deleted") {
        val entityId = event.entityId ?: return false
        removeAdmin(entityId)
        return true
    }

    if (event.path.contains("/groupes/") && event.path.contains("/admins")) {
        val dto = runCatching {
            realtimePayloadJson.decodeFromJsonElement(UserApiDto.serializer(), payload)
        }.getOrNull() ?: return false
        val groupId = payload["groupId"]
            ?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
            ?: event.path.substringAfter("/groupes/").substringBefore("/")
        if (groupId.isBlank()) return false
        upsertAdminGroupe(groupId, dto)
        return true
    }

    val dto = runCatching {
        realtimePayloadJson.decodeFromJsonElement(AdminUserApiDto.serializer(), payload)
    }.getOrNull() ?: return false

    upsertAdmin(dto)
    return true
}
