package cg.epilote.backend.admin

internal object AdminRealtimePathSupport {
    fun shouldPublish(path: String, method: String, status: Int): Boolean {
        if (method !in setOf("POST", "PUT", "DELETE")) return false
        if (!path.startsWith("/api/super-admin/")) return false
        if (path.startsWith("/api/super-admin/events/")) return false
        return status in 200..299
    }

    fun resolveEntityType(path: String): String {
        val normalized = path.removePrefix("/api/super-admin/")
        return when {
            normalized.startsWith("communications/announcements") -> "announcement"
            normalized.startsWith("communications/messages") -> "message"
            normalized.startsWith("subscriptions") -> "subscription"
            normalized.startsWith("invoices") -> "invoice"
            normalized.startsWith("groupes") && normalized.contains("/admins") -> "admin"
            normalized.startsWith("groupes") -> "groupe"
            normalized.startsWith("plans") -> "plan"
            normalized.startsWith("modules") -> "module"
            normalized.startsWith("categories") -> "category"
            normalized.startsWith("admins") -> "admin"
            else -> normalized.substringBefore('/').ifBlank { "admin" }
        }
    }

    fun resolveEntityId(path: String): String? {
        val adminPrefix = "/api/super-admin/"
        val afterPrefix = path.removePrefix(adminPrefix)
        val segments = afterPrefix.split("/").filter { it.isNotBlank() }

        return when {
            segments.isEmpty() -> null
            segments[0] == "communications" && segments.size >= 3 -> segments.getOrNull(2)
            segments[0] == "groupes" && segments.getOrNull(2) == "admins" -> segments.getOrNull(3)
            segments.size >= 2 -> {
                val candidate = segments[1]
                if (candidate.isNotEmpty() && candidate !in setOf("admins", "status", "pdf")) candidate else null
            }
            else -> null
        }
    }

    fun resolveAction(method: String, path: String): String = when {
        method == "POST" -> "created"
        method == "DELETE" -> "deleted"
        path.endsWith("/status") -> "status_changed"
        else -> "updated"
    }

    fun eventType(entityType: String, action: String): String =
        "${entityType.uppercase()}_${action.uppercase()}"
}
