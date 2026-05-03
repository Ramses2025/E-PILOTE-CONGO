package cg.epilote.backend.config

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RateLimitFilterTest {

    private lateinit var filter: RateLimitFilter
    private lateinit var chain: FilterChain

    @BeforeEach
    fun setUp() {
        filter = RateLimitFilter()
        chain = FilterChain { _, _ -> }
    }

    @Test
    fun `should allow requests to non-auth paths`() {
        val request = MockHttpServletRequest("GET", "/api/super-admin/groupes")
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, chain)
        assertEquals(200, response.status)
    }

    @Test
    fun `should allow up to MAX_ATTEMPTS on login`() {
        repeat(10) {
            val request = MockHttpServletRequest("POST", "/api/auth/login")
            request.remoteAddr = "10.0.0.1"
            val response = MockHttpServletResponse()
            filter.doFilter(request, response, chain)
            assertEquals(200, response.status, "Request #${it + 1} should succeed")
        }
    }

    @Test
    fun `should block after MAX_ATTEMPTS on login`() {
        repeat(10) {
            val request = MockHttpServletRequest("POST", "/api/auth/login")
            request.remoteAddr = "10.0.0.2"
            val response = MockHttpServletResponse()
            filter.doFilter(request, response, chain)
        }

        val request = MockHttpServletRequest("POST", "/api/auth/login")
        request.remoteAddr = "10.0.0.2"
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, chain)
        assertEquals(429, response.status)
        assertTrue(response.contentAsString.contains("RATE_LIMITED"))
    }

    @Test
    fun `should track IPs independently`() {
        repeat(10) {
            val request = MockHttpServletRequest("POST", "/api/auth/login")
            request.remoteAddr = "10.0.0.3"
            val response = MockHttpServletResponse()
            filter.doFilter(request, response, chain)
        }

        val request = MockHttpServletRequest("POST", "/api/auth/login")
        request.remoteAddr = "10.0.0.4"
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, chain)
        assertEquals(200, response.status)
    }

    @Test
    fun `should also rate limit refresh endpoint`() {
        repeat(11) {
            val request = MockHttpServletRequest("POST", "/api/auth/refresh")
            request.remoteAddr = "10.0.0.5"
            val response = MockHttpServletResponse()
            filter.doFilter(request, response, chain)
        }

        val request = MockHttpServletRequest("POST", "/api/auth/refresh")
        request.remoteAddr = "10.0.0.5"
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, chain)
        assertEquals(429, response.status)
    }

    @Test
    fun `should use X-Forwarded-For when present for consistency with AuthController`() {
        repeat(10) {
            val request = MockHttpServletRequest("POST", "/api/auth/login")
            request.remoteAddr = "10.0.0.6"
            request.addHeader("X-Forwarded-For", "192.168.1.100, 10.0.0.1")
            val response = MockHttpServletResponse()
            filter.doFilter(request, response, chain)
        }

        val request = MockHttpServletRequest("POST", "/api/auth/login")
        request.remoteAddr = "10.0.0.6"
        request.addHeader("X-Forwarded-For", "192.168.1.100, 10.0.0.1")
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, chain)
        assertEquals(429, response.status)

        val request2 = MockHttpServletRequest("POST", "/api/auth/login")
        request2.remoteAddr = "10.0.0.6"
        val response2 = MockHttpServletResponse()
        filter.doFilter(request2, response2, chain)
        assertEquals(200, response2.status)
    }
}
