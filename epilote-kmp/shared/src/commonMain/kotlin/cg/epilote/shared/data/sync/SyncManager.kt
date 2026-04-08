package cg.epilote.shared.data.sync

import cg.epilote.shared.domain.model.SyncStatus
import com.couchbase.lite.BasicAuthenticator
import com.couchbase.lite.Database
import com.couchbase.lite.Replicator
import com.couchbase.lite.ReplicatorActivityLevel
import com.couchbase.lite.ReplicatorConfiguration
import com.couchbase.lite.ReplicatorStatus
import com.couchbase.lite.ReplicatorType
import com.couchbase.lite.URLEndpoint
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
        val config = ReplicatorConfiguration(endpoint)
        config.setType(ReplicatorType.PUSH_AND_PULL)
        config.setContinuous(true)
        config.setAuthenticator(BasicAuthenticator(username, password.toCharArray()))
        config.setConflictResolver(EpiloteConflictResolver())
        config.setMaxAttempts(Int.MAX_VALUE)
        config.setMaxAttemptWaitTime(600)
        database.collections.forEach { config.addCollection(it, null) }

        replicator = Replicator(config).apply {
            addChangeListener { change -> updateStatus(change.status) }
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
        val collectionsToCheck = listOf(
            "grades", "attendances", "inscriptions", "students",
            "report_cards", "disciplines", "timetable"
        )
        val pending = collectionsToCheck.sumOf { name ->
            runCatching {
                database.getCollection(name)?.let {
                    replicator?.getPendingDocumentIds(it)?.size?.toLong()
                } ?: 0L
            }.getOrElse { 0L }
        }

        _pendingCount.value = pending

        _syncStatus.value = when {
            status.activityLevel == ReplicatorActivityLevel.STOPPED  -> SyncStatus.OFFLINE
            status.activityLevel == ReplicatorActivityLevel.OFFLINE  -> SyncStatus.OFFLINE
            status.activityLevel == ReplicatorActivityLevel.IDLE && pending == 0L -> SyncStatus.SYNCED
            else -> SyncStatus.PENDING
        }
    }
}
