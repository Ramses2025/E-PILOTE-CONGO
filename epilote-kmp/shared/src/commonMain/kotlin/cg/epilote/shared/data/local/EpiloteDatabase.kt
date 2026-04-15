package cg.epilote.shared.data.local

import cg.epilote.shared.platform.initCouchbaseLite
import com.couchbase.lite.Database
import com.couchbase.lite.DatabaseConfiguration

object EpiloteDatabase {

    private var _sessionsDb: Database? = null
    private var _dataDb: Database? = null
    private var _cbliteInitialized = false

    val sessionsInstance: Database
        get() = _sessionsDb ?: error("EpiloteDatabase sessions non initialisé. Appelez initSessions() d'abord.")

    val instance: Database
        get() = _dataDb ?: error("EpiloteDatabase data non initialisé. Appelez initUserData() d'abord.")

    fun initSessions(context: Any?) {
        if (_sessionsDb != null) return
        ensureCbliteInit(context)
        val config = DatabaseConfiguration()
        // SECURITY WARNING: CBLite CE does not support encryption.
        // Session tokens are stored in plaintext on disk.
        // Migrate to CBLite EE or use platform keystore for token storage.
        _sessionsDb = Database("epilote_sessions", config)
        _sessionsDb!!.createCollection("sessions")
    }

    fun initUserData(context: Any?, userId: String) {
        if (_dataDb != null) return
        ensureCbliteInit(context)
        val safeName = "epilote_data_${sanitizeUserId(userId)}"
        val config = DatabaseConfiguration()
        // NOTE: Database encryption requires CBLite Enterprise Edition (commercial license).
        // CE 3.1.3 does not support setEncryptionKey(). Per-user DB isolation provides
        // data separation. EE migration path: config.setEncryptionKey(EncryptionKey(derivedKey))
        _dataDb = Database(safeName, config)
        ensureDataCollections(_dataDb!!)
    }

    fun closeAll() {
        _dataDb?.close()
        _dataDb = null
        _sessionsDb?.close()
        _sessionsDb = null
    }

    fun closeUserData() {
        _dataDb?.close()
        _dataDb = null
    }

    fun isSessionsOpen(): Boolean = _sessionsDb != null
    fun isDataOpen(): Boolean = _dataDb != null
    fun isOpen(): Boolean = _dataDb != null

    private fun ensureCbliteInit(context: Any?) {
        if (!_cbliteInitialized) {
            initCouchbaseLite(context)
            _cbliteInitialized = true
        }
    }

    private fun sanitizeUserId(userId: String): String =
        userId.replace("::", "_").replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

    private fun ensureDataCollections(db: Database) {
        DataCollections.all.forEach { name ->
            db.createCollection(name)
        }
    }
}
