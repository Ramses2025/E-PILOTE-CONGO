package cg.epilote.shared.data.local

import cg.epilote.shared.platform.initCouchbaseLite
import com.couchbase.lite.Database
import com.couchbase.lite.DatabaseConfiguration

object EpiloteDatabase {

    private var _database: Database? = null

    val instance: Database
        get() = _database ?: error("EpiloteDatabase non initialisé. Appelez init() d'abord.")

    fun init(context: Any?, userId: String, pin: String) {
        if (_database != null) return

        initCouchbaseLite(context)

        val config = DatabaseConfiguration()
        _database = Database("epilote_local", config)
        ensureCollections(_database!!)
    }

    fun close() {
        _database?.close()
        _database = null
    }

    fun isOpen(): Boolean = _database != null

    private fun ensureCollections(db: Database) {
        listOf(
            "grades", "attendances", "students", "inscriptions",
            "timetable", "report_cards", "disciplines",
            "academic_config", "users", "config",
            "school_groups", "schools", "staff",
            "announcements", "messages", "notifications",
            "sync_mutations", "sync_conflicts", "sessions"
        ).forEach { name ->
            db.createCollection(name)
        }
    }
}
