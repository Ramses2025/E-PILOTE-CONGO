package cg.epilote.shared.data.local

import com.couchbase.lite.*

object EpiloteDatabase {

    private var _database: Database? = null

    val instance: Database
        get() = _database ?: error("EpiloteDatabase non initialisé. Appelez init() d'abord.")

    fun init(context: Any?, userId: String, pin: String) {
        if (_database != null) return

        CouchbaseLite.init(context)

        val key = deriveKey(pin, userId)
        val config = DatabaseConfigurationFactory.newConfig(
            encryptionKey = DatabaseEncryptionKey(key)
        )
        _database = Database("epilote_local", config)
        ensureCollections(_database!!)
    }

    fun close() {
        _database?.close()
        _database = null
    }

    private fun ensureCollections(db: Database) {
        listOf("notes", "absences", "eleves", "classes", "matieres",
               "users", "modules_config", "schools").forEach { name ->
            db.createCollection(name)
        }
    }

    private fun deriveKey(pin: String, userId: String): String {
        val combined = "$pin:$userId:epilote_salt_v1"
        return combined.hashCode().toString(16).padStart(32, '0').take(32)
    }
}
