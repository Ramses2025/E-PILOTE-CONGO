package cg.epilote.shared.domain.usecase.auth

import cg.epilote.shared.data.local.EpiloteDatabase
import cg.epilote.shared.data.local.UserSessionRepository
import cg.epilote.shared.data.remote.AuthApiService
import cg.epilote.shared.data.sync.SyncManager
import cg.epilote.shared.domain.model.UserSession

sealed class LoginResult {
    data class Success(val session: UserSession) : LoginResult()
    data class Error(val message: String) : LoginResult()
    object NoNetwork : LoginResult()
}

class LoginUseCase(
    private val authApi: AuthApiService,
    private val sessionRepo: UserSessionRepository,
    private val syncManager: SyncManager,
    private val context: Any?
) {
    suspend fun execute(email: String, password: String): LoginResult {
        return try {
            val dto = authApi.login(email, password)

            val session = UserSession(
                userId               = dto.userId,
                email                = dto.email,
                firstName            = dto.firstName,
                lastName             = dto.lastName,
                schoolId = dto.schoolId,
                groupId = dto.groupId,
                role                 = dto.role,
                accessToken          = dto.accessToken,
                refreshToken         = dto.refreshToken,
                offlineToken         = dto.offlineToken,
                offlineTokenExpiresAt = dto.offlineTokenExpiresAt,
                permissions          = dto.permissions.map { p ->
                    cg.epilote.shared.domain.model.ModulePermission(
                        moduleSlug = p.moduleSlug,
                        canRead    = p.canRead,
                        canWrite   = p.canWrite,
                        canDelete  = p.canDelete,
                        canExport  = p.canExport
                    )
                }
            )

            EpiloteDatabase.initUserData(context, dto.userId)
            sessionRepo.saveSession(session)

            // Provisioning App Services (best-effort, n'empêche pas le login si échec réseau)
            var syncToken: String? = null
            val schoolIds = dto.schoolId?.let { listOf(it) } ?: emptyList()
            if (dto.groupId != null || schoolIds.isNotEmpty()) {
                syncToken = runCatching {
                    authApi.provisionSyncUser(dto.userId, dto.groupId, schoolIds, dto.role)
                }.getOrNull()?.syncToken
            }

            // Démarrage de la sync CBLite (best-effort, n'empêche pas le login)
            if (syncToken != null) {
                runCatching {
                    syncManager.start(dto.userId, syncToken)
                }
            }

            LoginResult.Success(session)
        } catch (e: Exception) {
            val msg = e.message ?: (e::class.simpleName ?: "Erreur inconnue")
            when {
                msg.contains("401") || msg.contains("Unauthorized") ||
                msg.contains("Identifiants")                         -> LoginResult.Error("Identifiants incorrects")
                msg.contains("connect") || msg.contains("timeout") ||
                msg.contains("Network") || msg.contains("Unable")   -> LoginResult.NoNetwork
                else                                                 -> LoginResult.Error("Erreur : $msg")
            }
        }
    }
}
