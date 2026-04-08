package cg.epilote.shared.data.local

import cg.epilote.shared.domain.model.ModulePermission
import cg.epilote.shared.domain.model.UserSession
import com.couchbase.lite.Database
import com.couchbase.lite.MutableArray
import com.couchbase.lite.MutableDictionary
import com.couchbase.lite.MutableDocument

class UserSessionRepository(private val db: Database) {

    private val collection: com.couchbase.lite.Collection
        get() = db.getCollection("sessions") ?: db.createCollection("sessions")

    private val SESSION_ID = "current_session"

    fun saveSession(session: UserSession) {
        val doc = MutableDocument(SESSION_ID).apply {
            setString("userId",               session.userId)
            setString("username",             session.username)
            setString("firstName",            session.firstName)
            setString("lastName",             session.lastName)
            setString("ecoleId",              session.ecoleId ?: "")
            setString("groupeId",             session.groupeId ?: "")
            setString("role",                 session.role)
            setString("accessToken",          session.accessToken)
            setString("refreshToken",         session.refreshToken)
            setString("offlineToken",         session.offlineToken)
            setLong("offlineTokenExpiresAt",  session.offlineTokenExpiresAt)
            setLong("savedAt",                System.currentTimeMillis())

            val permsArray = MutableArray()
            session.permissions.forEach { perm ->
                val d = MutableDictionary()
                d.setString("moduleSlug",  perm.moduleSlug)
                d.setBoolean("canRead",    perm.canRead)
                d.setBoolean("canWrite",   perm.canWrite)
                d.setBoolean("canDelete",  perm.canDelete)
                d.setBoolean("canExport",  perm.canExport)
                permsArray.addDictionary(d)
            }
            setArray("permissions", permsArray)
        }
        collection.save(doc)
    }

    fun getSession(): UserSession? {
        val doc = collection.getDocument(SESSION_ID) ?: return null

        val permsArr = doc.getArray("permissions")
        val perms: List<ModulePermission> = if (permsArr != null) {
            (0 until permsArr.count()).mapNotNull { i ->
                val d = permsArr.getDictionary(i) ?: return@mapNotNull null
                val slug = d.getString("moduleSlug") ?: return@mapNotNull null
                ModulePermission(
                    moduleSlug = slug,
                    canRead    = d.getBoolean("canRead"),
                    canWrite   = d.getBoolean("canWrite"),
                    canDelete  = d.getBoolean("canDelete"),
                    canExport  = d.getBoolean("canExport")
                )
            }
        } else emptyList()

        val userId      = doc.getString("userId")      ?: return null
        val username    = doc.getString("username")    ?: return null
        val accessToken = doc.getString("accessToken") ?: return null
        val refreshToken = doc.getString("refreshToken") ?: return null
        val offlineToken = doc.getString("offlineToken") ?: return null

        return UserSession(
            userId                = userId,
            username              = username,
            firstName             = doc.getString("firstName") ?: "",
            lastName              = doc.getString("lastName") ?: "",
            ecoleId               = doc.getString("ecoleId")?.takeIf { s -> s.isNotEmpty() },
            groupeId              = doc.getString("groupeId")?.takeIf { s -> s.isNotEmpty() },
            role                  = doc.getString("role") ?: "USER",
            accessToken           = accessToken,
            refreshToken          = refreshToken,
            offlineToken          = offlineToken,
            offlineTokenExpiresAt = doc.getLong("offlineTokenExpiresAt"),
            permissions           = perms
        )
    }

    fun updateAccessToken(newToken: String) {
        val doc = collection.getDocument(SESSION_ID)?.toMutable() ?: return
        doc.setString("accessToken", newToken)
        collection.save(doc)
    }

    fun clearSession() {
        collection.getDocument(SESSION_ID)?.let { collection.delete(it) }
    }

    fun hasSession(): Boolean = collection.getDocument(SESSION_ID) != null
}
