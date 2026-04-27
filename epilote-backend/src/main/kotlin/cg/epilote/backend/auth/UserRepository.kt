package cg.epilote.backend.auth

import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.query.execute
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Repository
 import java.util.UUID

@Repository
class UserRepository(private val bucket: Bucket) {

    private val collection: Collection by lazy {
        runBlocking { bucket.defaultScope().collection("users") }
    }

    private fun migrateRole(raw: String?): UserRole = when (raw) {
        "SUPER_ADMIN"   -> UserRole.SUPER_ADMIN
        "ADMIN_SYSTEME" -> UserRole.SUPER_ADMIN   // garde-fou : rôle inexistant, redirigé vers SUPER_ADMIN
        "ADMIN_GROUPE"  -> UserRole.ADMIN_GROUPE
        "DIRECTOR"      -> UserRole.USER           // garde-fou : rôle inexistant, le directeur est un USER avec profil
        else            -> UserRole.USER
    }

    suspend fun findByEmail(email: String): EpiloteUserDetails? {
        // Comparaison normalisée (LOWER+TRIM) côté N1QL pour s'aligner sur la
        // vérification d'unicité à la création (`AdminRepository.emailAlreadyUsed`).
        // Sans cela, un utilisateur inscrit avec "John@X.com" ne pourrait pas se
        // reconnecter avec "john@x.com" alors que l'unicité l'aurait pourtant
        // rejeté à la création — UX incohérente. Voir Devin Review PR #3.
        val normalized = email.trim().lowercase()
        val result = bucket.defaultScope().query(
            statement = "SELECT META().id, * FROM `users` WHERE LOWER(TRIM(`email`)) = \$email LIMIT 1",
            parameters = com.couchbase.client.kotlin.query.QueryParameters.named("email" to normalized)
        ).execute()

        return result.rows.firstOrNull()?.let { row ->
            val doc = row.contentAs<Map<String, Any>>()
            val inner = doc["users"] as? Map<*, *> ?: doc
            EpiloteUserDetails(
                userId       = doc["id"] as? String ?: "",
                username     = inner["username"] as? String ?: "",
                firstName    = inner["prenom"] as? String ?: "",
                lastName     = inner["nom"] as? String ?: "",
                schoolId     = (inner["schoolId"] ?: inner["ecoleId"]) as? String,
                groupId      = (inner["groupId"] ?: inner["groupeId"]) as? String,
                role         = migrateRole(inner["role"] as? String),
                permissions  = parsePermissions(inner),
                passwordHash = inner["passwordHash"] as? String ?: "",
                email        = inner["email"] as? String ?: "",
                isActive     = inner["isActive"] as? Boolean ?: true
            )
        }
    }

    suspend fun findById(userId: String): EpiloteUserDetails? = runCatching {
        val result = collection.get(userId)
        val doc = result.contentAs<Map<String, Any>>()
        EpiloteUserDetails(
            userId       = userId,
            username     = doc["username"] as? String ?: "",
            email        = doc["email"] as? String ?: "",
            firstName    = doc["prenom"] as? String ?: "",
            lastName     = doc["nom"] as? String ?: "",
            schoolId     = (doc["schoolId"] ?: doc["ecoleId"]) as? String,
            groupId      = (doc["groupId"] ?: doc["groupeId"]) as? String,
            role         = migrateRole(doc["role"] as? String),
            permissions  = parsePermissions(doc),
            passwordHash = doc["passwordHash"] as? String ?: "",
            isActive     = doc["isActive"] as? Boolean ?: true
        )
    }.getOrNull()

    /**
     * Met à jour le `passwordHash` (déjà encodé BCrypt par l'appelant) d'un
     * utilisateur via une opération KV `get → mutate → upsert` cohérente.
     *
     * Le flag `mustChangePassword` est forcé à `false` car l'utilisateur vient,
     * par définition, de définir un nouveau mot de passe.
     *
     * Retourne `true` si la mise à jour a réussi, `false` si l'utilisateur
     * n'existe pas. Toute autre erreur est propagée.
     *
     * Référence officielle Couchbase Kotlin SDK :
     *   https://docs.couchbase.com/kotlin-sdk/current/howtos/kv-operations.html#upsert
     */
    suspend fun updatePasswordHash(userId: String, newPasswordHash: String): Boolean {
        val doc = runCatching { collection.get(userId).contentAs<MutableMap<String, Any?>>() }.getOrNull()
            ?: return false
        doc["passwordHash"] = newPasswordHash
        doc["mustChangePassword"] = false
        doc["passwordChangedAt"] = System.currentTimeMillis()
        doc["updatedAt"] = System.currentTimeMillis()
        collection.upsert(userId, doc)
        return true
    }

    suspend fun ensureSyncToken(userId: String): String {
        val existing = collection.get(userId).contentAs<MutableMap<String, Any?>>()
        val current = existing["syncToken"] as? String
        if (!current.isNullOrBlank()) {
            return current
        }

        val token = UUID.randomUUID().toString()
        existing["syncToken"] = token
        existing["updatedAt"] = System.currentTimeMillis()
        collection.upsert(userId, existing)
        return token
    }

    @Suppress("UNCHECKED_CAST")
    private fun parsePermissions(doc: Map<*, *>): List<PermissionDto> {
        val raw = when (val permissions = doc["permissions"]) {
            is List<*> -> permissions
            is Map<*, *> -> permissions["modules"] as? List<*>
            else -> null
        } ?: return emptyList()
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
