package cg.epilote.backend.auth

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank val email: String,
    @field:NotBlank val password: String
)

data class PermissionDto(
    val moduleSlug: String,
    val canRead: Boolean,
    val canWrite: Boolean,
    val canDelete: Boolean,
    val canExport: Boolean
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val offlineToken: String,
    val offlineTokenExpiresAt: Long,
    val userId: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val schoolId: String?,
    val groupId: String?,
    val role: String,
    val permissions: List<PermissionDto>,
    val expiresIn: Long
)

data class RefreshRequest(
    @field:NotBlank val refreshToken: String
)

/**
 * Demande de changement de mot de passe.
 *
 * - `currentPassword` : obligatoire pour les utilisateurs changeant leur propre
 *   mot de passe. Vérifié contre `passwordHash` en BCrypt.
 * - `newPassword` : nouveau mot de passe en clair (sera encodé BCrypt).
 * - `targetUserId` : optionnel. Réservé au SUPER_ADMIN pour réinitialiser le
 *   mot de passe d'un autre compte (sans `currentPassword`). Si omis, l'opération
 *   s'applique au porteur du JWT.
 */
data class ChangePasswordRequest(
    val currentPassword: String? = null,
    @field:NotBlank val newPassword: String,
    val targetUserId: String? = null
)

data class ChangePasswordResponse(
    val userId: String,
    val mustChangePassword: Boolean,
    val passwordChangedAt: Long
)

data class TokenResponse(
    val accessToken: String,
    val expiresIn: Long
)

data class EpiloteUserDetails(
    val userId: String,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val schoolId: String?,
    val groupId: String?,
    val role: UserRole,
    val permissions: List<PermissionDto>,
    val passwordHash: String,
    val isActive: Boolean = true
)

enum class UserRole {
    SUPER_ADMIN,
    ADMIN_GROUPE,
    USER
}
