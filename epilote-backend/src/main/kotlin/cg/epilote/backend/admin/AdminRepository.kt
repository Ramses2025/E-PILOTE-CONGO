package cg.epilote.backend.admin

import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import com.couchbase.client.kotlin.query.execute
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class AdminRepository(private val bucket: Bucket) {

    private val scope by lazy { runBlocking { bucket.defaultScope() } }

    private fun col(name: String): Collection = runBlocking { scope.collection(name) }

    // ── Helpers ──────────────────────────────────────────────────

    private fun newId(prefix: String): String = "$prefix::${UUID.randomUUID()}"
    private fun now(): Long = System.currentTimeMillis()

    // ── Groupes ──────────────────────────────────────────────────

    suspend fun createGroupe(req: CreateGroupeRequest, createdBy: String): GroupeResponse {
        val id = newId("groupe")
        val doc = mapOf(
            "type"        to "groupe",
            "nom"         to req.nom,
            "province"    to req.province,
            "planId"      to req.planId,
            "ecolesCount" to 0,
            "adminIds"    to emptyList<String>(),
            "createdBy"   to createdBy,
            "createdAt"   to now(),
            "updatedAt"   to now()
        )
        col("groupes").upsert(id, doc)
        return GroupeResponse(id, req.nom, req.province, req.planId, 0, now())
    }

    suspend fun listGroupes(): List<GroupeResponse> {
        val result = scope.query("SELECT META().id, * FROM `groupes` WHERE type = 'groupe'").execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d["groupes"] as? Map<*, *> ?: d
            GroupeResponse(
                id          = d["id"] as? String ?: "",
                nom         = inner["nom"] as? String ?: "",
                province    = inner["province"] as? String ?: "",
                planId      = inner["planId"] as? String ?: "",
                ecolesCount = (inner["ecolesCount"] as? Number)?.toInt() ?: 0,
                createdAt   = (inner["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    // ── Écoles ───────────────────────────────────────────────────

    suspend fun createEcole(groupeId: String, req: CreateEcoleRequest, planId: String): EcoleResponse {
        val id = newId("school")
        val doc = mapOf(
            "type"        to "school",
            "groupeId"    to groupeId,
            "nom"         to req.nom,
            "province"    to req.province,
            "territoire"  to req.territoire,
            "niveaux"     to req.niveaux,
            "planId"      to planId,
            "isActive"    to true,
            "createdAt"   to now(),
            "updatedAt"   to now()
        )
        col("schools").upsert(id, doc)
        return EcoleResponse(id, groupeId, req.nom, req.province, req.territoire, req.niveaux, planId, now())
    }

    suspend fun listEcolesByGroupe(groupeId: String): List<EcoleResponse> {
        val result = scope.query(
            "SELECT META().id, * FROM `schools` WHERE type = 'school' AND groupeId = \$groupeId",
            parameters = com.couchbase.client.kotlin.query.QueryParameters.named("groupeId" to groupeId)
        ).execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d["schools"] as? Map<*, *> ?: d
            EcoleResponse(
                id          = d["id"] as? String ?: "",
                groupeId    = inner["groupeId"] as? String ?: "",
                nom         = inner["nom"] as? String ?: "",
                province    = inner["province"] as? String ?: "",
                territoire  = inner["territoire"] as? String ?: "",
                niveaux     = @Suppress("UNCHECKED_CAST") (inner["niveaux"] as? List<String> ?: emptyList()),
                planId      = inner["planId"] as? String ?: "",
                createdAt   = (inner["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    // ── Profils ──────────────────────────────────────────────────

    suspend fun createProfil(groupeId: String, req: CreateProfilRequest, createdBy: String): ProfilResponse {
        val id = newId("profil")
        val doc = mapOf(
            "type"        to "profil",
            "groupeId"    to groupeId,
            "nom"         to req.nom,
            "permissions" to req.permissions.map { p ->
                mapOf(
                    "moduleSlug" to p.moduleSlug,
                    "canRead"    to p.canRead,
                    "canWrite"   to p.canWrite,
                    "canDelete"  to p.canDelete,
                    "canExport"  to p.canExport
                )
            },
            "createdBy"   to createdBy,
            "createdAt"   to now(),
            "updatedAt"   to now()
        )
        col("profils").upsert(id, doc)
        return ProfilResponse(id, groupeId, req.nom, req.permissions, now())
    }

    suspend fun listProfilsByGroupe(groupeId: String): List<ProfilResponse> {
        val result = scope.query(
            "SELECT META().id, * FROM `profils` WHERE type = 'profil' AND groupeId = \$groupeId",
            parameters = com.couchbase.client.kotlin.query.QueryParameters.named("groupeId" to groupeId)
        ).execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d["profils"] as? Map<*, *> ?: d
            ProfilResponse(
                id          = d["id"] as? String ?: "",
                groupeId    = inner["groupeId"] as? String ?: "",
                nom         = inner["nom"] as? String ?: "",
                permissions = parseProfilPermissions(inner),
                createdAt   = (inner["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    // ── Utilisateurs ─────────────────────────────────────────────

    suspend fun createUser(groupeId: String, req: CreateUserRequest, passwordHash: String, profilPermissions: List<ProfilPermission>): UserResponse {
        val id = newId("user")
        val doc = mapOf(
            "type"         to "user",
            "username"     to req.username,
            "passwordHash" to passwordHash,
            "nom"          to req.nom,
            "prenom"       to req.prenom,
            "email"        to req.email,
            "ecoleId"      to req.ecoleId,
            "groupeId"     to groupeId,
            "profilId"     to req.profilId,
            "role"         to "USER",
            "permissions"  to profilPermissions.map { p ->
                mapOf(
                    "moduleSlug" to p.moduleSlug,
                    "canRead"    to p.canRead,
                    "canWrite"   to p.canWrite,
                    "canDelete"  to p.canDelete,
                    "canExport"  to p.canExport
                )
            },
            "isActive"     to true,
            "createdAt"    to now(),
            "updatedAt"    to now()
        )
        col("users").upsert(id, doc)
        return UserResponse(id, req.username, req.prenom, req.nom, req.email,
            req.ecoleId, groupeId, req.profilId, "USER", true, now())
    }

    suspend fun listUsersByEcole(ecoleId: String): List<UserResponse> {
        val result = scope.query(
            "SELECT META().id, * FROM `users` WHERE type = 'user' AND ecoleId = \$ecoleId",
            parameters = com.couchbase.client.kotlin.query.QueryParameters.named("ecoleId" to ecoleId)
        ).execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d["users"] as? Map<*, *> ?: d
            UserResponse(
                id        = d["id"] as? String ?: "",
                username  = inner["username"] as? String ?: "",
                firstName = inner["prenom"] as? String ?: "",
                lastName  = inner["nom"] as? String ?: "",
                email     = inner["email"] as? String ?: "",
                ecoleId   = inner["ecoleId"] as? String ?: "",
                groupeId  = inner["groupeId"] as? String ?: "",
                profilId  = inner["profilId"] as? String ?: "",
                role      = inner["role"] as? String ?: "USER",
                isActive  = inner["isActive"] as? Boolean ?: true,
                createdAt = (inner["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    suspend fun getProfilById(profilId: String): ProfilResponse? = runCatching {
        val result = col("profils").get(profilId)
        val doc = result.contentAs<Map<String, Any>>()
        ProfilResponse(
            id          = profilId,
            groupeId    = doc["groupeId"] as? String ?: "",
            nom         = doc["nom"] as? String ?: "",
            permissions = parseProfilPermissions(doc),
            createdAt   = (doc["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }.getOrNull()

    @Suppress("UNCHECKED_CAST")
    private fun parseProfilPermissions(doc: Map<*, *>): List<ProfilPermission> {
        val raw = doc["permissions"] as? List<*> ?: return emptyList()
        return raw.filterIsInstance<Map<String, Any>>().map { p ->
            ProfilPermission(
                moduleSlug = p["moduleSlug"] as? String ?: "",
                canRead    = p["canRead"] as? Boolean ?: true,
                canWrite   = p["canWrite"] as? Boolean ?: false,
                canDelete  = p["canDelete"] as? Boolean ?: false,
                canExport  = p["canExport"] as? Boolean ?: false
            )
        }
    }

    // ── Modules ──────────────────────────────────────────────────

    suspend fun createModule(req: CreateModuleRequest): ModuleResponse {
        val id = "module::${req.code}"
        val doc = mapOf(
            "type"                to "module",
            "code"                to req.code,
            "nom"                 to req.nom,
            "categorieCode"       to req.categorieCode,
            "description"         to req.description,
            "requiredPermissions" to req.requiredPermissions,
            "isActive"            to true,
            "version"             to "1.0.0",
            "updatedAt"           to now()
        )
        col("modules").upsert(id, doc)
        return ModuleResponse(id, req.code, req.nom, req.categorieCode, req.description, true)
    }

    suspend fun listModules(): List<ModuleResponse> {
        val result = scope.query("SELECT META().id, * FROM `modules` WHERE type = 'module'").execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d["modules"] as? Map<*, *> ?: d
            ModuleResponse(
                id           = d["id"] as? String ?: "",
                code         = inner["code"] as? String ?: "",
                nom          = inner["nom"] as? String ?: "",
                categorieCode = inner["categorieCode"] as? String ?: "",
                description  = inner["description"] as? String ?: "",
                isActive     = inner["isActive"] as? Boolean ?: true
            )
        }
    }

    // ── Plans ────────────────────────────────────────────────────

    suspend fun createPlan(req: CreatePlanRequest): PlanResponse {
        val id = newId("plan")
        val doc = mapOf(
            "type"               to "plan",
            "nom"                to req.nom,
            "maxEcoles"          to req.maxEcoles,
            "maxUtilisateurs"    to req.maxUtilisateurs,
            "modulesIncluded"    to req.modulesIncluded,
            "categoriesIncluded" to req.categoriesIncluded,
            "dureeJours"         to req.dureeJours,
            "updatedAt"          to now()
        )
        col("plans").upsert(id, doc)
        return PlanResponse(id, req.nom, req.maxEcoles, req.maxUtilisateurs,
            req.modulesIncluded, req.categoriesIncluded, req.dureeJours)
    }

    suspend fun listPlans(): List<PlanResponse> {
        val result = scope.query("SELECT META().id, * FROM `plans` WHERE type = 'plan'").execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d["plans"] as? Map<*, *> ?: d
            PlanResponse(
                id                  = d["id"] as? String ?: "",
                nom                 = inner["nom"] as? String ?: "",
                maxEcoles           = (inner["maxEcoles"] as? Number)?.toInt() ?: 0,
                maxUtilisateurs     = (inner["maxUtilisateurs"] as? Number)?.toInt() ?: 0,
                modulesIncluded     = @Suppress("UNCHECKED_CAST") (inner["modulesIncluded"] as? List<String> ?: emptyList()),
                categoriesIncluded  = @Suppress("UNCHECKED_CAST") (inner["categoriesIncluded"] as? List<String> ?: emptyList()),
                dureeJours          = (inner["dureeJours"] as? Number)?.toInt() ?: 365
            )
        }
    }
}
