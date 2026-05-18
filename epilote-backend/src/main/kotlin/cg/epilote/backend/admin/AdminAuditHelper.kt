package cg.epilote.backend.admin

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class AdminAuditHelper(private val auditRepo: AdminAuditLogRepository) {

    fun ipOf(req: HttpServletRequest?): String? {
        if (req == null) return null
        val xff = req.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
        return xff?.takeIf { it.isNotBlank() } ?: req.remoteAddr
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun audit(
        action: AuditAction,
        auth: Authentication?,
        req: HttpServletRequest?,
        outcome: AuditOutcome = AuditOutcome.SUCCESS,
        targetType: String? = null,
        targetId: String? = null,
        targetLabel: String? = null,
        message: String? = null,
        metadata: Map<String, Any?> = emptyMap()
    ) {
        val details = auth?.details as? Map<String, String> ?: emptyMap()
        auditRepo.record(
            action = action,
            outcome = outcome,
            actorId = auth?.principal as? String,
            actorEmail = details["email"],
            actorRole = auth?.authorities?.firstOrNull()?.authority?.removePrefix("ROLE_"),
            targetType = targetType,
            targetId = targetId,
            targetLabel = targetLabel,
            ipAddress = ipOf(req),
            userAgent = req?.getHeader("User-Agent"),
            message = message,
            metadata = metadata
        )
    }
}
