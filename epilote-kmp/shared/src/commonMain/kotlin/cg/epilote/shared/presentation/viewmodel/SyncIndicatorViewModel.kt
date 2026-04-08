package cg.epilote.shared.presentation.viewmodel

import cg.epilote.shared.data.sync.SyncManager
import cg.epilote.shared.domain.model.SyncStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SyncIndicatorViewModel(syncManager: SyncManager) {

    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus
    val pendingCount: StateFlow<Long>     = syncManager.pendingCount

    val label: kotlinx.coroutines.flow.Flow<String> =
        combine(syncManager.syncStatus, syncManager.pendingCount) { status, count ->
            when (status) {
                SyncStatus.SYNCED   -> "Synchronisé ✓"
                SyncStatus.OFFLINE  -> "Hors ligne"
                SyncStatus.PENDING  -> if (count > 0) "$count élément(s) en attente" else "Synchronisation..."
                SyncStatus.ERROR    -> "Erreur de synchronisation"
                SyncStatus.CONFLICT -> "Conflits détectés"
            }
        }

    val emoji: kotlinx.coroutines.flow.Flow<String> =
        syncManager.syncStatus.map { status ->
            when (status) {
                SyncStatus.SYNCED   -> "🟢"
                SyncStatus.OFFLINE  -> "🔴"
                SyncStatus.PENDING  -> "🟡"
                SyncStatus.ERROR    -> "🔴"
                SyncStatus.CONFLICT -> "🟠"
            }
        }
}
