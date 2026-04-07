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
                userId         = doc["id"] as? String ?: "",
                username       = inner["username"] as? String ?: "",
                ecoleId        = inner["ecoleId"] as? String,
                groupeId       = inner["groupeId"] as? String,
                role           = UserRole.valueOf(inner["role"] as? String ?: "USER"),
                modulesAccess  = @Suppress("UNCHECKED_CAST") (inner["modulesAccess"] as? List<String> ?: emptyList()),
                passwordHash   = inner["passwordHash"] as? String ?: ""
            )
        }
    }

    suspend fun findById(userId: String): EpiloteUserDetails? = runCatching {
        val result = collection.get(userId)
        val doc = result.contentAs<Map<String, Any>>()
        EpiloteUserDetails(
            userId        = userId,
            username      = doc["username"] as? String ?: "",
            ecoleId       = doc["ecoleId"] as? String,
            groupeId      = doc["groupeId"] as? String,
            role          = UserRole.valueOf(doc["role"] as? String ?: "USER"),
            modulesAccess = @Suppress("UNCHECKED_CAST") (doc["modulesAccess"] as? List<String> ?: emptyList()),
            passwordHash  = doc["passwordHash"] as? String ?: ""
        )
    }.getOrNull()
}
