package cg.epilote.backend.auth

import cg.epilote.backend.admin.AdminRepository
import cg.epilote.backend.admin.SubscriptionExpiredException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val adminRepository: AdminRepository
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
                    val sub = adminRepository.getActiveSubscriptionByGroupe(groupId)
                    if (sub == null || sub.dateFin < System.currentTimeMillis()) {
                        throw SubscriptionExpiredException("Abonnement expir\u00e9 ou inexistant \u2014 contactez le support E-PILOTE")
                    }
                    val plan = adminRepository.getPlanById(sub.planId)
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
                    val sub = adminRepository.getActiveSubscriptionByGroupe(groupId)
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
            expiresIn            = 3600
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
