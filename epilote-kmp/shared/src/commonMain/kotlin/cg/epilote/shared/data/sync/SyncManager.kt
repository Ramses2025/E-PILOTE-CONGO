package cg.epilote.shared.data.sync

import cg.epilote.shared.domain.model.SyncStatus
import com.couchbase.lite.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI

class SyncManager(
    private val database: Database,
    private val syncGatewayUrl: String
) {
    private var replicator: Replicator? = null

    private val _syncStatus = MutableStateFlow(SyncStatus.OFFLINE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _pendingCount = MutableStateFlow(0L)
    val pendingCount: StateFlow<Long> = _pendingCount.asStateFlow()

    fun start(username: String, password: String) {
        if (replicator != null) return

        val endpoint = URLEndpoint(URI(syncGatewayUrl))
        val config = ReplicatorConfigurationFactory.newConfig(
            target = endpoint,
            collections = mapOf(database.collections to null),
            type = ReplicatorType.PUSH_AND_PULL,
            continuous = true,
            authenticator = BasicAuthenticator(username, password.toCharArray()),
            conflictResolver = EpiloteConflictResolver(),
            maxAttempts = Int.MAX_VALUE,
            maxAttemptWaitTime = 600u
        )

        replicator = Replicator(config).apply {
            addChangeListener { change ->
                updateStatus(change.status)
            }
            start()
        }
    }

    fun stop() {
        replicator?.stop()
        replicator = null
        _syncStatus.value = SyncStatus.OFFLINE
    }

    fun onNetworkAvailable() {
        replicator?.start()
        _syncStatus.value = SyncStatus.PENDING
    }

    fun onNetworkLost() {
        replicator?.stop()
        _syncStatus.value = SyncStatus.OFFLINE
    }

    private fun updateStatus(status: ReplicatorStatus) {
        val pending = replicator?.getPendingDocumentIds(
            database.getCollection("grades")!!
        )?.size?.toLong() ?: 0L

        _pendingCount.value = pending

        _syncStatus.value = when {
            status.activityLevel == ReplicatorActivityLevel.STOPPED  -> SyncStatus.OFFLINE
            status.activityLevel == ReplicatorActivityLevel.OFFLINE  -> SyncStatus.OFFLINE
            status.activityLevel == ReplicatorActivityLevel.IDLE && pending == 0L -> SyncStatus.SYNCED
            else -> SyncStatus.PENDING
        }
    }
}
