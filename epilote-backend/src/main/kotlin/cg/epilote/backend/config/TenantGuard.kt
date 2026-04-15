package cg.epilote.backend.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.regex.Pattern

@Component
@Order(10)
class TenantGuard : OncePerRequestFilter() {

    private val groupePathPattern = Pattern.compile("^/api/groupes/([^/]+)")
    private val schoolPathPattern = Pattern.compile("^/api/schools/([^/]+)")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated) {
            filterChain.doFilter(request, response)
            return
        }

        val authorities = auth.authorities.map { it.authority }.toSet()

        // SUPER_ADMIN bypasses tenant check
        if ("ROLE_SUPER_ADMIN" in authorities) {
            filterChain.doFilter(request, response)
            return
        }

        // ADMIN_GROUPE: verify groupeId from path matches JWT groupId
        if ("ROLE_ADMIN_GROUPE" in authorities) {
            @Suppress("UNCHECKED_CAST")
            val details = auth.details as? Map<String, String> ?: emptyMap()
            val userGroupId = details["groupId"]

            val path = request.requestURI

            val groupeMatcher = groupePathPattern.matcher(path)
            if (groupeMatcher.find()) {
                val pathGroupeId = groupeMatcher.group(1)
                if (userGroupId.isNullOrBlank() || pathGroupeId != userGroupId) {
                    response.status = HttpServletResponse.SC_FORBIDDEN
                    response.contentType = "application/json"
                    response.writer.write("""{"error":"Accès interdit — ce groupe ne vous appartient pas"}""")
                    return
                }
            }

            // For /api/schools/{schoolId}, we let the endpoint itself handle the check
            // since the controller already scopes queries by groupId
        }

        filterChain.doFilter(request, response)
    }
}
