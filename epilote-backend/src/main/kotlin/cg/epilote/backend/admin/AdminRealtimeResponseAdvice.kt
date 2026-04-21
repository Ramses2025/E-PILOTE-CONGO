package cg.epilote.backend.admin

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

@ControllerAdvice(assignableTypes = [AdminController::class, AdminCommunicationController::class])
class AdminRealtimeResponseAdvice(
    private val broker: AdminRealtimeBroker,
    private val objectMapper: ObjectMapper
) : ResponseBodyAdvice<Any> {
    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>
    ): Boolean = true

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any? {
        val servletRequest = (request as? ServletServerHttpRequest)?.servletRequest ?: return body
        val servletResponse = (response as? ServletServerHttpResponse)?.servletResponse ?: return body
        val method = servletRequest.method.uppercase()
        val path = servletRequest.requestURI ?: return body

        if (!AdminRealtimePathSupport.shouldPublish(path, method, servletResponse.status)) return body
        if (method == "DELETE") return body
        if (body == null) return body

        val payload = runCatching {
            objectMapper.convertValue(body, object : TypeReference<Map<String, Any?>>() {})
        }.getOrNull() ?: return body

        val actorId = SecurityContextHolder.getContext().authentication?.name
        val entityType = AdminRealtimePathSupport.resolveEntityType(path)
        val action = AdminRealtimePathSupport.resolveAction(method, path)
        val entityId = payload["id"]?.toString() ?: AdminRealtimePathSupport.resolveEntityId(path)

        broker.publish(
            AdminRealtimeEvent(
                channel = "super-admin",
                eventType = AdminRealtimePathSupport.eventType(entityType, action),
                entityType = entityType,
                entityId = entityId,
                action = action,
                path = path,
                actorId = actorId,
                payload = payload
            )
        )
        return body
    }
}
