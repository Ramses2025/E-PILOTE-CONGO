package cg.epilote.shared.data.local

import cg.epilote.shared.domain.model.ModulePermission
import cg.epilote.shared.domain.model.UserSession
import com.couchbase.lite.*

class UserSessionRepository(private val db: Database) {

    private val collection: Collection
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
        val perms = doc.getArray("permissions")?.let { arr ->
            (0 until arr.count()).mapNotNull { i ->
                arr.getDictionary(i)?.let { d ->
                    ModulePermission(
                        moduleSlug = d.getString("moduleSlug") ?: return@mapNotNull null,
                        canRead    = d.getBoolean("canRead"),
                        canWrite   = d.getBoolean("canWrite"),
                        canDelete  = d.getBoolean("canDelete"),
                        canExport  = d.getBoolean("canExport")
                    )
                }
            }
        } ?: emptyList()
        return UserSession(
            userId               = doc.getString("userId") ?: return null,
            username             = doc.getString("username") ?: return null,
            firstName            = doc.getString("firstName") ?: "",
            lastName             = doc.getString("lastName") ?: "",
            ecoleId              = doc.getString("ecoleId")?.takeIf { it.isNotEmpty() },
            groupeId             = doc.getString("groupeId")?.takeIf { it.isNotEmpty() },
            role                 = doc.getString("role") ?: "USER",
            accessToken          = doc.getString("accessToken") ?: return null,
            refreshToken         = doc.getString("refreshToken") ?: return null,
            offlineToken         = doc.getString("offlineToken") ?: return null,
            offlineTokenExpiresAt = doc.getLong("offlineTokenExpiresAt"),
            permissions          = perms
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
