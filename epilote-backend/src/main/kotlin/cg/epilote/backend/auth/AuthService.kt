package cg.epilote.backend.auth

import cg.epilote.backend.admin.AdminPlanRepository
import cg.epilote.backend.admin.AdminSubscriptionRepository
import cg.epilote.backend.admin.SubscriptionExpiredException
import cg.epilote.backend.config.ValidationException
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val subscriptionRepository: AdminSubscriptionRepository,
    private val planRepository: AdminPlanRepository
) {

    suspend fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw AuthException("Identifiants incorrects")

        if (!user.isActive) {
            throw AuthException("Compte désactivé — contactez votre administrateur")
        }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw AuthException("Identifiants incorrects")
        }

        // Derive effective permissions based on role
        val effectiveUser = when (user.role) {
            UserRole.ADMIN_GROUPE -> {
                // ADMIN_GROUPE gets all modules from the group's subscription plan
                val groupId = user.groupId
                if (!groupId.isNullOrBlank()) {
                    val sub = subscriptionRepository.getActiveSubscriptionByGroupe(groupId)
                    if (sub == null || sub.dateFin < System.currentTimeMillis()) {
                        throw SubscriptionExpiredException("Abonnement expiré ou inexistant — contactez le support E-PILOTE")
                    }
                    val plan = planRepository.getPlanById(sub.planId)
                    if (plan != null) {
                        val planPermissions = plan.modulesIncluded.map { slug ->
                            PermissionDto(slug, canRead = true, canWrite = true, canDelete = true, canExport = true)
                        }
                        user.copy(permissions = planPermissions)
                    } else user
                } else user
            }
            UserRole.USER -> {
                // USER keeps profile-based permissions (already loaded from doc)
                // But verify the group subscription is active
                val groupId = user.groupId
                if (!groupId.isNullOrBlank()) {
                    val sub = subscriptionRepository.getActiveSubscriptionByGroupe(groupId)
                    if (sub == null || sub.dateFin < System.currentTimeMillis()) {
                        throw SubscriptionExpiredException("Abonnement du groupe expir\u00e9 \u2014 contactez votre administrateur")
                    }
                }
                user
            }
            UserRole.SUPER_ADMIN -> user
        }

        val offlineToken = jwtService.generateOfflineToken(effectiveUser)
        return LoginResponse(
            accessToken          = jwtService.generateAccessToken(effectiveUser),
            refreshToken         = jwtService.generateRefreshToken(effectiveUser.userId),
            offlineToken         = offlineToken,
            offlineTokenExpiresAt = System.currentTimeMillis() + jwtService.offlineExpirationMs,
            userId               = effectiveUser.userId,
            email                = effectiveUser.email,
            firstName            = effectiveUser.firstName,
            lastName             = effectiveUser.lastName,
            schoolId              = effectiveUser.schoolId,
            groupId              = effectiveUser.groupId,
            role                 = effectiveUser.role.name,
            permissions          = effectiveUser.permissions,
            expiresIn            = 3600,
            // Propage le flag pour que le client desktop force le changement
            // de mot de passe avant tout accès aux écrans applicatifs (politique
            // de mot de passe initial à usage unique). Le hash courant reste
            // valable pour la requête de changement (route /auth/change-password
            // accepte `currentPassword`).
            mustChangePassword   = effectiveUser.mustChangePassword
        )
    }

    /**
     * Modifie le mot de passe d'un utilisateur.
     *
     * Règles :
     * - `targetUserId == null` ou égal à `actorUserId` : self-service (l'utilisateur
     *   change son propre mot de passe). `currentPassword` est OBLIGATOIRE et doit
     *   correspondre au hash BCrypt enregistré.
     * - `targetUserId != actorUserId` : réinitialisation administrative.
     *   Réservée au rôle `SUPER_ADMIN`. `currentPassword` n'est pas requis.
     *
     * Toute autre combinaison lève [AuthException]. Le nouveau mot de passe est
     * encodé en BCrypt via [PasswordEncoder] avant persistence (pattern Spring
     * Security officiel : https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).
     *
     * Retourne le timestamp `passwordChangedAt` pour permettre au client de
     * forcer un re-login si nécessaire.
     */
    suspend fun changePassword(
        actorUserId: String,
        actorRole: UserRole,
        request: ChangePasswordRequest
    ): ChangePasswordResponse {
        val targetId = request.targetUserId?.takeIf { it.isNotBlank() } ?: actorUserId
        val isSelfService = targetId == actorUserId
        val isAdminReset = !isSelfService

        // Erreurs d'autorisation → 403 Forbidden (l'utilisateur EST authentifié, mais
        // n'a pas les droits pour cette action). Pas 401 : sinon le client desktop
        // (DesktopAdminClient.execute) interprèterait ces erreurs comme un token
        // expiré et déclencherait un refresh + retry inutile.
        if (isAdminReset && actorRole != UserRole.SUPER_ADMIN) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Action réservée au Super Admin")
        }

        // Cible introuvable → 404. Pas 401 (même raison qu'au-dessus).
        val target = userRepository.findById(targetId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable")

        // Erreurs de validation d'entrée → 400 Bad Request (via ValidationException
        // déjà mappée par GlobalExceptionHandler). Le client desktop différencie
        // ainsi un mauvais mot de passe (à ré-essayer) d'une session expirée.
        if (isSelfService) {
            val provided = request.currentPassword
            if (provided.isNullOrBlank()) {
                throw ValidationException("Mot de passe actuel requis")
            }
            if (!passwordEncoder.matches(provided, target.passwordHash)) {
                throw ValidationException("Mot de passe actuel incorrect")
            }
        }

        // Politique minimale (alignée avec les exigences UI) : 8 caractères au moins.
        if (request.newPassword.length < 8) {
            throw ValidationException("Le nouveau mot de passe doit contenir au moins 8 caractères")
        }
        if (passwordEncoder.matches(request.newPassword, target.passwordHash)) {
            throw ValidationException("Le nouveau mot de passe doit être différent de l'actuel")
        }

        val newHash = passwordEncoder.encode(request.newPassword)
        val updated = userRepository.updatePasswordHash(target.userId, newHash)
        if (!updated) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible de mettre à jour le mot de passe")
        }
        val now = System.currentTimeMillis()
        return ChangePasswordResponse(
            userId = target.userId,
            mustChangePassword = false,
            passwordChangedAt = now
        )
    }

    suspend fun refresh(request: RefreshRequest): TokenResponse {
        val token = request.refreshToken
        if (!jwtService.validateToken(token) || !jwtService.isTokenType(token, "refresh")) {
            throw AuthException("Token de rafraîchissement invalide")
        }
        val userId = jwtService.getUserIdFromToken(token)
        val user = userRepository.findById(userId)
            ?: throw AuthException("Utilisateur introuvable")

        return TokenResponse(
            accessToken = jwtService.generateAccessToken(user),
            expiresIn   = 3600
        )
    }
}

class AuthException(message: String) : RuntimeException(message)
