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
                val rawRole = claims["role"] as? String ?: "USER"
                val role = when (rawRole) {
                    "ADMIN_SYSTEME" -> "SUPER_ADMIN"   // garde-fou : rôle inexistant, redirigé vers SUPER_ADMIN
                    "DIRECTOR"      -> "USER"           // garde-fou : rôle inexistant, le directeur est un USER avec profil
                    else            -> rawRole
                }

                @Suppress("UNCHECKED_CAST")
                val permissions = claims["permissions"] as? List<Map<String, Any>> ?: emptyList()

                val authorities = mutableListOf(SimpleGrantedAuthority("ROLE_$role"))
                permissions.forEach { p ->
                    val slug = p["moduleSlug"] as? String ?: return@forEach
                    authorities.add(SimpleGrantedAuthority("READ_$slug"))
                    if (p["canWrite"] == true)  authorities.add(SimpleGrantedAuthority("WRITE_$slug"))
                    if (p["canDelete"] == true) authorities.add(SimpleGrantedAuthority("DELETE_$slug"))
                    if (p["canExport"] == true) authorities.add(SimpleGrantedAuthority("EXPORT_$slug"))
                }

                val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
                auth.details = mapOf(
                    "schoolId" to (claims["schoolId"] as? String ?: ""),
                    "groupId"  to (claims["groupId"] as? String ?: ""),
                    "role"     to role,
                    "email"    to (claims["email"] as? String ?: "")
                )
                SecurityContextHolder.getContext().authentication = auth
            }
        }

        filterChain.doFilter(request, response)
    }
}
