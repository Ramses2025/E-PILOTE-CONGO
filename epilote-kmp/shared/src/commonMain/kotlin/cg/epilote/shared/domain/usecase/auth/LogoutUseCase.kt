package cg.epilote.shared.domain.usecase.auth

import cg.epilote.shared.data.local.EpiloteDatabase
import cg.epilote.shared.data.local.UserSessionRepository
import cg.epilote.shared.data.sync.SyncManager

class LogoutUseCase(
    private val sessionRepo: UserSessionRepository,
    private val syncManager: SyncManager
) {
    fun execute() {
        syncManager.stop()
        sessionRepo.clearSession()
        EpiloteDatabase.closeAll()
    }
}
