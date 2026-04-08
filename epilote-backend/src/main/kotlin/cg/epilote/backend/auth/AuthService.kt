package cg.epilote.backend.auth

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) {

    suspend fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByUsername(request.username)
            ?: throw AuthException("Identifiants incorrects")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw AuthException("Identifiants incorrects")
        }

        val offlineToken = jwtService.generateOfflineToken(user)
        return LoginResponse(
            accessToken          = jwtService.generateAccessToken(user),
            refreshToken         = jwtService.generateRefreshToken(user.userId),
            offlineToken         = offlineToken,
            offlineTokenExpiresAt = System.currentTimeMillis() + jwtService.offlineExpirationMs,
            userId               = user.userId,
            username             = user.username,
            firstName            = user.firstName,
            lastName             = user.lastName,
            ecoleId              = user.ecoleId,
            groupeId             = user.groupeId,
            role                 = user.role.name,
            permissions          = user.permissions,
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
