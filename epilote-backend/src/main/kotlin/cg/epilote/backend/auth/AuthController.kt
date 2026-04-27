package cg.epilote.backend.auth

import cg.epilote.backend.admin.AdminAuditLogRepository
import cg.epilote.backend.admin.AuditAction
import cg.epilote.backend.admin.AuditOutcome
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val auditRepo: AdminAuditLogRepository
) {

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpReq: HttpServletRequest
    ): ResponseEntity<LoginResponse> = runBlocking {
        val ip = (httpReq.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?.takeIf { it.isNotBlank() }) ?: httpReq.remoteAddr
        val ua = httpReq.getHeader("User-Agent")
        try {
            val response = authService.login(request)
            auditRepo.record(
                action = AuditAction.LOGIN_SUCCESS,
                outcome = AuditOutcome.SUCCESS,
                actorId = response.userId,
                actorEmail = response.email,
                actorRole = response.role,
                ipAddress = ip,
                userAgent = ua,
                message = "Connexion réussie : ${response.email}"
            )
            ResponseEntity.ok(response)
        } catch (e: AuthException) {
            auditRepo.record(
                action = AuditAction.LOGIN_FAILURE,
                outcome = AuditOutcome.FAILURE,
                actorEmail = request.email,
                ipAddress = ip,
                userAgent = ua,
                message = "Échec de connexion : ${e.message}",
                metadata = mapOf("reason" to (e.message ?: "unknown"))
            )
            throw e
        }
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): ResponseEntity<TokenResponse> =
        runBlocking {
            ResponseEntity.ok(authService.refresh(request))
        }

    @ExceptionHandler(AuthException::class)
    fun handleAuthException(ex: AuthException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("error" to (ex.message ?: "Erreur d'authentification")))
}
