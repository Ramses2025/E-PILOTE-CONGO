package cg.epilote.backend.auth

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank val username: String,
    @field:NotBlank val password: String,
    val ecoleId: String? = null
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
    val username: String,
    val firstName: String,
    val lastName: String,
    val ecoleId: String?,
    val groupeId: String?,
    val role: String,
    val permissions: List<PermissionDto>,
    val expiresIn: Long
)

data class RefreshRequest(
    @field:NotBlank val refreshToken: String
)

data class TokenResponse(
    val accessToken: String,
    val expiresIn: Long
)

data class EpiloteUserDetails(
    val userId: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val ecoleId: String?,
    val groupeId: String?,
    val role: UserRole,
    val permissions: List<PermissionDto>,
    val passwordHash: String
)

enum class UserRole {
    SUPER_ADMIN,
    ADMIN_SYSTEME,
    ADMIN_GROUPE,
    DIRECTOR,
    ADMIN_ECOLE,
    USER
}
