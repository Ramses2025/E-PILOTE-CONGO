package cg.epilote.backend.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Rate limiter basé sur l'IP pour les endpoints d'authentification.
 * Maximum [MAX_ATTEMPTS] tentatives par fenêtre de [WINDOW_MS] millisecondes.
 *
 * Protège contre le brute-force sur /api/auth/login et /api/auth/refresh.
 */
@Component
@Order(1)
class RateLimitFilter : OncePerRequestFilter() {

    private companion object {
        const val MAX_ATTEMPTS = 10
        const val WINDOW_MS = 60_000L
    }

    private data class RateWindow(val count: AtomicInteger = AtomicInteger(0), val windowStart: Long = System.currentTimeMillis())

    private val ipWindows = ConcurrentHashMap<String, RateWindow>()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI ?: return true
        return !path.startsWith("/api/auth/login") && !path.startsWith("/api/auth/refresh")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val ip = extractIp(request)
        val now = System.currentTimeMillis()

        val window = ipWindows.compute(ip) { _, existing ->
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                RateWindow(AtomicInteger(1), now)
            } else {
                existing.count.incrementAndGet()
                existing
            }
        }!!

        if (window.count.get() > MAX_ATTEMPTS) {
            response.status = 429
            response.contentType = "application/json"
            response.writer.write("""{"code":"RATE_LIMITED","message":"Trop de tentatives — réessayez dans une minute"}""")
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun extractIp(request: HttpServletRequest): String {
        return request.remoteAddr ?: "unknown"
    }
}
