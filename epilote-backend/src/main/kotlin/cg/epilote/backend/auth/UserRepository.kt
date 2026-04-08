package cg.epilote.backend.auth

import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.query.execute
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Repository

@Repository
class UserRepository(private val bucket: Bucket) {

    private val collection: Collection by lazy {
        runBlocking { bucket.defaultScope().collection("users") }
    }

    suspend fun findByUsername(username: String): EpiloteUserDetails? {
        val result = bucket.defaultScope().query(
            statement = "SELECT META().id, * FROM `users` WHERE username = \$username LIMIT 1",
            parameters = com.couchbase.client.kotlin.query.QueryParameters.named("username" to username)
        ).execute()

        return result.rows.firstOrNull()?.let { row ->
            val doc = row.contentAs<Map<String, Any>>()
            val inner = doc["users"] as? Map<*, *> ?: doc
            EpiloteUserDetails(
                userId       = doc["id"] as? String ?: "",
                username     = inner["username"] as? String ?: "",
                firstName    = inner["prenom"] as? String ?: "",
                lastName     = inner["nom"] as? String ?: "",
                ecoleId      = inner["ecoleId"] as? String,
                groupeId     = inner["groupeId"] as? String,
                role         = UserRole.valueOf(inner["role"] as? String ?: "USER"),
                permissions  = parsePermissions(inner),
                passwordHash = inner["passwordHash"] as? String ?: ""
            )
        }
    }

    suspend fun findById(userId: String): EpiloteUserDetails? = runCatching {
        val result = collection.get(userId)
        val doc = result.contentAs<Map<String, Any>>()
        EpiloteUserDetails(
            userId       = userId,
            username     = doc["username"] as? String ?: "",
            firstName    = doc["prenom"] as? String ?: "",
            lastName     = doc["nom"] as? String ?: "",
            ecoleId      = doc["ecoleId"] as? String,
            groupeId     = doc["groupeId"] as? String,
            role         = UserRole.valueOf(doc["role"] as? String ?: "USER"),
            permissions  = parsePermissions(doc),
            passwordHash = doc["passwordHash"] as? String ?: ""
        )
    }.getOrNull()

    @Suppress("UNCHECKED_CAST")
    private fun parsePermissions(doc: Map<*, *>): List<PermissionDto> {
        val raw = doc["permissions"] as? List<*> ?: return emptyList()
        return raw.filterIsInstance<Map<String, Any>>().map { p ->
            PermissionDto(
                moduleSlug = p["moduleSlug"] as? String ?: "",
                canRead    = p["canRead"] as? Boolean ?: true,
                canWrite   = p["canWrite"] as? Boolean ?: false,
                canDelete  = p["canDelete"] as? Boolean ?: false,
                canExport  = p["canExport"] as? Boolean ?: false
            )
        }
    }
}
