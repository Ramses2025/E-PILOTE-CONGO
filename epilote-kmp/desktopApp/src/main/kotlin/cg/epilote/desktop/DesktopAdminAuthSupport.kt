package cg.epilote.desktop

import cg.epilote.shared.data.local.UserSessionRepository
import cg.epilote.shared.data.remote.ApiClient
import cg.epilote.shared.data.remote.AuthApiService
import cg.epilote.shared.domain.model.UserSession
import cg.epilote.shared.domain.usecase.auth.RefreshTokenUseCase

internal fun buildDesktopAdminTokenProvider(
    sessionRepo: UserSessionRepository,
    sessionProvider: () -> UserSession?
): () -> String? = {
    sessionRepo.getSession()?.accessToken ?: sessionProvider()?.accessToken
}

internal suspend fun refreshDesktopAdminSession(
    baseUrl: String,
    sessionRepo: UserSessionRepository,
    onSessionUpdated: (UserSession?) -> Unit
): Boolean {
    val apiClient = ApiClient(
        baseUrl = baseUrl,
        tokenProvider = { null },
        onTokenExpired = {}
    )
    val authService = AuthApiService(apiClient)
    val refreshed = RefreshTokenUseCase(authService, sessionRepo).execute()
    onSessionUpdated(if (refreshed) sessionRepo.getSession() else null)
    return refreshed
}
