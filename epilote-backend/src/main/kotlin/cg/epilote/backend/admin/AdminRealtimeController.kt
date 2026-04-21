package cg.epilote.backend.admin

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/super-admin/events")
class AdminRealtimeController(
    private val broker: AdminRealtimeBroker
) {
    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(authentication: Authentication, request: HttpServletRequest): SseEmitter {
        val clientId = "${authentication.name}-${System.currentTimeMillis()}"
        val lastEventId = request.getHeader("Last-Event-ID")
        return broker.subscribe(clientId, lastEventId)
    }
}
