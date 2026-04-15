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
