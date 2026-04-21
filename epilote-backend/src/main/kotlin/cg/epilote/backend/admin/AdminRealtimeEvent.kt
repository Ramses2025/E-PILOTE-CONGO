package cg.epilote.backend.admin

import java.util.UUID

data class AdminRealtimeEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: String,
    val channel: String,
    val entityType: String,
    val entityId: String? = null,
    val action: String,
    val path: String,
    val emittedAt: Long = System.currentTimeMillis(),
    val actorId: String? = null,
    val payload: Map<String, Any?>? = null
)
