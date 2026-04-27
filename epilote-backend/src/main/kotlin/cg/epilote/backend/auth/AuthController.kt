package cg.epilote.backend.auth

import cg.epilote.backend.admin.AdminAuditLogRepository
import cg.epilote.backend.admin.AuditAction
import cg.epilote.backend.admin.AuditOutcome
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
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

    /**
     * Change le mot de passe de l'utilisateur courant ou — pour un Super Admin
     * — d'un autre utilisateur (réinitialisation administrative).
     *
     * Sécurité :
     *  - Authentification JWT obligatoire (`@PreAuthorize("isAuthenticated()")`).
     *  - Vérification du `currentPassword` côté self-service dans [AuthService.changePassword].
     *  - Audit systématique (succès comme échec) avec IP + User-Agent.
     *
     * Référence Spring Security :
     *   https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    fun changePassword(
        @Valid @RequestBody request: ChangePasswordRequest,
        auth: Authentication,
        httpReq: HttpServletRequest
    ): ResponseEntity<ChangePasswordResponse> = runBlocking {
        val ip = (httpReq.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
            ?.takeIf { it.isNotBlank() }) ?: httpReq.remoteAddr
        val ua = httpReq.getHeader("User-Agent")
        val actorId = auth.principal as String
        @Suppress("UNCHECKED_CAST")
        val actorEmail = (auth.details as? Map<String, String>)?.get("email")?.takeIf { it.isNotBlank() }
        val rawRole = auth.authorities.firstOrNull()?.authority?.removePrefix("ROLE_") ?: "USER"
        val actorRole = runCatching { UserRole.valueOf(rawRole) }.getOrDefault(UserRole.USER)
        val targetId = request.targetUserId?.takeIf { it.isNotBlank() } ?: actorId
        val isAdminReset = targetId != actorId

        try {
            val result = authService.changePassword(actorId, actorRole, request)
            auditRepo.record(
                action = if (isAdminReset) AuditAction.PASSWORD_RESET else AuditAction.PASSWORD_CHANGED,
                outcome = AuditOutcome.SUCCESS,
                actorId = actorId,
                actorEmail = actorEmail,
                actorRole = actorRole.name,
                targetType = "user",
                targetId = result.userId,
                ipAddress = ip,
                userAgent = ua,
                message = if (isAdminReset)
                    "Mot de passe réinitialisé par Super Admin pour l'utilisateur ${result.userId}"
                else
                    "Mot de passe modifié avec succès",
                metadata = mapOf("passwordChangedAt" to result.passwordChangedAt)
            )
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            // Couvre AuthException (legacy), ValidationException (entrée invalide → 400),
            // ResponseStatusException (autorisation 403, cible 404, échec 500). Toutes
            // sont auditées en FAILURE puis re-propagées au GlobalExceptionHandler qui
            // applique le bon statut HTTP.
            auditRepo.record(
                action = if (isAdminReset) AuditAction.PASSWORD_RESET else AuditAction.PASSWORD_CHANGED,
                outcome = AuditOutcome.FAILURE,
                actorId = actorId,
                actorEmail = actorEmail,
                actorRole = actorRole.name,
                targetType = "user",
                targetId = targetId,
                ipAddress = ip,
                userAgent = ua,
                message = "Échec changement de mot de passe : ${e.message}",
                metadata = mapOf("reason" to (e.message ?: "unknown"))
            )
            throw e
        }
    }

    @ExceptionHandler(AuthException::class)
    fun handleAuthException(ex: AuthException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("error" to (ex.message ?: "Erreur d'authentification")))
}
