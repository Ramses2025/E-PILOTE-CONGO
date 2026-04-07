package cg.epilote.shared.domain.usecase.auth

import cg.epilote.shared.data.local.UserSessionRepository
import cg.epilote.shared.data.remote.AuthApiService

class RefreshTokenUseCase(
    private val authApi: AuthApiService,
    private val sessionRepo: UserSessionRepository
) {
    suspend fun execute(): Boolean {
        val session = sessionRepo.getSession() ?: return false
        return runCatching {
            val dto = authApi.refresh(session.refreshToken)
            sessionRepo.updateAccessToken(dto.accessToken)
            true
        }.getOrDefault(false)
    }
}
