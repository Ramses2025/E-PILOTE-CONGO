package cg.epilote.backend.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            if (jwtService.validateToken(token) && !jwtService.isTokenType(token, "refresh")) {
                val claims = jwtService.getClaimsFromToken(token)
                val userId = claims.subject
                val role = claims["role"] as? String ?: "USER"

                @Suppress("UNCHECKED_CAST")
                val modules = claims["modulesAccess"] as? List<String> ?: emptyList()

                val authorities = mutableListOf(SimpleGrantedAuthority("ROLE_$role"))
                modules.forEach { authorities.add(SimpleGrantedAuthority("MODULE_$it")) }

                val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
                auth.details = mapOf(
                    "ecoleId"  to (claims["ecoleId"] as? String ?: ""),
                    "groupeId" to (claims["groupeId"] as? String ?: ""),
                    "role"     to role
                )
                SecurityContextHolder.getContext().authentication = auth
            }
        }

        filterChain.doFilter(request, response)
    }
}
