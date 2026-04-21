package cg.epilote.backend.admin

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AdminRealtimeInterceptor(
    private val broker: AdminRealtimeBroker
) : HandlerInterceptor {
    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        if (ex != null) return

        val method = request.method.uppercase()
        val path = request.requestURI ?: return
        if (!AdminRealtimePathSupport.shouldPublish(path, method, response.status)) return
        if (method != "DELETE") return

        val actorId = SecurityContextHolder.getContext().authentication?.name
        val entityType = AdminRealtimePathSupport.resolveEntityType(path)
        val action = AdminRealtimePathSupport.resolveAction(method, path)
        val entityId = AdminRealtimePathSupport.resolveEntityId(path)
        val eventType = AdminRealtimePathSupport.eventType(entityType, action)

        broker.publish(
            AdminRealtimeEvent(
                channel = "super-admin",
                eventType = eventType,
                entityType = entityType,
                entityId = entityId,
                action = action,
                path = path,
                actorId = actorId
            )
        )
    }
}
