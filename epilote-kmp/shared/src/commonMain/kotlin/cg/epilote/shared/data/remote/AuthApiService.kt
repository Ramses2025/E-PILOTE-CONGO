package cg.epilote.shared.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(val email: String, val password: String)

@Serializable
data class PermissionDto(
    val moduleSlug: String,
    val canRead: Boolean = true,
    val canWrite: Boolean = false,
    val canDelete: Boolean = false,
    val canExport: Boolean = false
)

@Serializable
data class LoginResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val offlineToken: String,
    val offlineTokenExpiresAt: Long = 0L,
    val userId: String,
    val email: String,
    val firstName: String = "",
    val lastName: String = "",
    val schoolId: String?,
    val groupId: String?,
    val role: String,
    val permissions: List<PermissionDto> = emptyList(),
    val expiresIn: Long
)

@Serializable
data class RefreshRequestDto(val refreshToken: String)

@Serializable
data class TokenResponseDto(val accessToken: String, val expiresIn: Long)

@Serializable
data class ProvisionSyncUserRequestDto(
    val userId: String,
    val groupId: String? = null,
    val schoolIds: List<String> = emptyList(),
    val role: String
)

@Serializable
data class ProvisionSyncUserResponseDto(
    val provisioned: Boolean,
    val message: String = "",
    val syncToken: String = ""
)

class AuthApiService(private val client: ApiClient) {

    suspend fun login(email: String, password: String): LoginResponseDto =
        client.post("/api/auth/login", LoginRequestDto(email, password))

    suspend fun refresh(refreshToken: String): TokenResponseDto =
        client.post("/api/auth/refresh", RefreshRequestDto(refreshToken))

    suspend fun provisionSyncUser(userId: String, groupId: String?, schoolIds: List<String>, role: String): ProvisionSyncUserResponseDto =
        client.post("/api/provisioning/sync-user", ProvisionSyncUserRequestDto(userId, groupId, schoolIds, role))
}
