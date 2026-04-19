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

    private companion object {
        const val GROUPS_COLLECTION = "school_groups"
        const val LEGACY_GROUPS_COLLECTION = "groupes"
        const val INVOICES_COLLECTION = "invoices"
        const val LEGACY_INVOICES_COLLECTION = "invoices_platform"
        const val SUBSCRIPTIONS_COLLECTION = "subscriptions"
    }

    private fun col(name: String): Collection = runBlocking { scope.collection(name) }

    // ── Helpers ──────────────────────────────────────────────────

    private fun newId(prefix: String): String = "$prefix::${UUID.randomUUID()}"
    private fun now(): Long = System.currentTimeMillis()

    // ── Groupes ──────────────────────────────────────────────────

    private fun generateSlug(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val suffix = (1..7).map { chars.random() }.joinToString("")
        return "E-PILOT-$suffix"
    }

    suspend fun createGroupe(req: CreateGroupeRequest, createdBy: String): GroupeResponse {
        val id = newId("groupe")
        val slug = generateSlug()
        val normalizedLogo = req.logo?.trim()?.takeIf { it.isNotEmpty() }
        val doc = mapOf(
            "type"        to "school_group",
            "nom"         to req.nom,
            "slug"        to slug,
            "email"       to req.email,
            "phone"       to req.phone,
            "department"  to req.department,
            "city"        to req.city,
            "address"     to req.address,
            "country"     to "Congo",
            "logo"        to normalizedLogo,
            "description" to req.description,
            "foundedYear" to req.foundedYear,
            "website"     to req.website,
            "planId"      to req.planId,
            "ecolesCount" to 0,
            "isActive"    to true,
            "adminIds"    to emptyList<String>(),
            "createdBy"   to createdBy,
            "createdAt"   to now(),
            "updatedAt"   to now()
        )
        col(GROUPS_COLLECTION).upsert(id, doc)
        createDefaultProfils(id, req.planId)
        return GroupeResponse(
            id = id, nom = req.nom, slug = slug,
            email = req.email, phone = req.phone,
            department = req.department, city = req.city, address = req.address,
            country = "Congo", logo = normalizedLogo, description = req.description,
            foundedYear = req.foundedYear, website = req.website,
            planId = req.planId, ecolesCount = 0, usersCount = 0,
            isActive = true, createdAt = now()
        )
    }

    private fun mapToGroupeResponse(id: String, inner: Map<*, *>): GroupeResponse {
        return GroupeResponse(
            id          = id,
            nom         = inner["nom"] as? String ?: inner["name"] as? String ?: "",
            slug        = inner["slug"] as? String ?: "",
            email       = inner["email"] as? String,
            phone       = inner["phone"] as? String,
            department  = inner["department"] as? String ?: inner["province"] as? String,
            city        = inner["city"] as? String,
            address     = inner["address"] as? String,
            country     = inner["country"] as? String ?: "Congo",
            logo        = inner["logo"] as? String,
            description = inner["description"] as? String,
            foundedYear = (inner["foundedYear"] as? Number)?.toInt() ?: (inner["founded_year"] as? Number)?.toInt(),
            website     = inner["website"] as? String,
            planId      = inner["planId"] as? String ?: "",
            ecolesCount = (inner["ecolesCount"] as? Number)?.toInt() ?: 0,
            usersCount  = 0,
            isActive    = inner["isActive"] as? Boolean ?: inner["is_active"] as? Boolean ?: true,
            createdAt   = (inner["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }

    suspend fun listGroupes(): List<GroupeResponse> {
        val result = scope.query("SELECT META().id, * FROM `${GROUPS_COLLECTION}` WHERE `type` IN ['school_group', 'groupe']").execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d[GROUPS_COLLECTION] as? Map<*, *> ?: d[LEGACY_GROUPS_COLLECTION] as? Map<*, *> ?: d
            mapToGroupeResponse(d["id"] as? String ?: "", inner)
        }
    }

    suspend fun getPlanById(planId: String): PlanResponse? = runCatching {
        val doc = col("plans").get(planId).contentAs<Map<String, Any>>()
        PlanResponse(
            id                 = planId,
            nom                = doc["nom"] as? String ?: "",
            prixXAF            = (doc["prixXAF"] as? Number)?.toLong() ?: 0L,
            maxEcoles          = (doc["maxEcoles"] as? Number)?.toInt() ?: 0,
            maxUtilisateurs    = (doc["maxUtilisateurs"] as? Number)?.toInt() ?: 0,
            modulesIncluded    = @Suppress("UNCHECKED_CAST") (doc["modulesIncluded"] as? List<String> ?: emptyList()),
            categoriesIncluded = @Suppress("UNCHECKED_CAST") (doc["categoriesIncluded"] as? List<String> ?: emptyList()),
            dureeJours         = (doc["dureeJours"] as? Number)?.toInt() ?: 365,
            isActive           = doc["isActive"] as? Boolean ?: true
        )
    }.getOrNull()

    suspend fun getGroupeById(groupeId: String): GroupeResponse? = runCatching {
        val doc = col(GROUPS_COLLECTION).get(groupeId).contentAs<Map<String, Any>>()
        mapToGroupeResponse(groupeId, doc)
    }.getOrNull()

    suspend fun getModulesDisponibles(groupeId: String): List<ModuleResponse> {
        val groupe = getGroupeById(groupeId) ?: return emptyList()
        val plan = getPlanById(groupe.planId) ?: return emptyList()
        val allowed = plan.modulesIncluded.toSet()
        return listModules().filter { it.code in allowed }
    }

    private suspend fun createDefaultProfils(groupeId: String, planId: String) {
        val plan = getPlanById(planId)
        val planModules = plan?.modulesIncluded?.toSet() ?: emptySet()

        data class DefaultProfil(val code: String, val nom: String, val writeModules: List<String>)

        fun perms(vararg slugs: String) = slugs
            .filter { it in planModules }
            .map { ProfilPermission(it, canRead = true, canWrite = true, canDelete = false, canExport = true) }

        val defaults = listOf(
            DefaultProfil("chef_etablissement", "Chef d'\u00e9tablissement", planModules.toList()),
            DefaultProfil("directeur",          "Directeur",               planModules.toList()),
            DefaultProfil("enseignant",         "Enseignant",              listOf("notes", "matieres", "bulletins", "presences-eleves", "evaluations", "cahier-textes")),
            DefaultProfil("surveillant",        "Surveillant",             listOf("presences-eleves", "discipline")),
            DefaultProfil("comptable",          "Comptable",               listOf("finances", "facturation", "depenses", "budget", "comptabilite")),
            DefaultProfil("secretaire",         "Secr\u00e9taire",               listOf("inscriptions", "eleves", "transferts", "documents"))
        )

        defaults.forEach { dp ->
            val permissions = dp.writeModules
                .filter { it in planModules }
                .map { ProfilPermission(it, canRead = true, canWrite = true, canDelete = false, canExport = true) }
                .ifEmpty {
                    planModules.map { ProfilPermission(it, canRead = true, canWrite = false, canDelete = false, canExport = false) }
                }
            val id = "profil::${groupeId}::${dp.code}"
            val doc = mapOf(
                "type"        to "profil",
                "groupId"     to groupeId,
                "nom"         to dp.nom,
                "code"        to dp.code,
                "isDefault"   to true,
                "permissions" to permissions.map { p ->
                    mapOf(
                        "moduleSlug" to p.moduleSlug,
                        "canRead"    to p.canRead,
                        "canWrite"   to p.canWrite,
                        "canDelete"  to p.canDelete,
                        "canExport"  to p.canExport
                    )
                },
                "createdBy"   to "system",
                "createdAt"   to now(),
                "updatedAt"   to now()
            )
            col("profils").upsert(id, doc)
        }
    }

    // ── Écoles ───────────────────────────────────────────────────

    suspend fun createEcole(groupeId: String, req: CreateEcoleRequest, planId: String): EcoleResponse {
        val id = newId("school")
        val doc = mapOf(
            "type"        to "school",
            "groupId"     to groupeId,
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
            "SELECT META().id, * FROM `schools` WHERE `type` = 'school' AND (`groupId` = \$gid OR `groupeId` = \$gid)",
            parameters = com.couchbase.client.kotlin.query.QueryParameters.named("gid" to groupeId)
        ).execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d["schools"] as? Map<*, *> ?: d
            EcoleResponse(
                id          = d["id"] as? String ?: "",
                groupId     = (inner["groupId"] ?: inner["groupeId"]) as? String ?: "",
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
        val groupe = getGroupeById(groupeId)
        if (groupe != null) {
            val plan = getPlanById(groupe.planId)
            if (plan != null && plan.modulesIncluded.isNotEmpty()) {
                val allowed = plan.modulesIncluded.toSet()
                val invalid = req.permissions.map { it.moduleSlug }.filter { it !in allowed }
                if (invalid.isNotEmpty()) throw InvalidModuleForPlanException(invalid)
            }
        }
        val id = newId("profil")
        val doc = mapOf(
            "type"        to "profil",
            "groupId"     to groupeId,
            "nom"         to req.nom,
            "isDefault"   to false,
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
        return ProfilResponse(id, groupeId, req.nom, req.permissions, false, now())
    }

    suspend fun listProfilsByGroupe(groupeId: String): List<ProfilResponse> {
        val result = scope.query(
            "SELECT META().id, * FROM `profils` WHERE `type` = 'profil' AND (`groupId` = \$gid OR `groupeId` = \$gid)",
            parameters = com.couchbase.client.kotlin.query.QueryParameters.named("gid" to groupeId)
        ).execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d["profils"] as? Map<*, *> ?: d
            ProfilResponse(
                id          = d["id"] as? String ?: "",
                groupId     = (inner["groupId"] ?: inner["groupeId"]) as? String ?: "",
                nom         = inner["nom"] as? String ?: "",
                permissions = parseProfilPermissions(inner),
                isDefault   = inner["isDefault"] as? Boolean ?: false,
                createdAt   = (inner["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    // ── Utilisateurs ─────────────────────────────────────────────

    private fun effectiveUsername(email: String, username: String?): String =
        username?.trim()?.takeIf { it.isNotEmpty() }
            ?: email.substringBefore("@").ifBlank { email }

    suspend fun createAdminGroupe(groupeId: String, req: CreateAdminGroupeRequest, passwordHash: String): UserResponse {
        val id = newId("user")
        val username = effectiveUsername(req.email, req.username)
        val doc = mapOf(
            "type"         to "user",
            "username"     to username,
            "passwordHash" to passwordHash,
            "nom"          to req.nom,
            "prenom"       to req.prenom,
            "email"        to req.email,
            "schoolId"     to null,
            "groupId"      to groupeId,
            "profilId"     to null,
            "role"         to "ADMIN_GROUPE",
            "permissions"  to emptyList<Map<String, Any>>(),
            "isActive"     to true,
            "createdAt"    to now(),
            "updatedAt"    to now()
        )
        col("users").upsert(id, doc)
        val groupeCollection = col(GROUPS_COLLECTION)
        val groupeDoc = groupeCollection.get(groupeId).contentAs<Map<String, Any>>()
        val currentAdminIds = (groupeDoc["adminIds"] as? List<*>)
            ?.mapNotNull { it as? String }
            ?.toMutableSet()
            ?: mutableSetOf()
        currentAdminIds += id
        val updatedGroupe = groupeDoc.toMutableMap()
        updatedGroupe["adminIds"] = currentAdminIds.toList()
        updatedGroupe["updatedAt"] = now()
        groupeCollection.upsert(groupeId, updatedGroupe)
        return UserResponse(id, username, req.prenom, req.nom, req.email, null, groupeId, null, "ADMIN_GROUPE", true, now())
    }

    suspend fun createUser(groupeId: String, req: CreateUserRequest, passwordHash: String, profilPermissions: List<ProfilPermission>): UserResponse {
        val id = newId("user")
        val username = effectiveUsername(req.email, req.username)
        val doc = mapOf(
            "type"         to "user",
            "username"     to username,
            "passwordHash" to passwordHash,
            "nom"          to req.nom,
            "prenom"       to req.prenom,
            "email"        to req.email,
            "schoolId"     to req.schoolId,
            "groupId"      to groupeId,
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
        return UserResponse(id, username, req.prenom, req.nom, req.email,
            req.schoolId, groupeId, req.profilId, "USER", true, now())
    }

    suspend fun listAdminGroupesByGroupe(groupeId: String): List<UserResponse> {
        val result = scope.query(
            "SELECT META().id, * FROM `users` WHERE `type` = 'user' AND (`groupId` = \$gid OR `groupeId` = \$gid) AND `role` = 'ADMIN_GROUPE'",
            parameters = com.couchbase.client.kotlin.query.QueryParameters.named("gid" to groupeId)
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
                schoolId  = (inner["schoolId"] ?: inner["schoolId"]) as? String,
                groupId   = (inner["groupId"] ?: inner["groupeId"]) as? String ?: "",
                profilId  = inner["profilId"] as? String,
                role      = inner["role"] as? String ?: "ADMIN_GROUPE",
                isActive  = inner["isActive"] as? Boolean ?: true,
                createdAt = (inner["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    suspend fun listUsersByEcole(schoolId: String): List<UserResponse> {
        val result = scope.query(
            "SELECT META().id, * FROM `users` WHERE `type` = 'user' AND (`schoolId` = \$sid OR `schoolId` = \$sid)",
            parameters = com.couchbase.client.kotlin.query.QueryParameters.named("sid" to schoolId)
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
                schoolId  = (inner["schoolId"] ?: inner["schoolId"]) as? String,
                groupId   = (inner["groupId"] ?: inner["groupeId"]) as? String ?: "",
                profilId  = inner["profilId"] as? String,
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
            groupId     = (doc["groupId"] ?: doc["groupeId"]) as? String ?: "",
            nom         = doc["nom"] as? String ?: "",
            permissions = parseProfilPermissions(doc),
            isDefault   = doc["isDefault"] as? Boolean ?: false,
            createdAt   = (doc["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }.getOrNull()

    suspend fun assignProfilToUser(userId: String, profilId: String, permissions: List<ProfilPermission>) {
        val existing = col("users").get(userId).contentAs<MutableMap<String, Any?>>()
        existing["profilId"] = profilId
        existing["permissions"] = permissions.map { p ->
            mapOf(
                "moduleSlug" to p.moduleSlug,
                "canRead"    to p.canRead,
                "canWrite"   to p.canWrite,
                "canDelete"  to p.canDelete,
                "canExport"  to p.canExport
            )
        }
        existing["updatedAt"] = now()
        col("users").upsert(userId, existing)
    }

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
        return ModuleResponse(id, req.code, req.nom, req.categorieCode, req.description, false, "gratuit", true)
    }

    suspend fun listModules(): List<ModuleResponse> {
        val result = scope.query("SELECT META().id, * FROM `modules` WHERE `type` = 'module'").execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d["modules"] as? Map<*, *> ?: d
            ModuleResponse(
                id           = d["id"] as? String ?: "",
                code         = inner["code"] as? String ?: "",
                nom          = inner["nom"] as? String ?: "",
                categorieCode = inner["categorieCode"] as? String ?: "",
                description  = inner["description"] as? String ?: "",
                isCore       = inner["isCore"] as? Boolean ?: false,
                requiredPlan = inner["requiredPlan"] as? String ?: "gratuit",
                isActive     = inner["isActive"] as? Boolean ?: true
            )
        }
    }

    // ── Dashboard Stats ─────────────────────────────────────────

    suspend fun countGroupes(): Long {
        val result = scope.query("SELECT COUNT(*) AS cnt FROM `${GROUPS_COLLECTION}` WHERE `type` IN ['school_group', 'groupe']").execute()
        return result.rows.firstOrNull()?.contentAs<Map<String, Any>>()?.let {
            (it["cnt"] as? Number)?.toLong() ?: 0L
        } ?: 0L
    }

    suspend fun countEcoles(): Long {
        val result = scope.query("SELECT COUNT(*) AS cnt FROM `schools` WHERE `type` = 'school'").execute()
        return result.rows.firstOrNull()?.contentAs<Map<String, Any>>()?.let {
            (it["cnt"] as? Number)?.toLong() ?: 0L
        } ?: 0L
    }

    suspend fun countUsers(): Long {
        val result = scope.query("SELECT COUNT(*) AS cnt FROM `users` WHERE `type` = 'user'").execute()
        return result.rows.firstOrNull()?.contentAs<Map<String, Any>>()?.let {
            (it["cnt"] as? Number)?.toLong() ?: 0L
        } ?: 0L
    }

    suspend fun countModules(): Long {
        val result = scope.query("SELECT COUNT(*) AS cnt FROM `modules` WHERE `type` = 'module'").execute()
        return result.rows.firstOrNull()?.contentAs<Map<String, Any>>()?.let {
            (it["cnt"] as? Number)?.toLong() ?: 0L
        } ?: 0L
    }

    // ── Plans ────────────────────────────────────────────────────

    suspend fun createPlan(req: CreatePlanRequest): PlanResponse {
        val id = newId("plan")
        val doc = mapOf(
            "type"               to "plan",
            "nom"                to req.nom,
            "prixXAF"            to req.prixXAF,
            "maxEcoles"          to req.maxEcoles,
            "maxUtilisateurs"    to req.maxUtilisateurs,
            "modulesIncluded"    to req.modulesIncluded,
            "categoriesIncluded" to req.categoriesIncluded,
            "dureeJours"         to req.dureeJours,
            "isActive"           to true,
            "createdAt"          to now(),
            "updatedAt"          to now()
        )
        col("plans").upsert(id, doc)
        return PlanResponse(id, req.nom, req.prixXAF, req.maxEcoles, req.maxUtilisateurs,
            req.modulesIncluded, req.categoriesIncluded, req.dureeJours, true)
    }

    suspend fun updatePlan(planId: String, req: UpdatePlanRequest): PlanResponse? {
        val existing = getPlanById(planId) ?: return null
        val doc = mapOf(
            "type"               to "plan",
            "nom"                to (req.nom ?: existing.nom),
            "prixXAF"            to (req.prixXAF ?: existing.prixXAF),
            "maxEcoles"          to (req.maxEcoles ?: existing.maxEcoles),
            "maxUtilisateurs"    to (req.maxUtilisateurs ?: existing.maxUtilisateurs),
            "modulesIncluded"    to (req.modulesIncluded ?: existing.modulesIncluded),
            "categoriesIncluded" to (req.categoriesIncluded ?: existing.categoriesIncluded),
            "dureeJours"         to (req.dureeJours ?: existing.dureeJours),
            "isActive"           to (req.isActive ?: existing.isActive),
            "updatedAt"          to now()
        )
        col("plans").upsert(planId, doc)
        return getPlanById(planId)
    }

    suspend fun listPlans(): List<PlanResponse> {
        val result = scope.query("SELECT META().id, * FROM `plans` WHERE `type` = 'plan'").execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d["plans"] as? Map<*, *> ?: d
            PlanResponse(
                id                  = d["id"] as? String ?: "",
                nom                 = inner["nom"] as? String ?: "",
                prixXAF             = (inner["prixXAF"] as? Number)?.toLong() ?: 0L,
                maxEcoles           = (inner["maxEcoles"] as? Number)?.toInt() ?: 0,
                maxUtilisateurs     = (inner["maxUtilisateurs"] as? Number)?.toInt() ?: 0,
                modulesIncluded     = @Suppress("UNCHECKED_CAST") (inner["modulesIncluded"] as? List<String> ?: emptyList()),
                categoriesIncluded  = @Suppress("UNCHECKED_CAST") (inner["categoriesIncluded"] as? List<String> ?: emptyList()),
                dureeJours          = (inner["dureeJours"] as? Number)?.toInt() ?: 365,
                isActive            = inner["isActive"] as? Boolean ?: true
            )
        }
    }

    // ── Catégories (CRUD dynamique) ─────────────────────────────

    suspend fun createCategorie(req: CreateCategorieRequest): CategorieInfo {
        val id = "cat::${req.code}"
        val doc = mapOf(
            "type"     to "categorie",
            "code"     to req.code,
            "nom"      to req.nom,
            "isCore"   to req.isCore,
            "ordre"    to req.ordre,
            "isActive" to true,
            "createdAt" to now(),
            "updatedAt" to now()
        )
        col("categories").upsert(id, doc)
        return CategorieInfo(req.code, req.nom, req.isCore, req.ordre, true)
    }

    suspend fun updateCategorie(code: String, req: UpdateCategorieRequest): CategorieInfo? {
        val id = "cat::$code"
        val existing = runCatching { col("categories").get(id).contentAs<Map<String, Any>>() }.getOrNull() ?: return null
        val doc = mapOf(
            "type"     to "categorie",
            "code"     to code,
            "nom"      to (req.nom ?: existing["nom"] as? String ?: ""),
            "isCore"   to (req.isCore ?: existing["isCore"] as? Boolean ?: false),
            "ordre"    to (req.ordre ?: (existing["ordre"] as? Number)?.toInt() ?: 0),
            "isActive" to (req.isActive ?: existing["isActive"] as? Boolean ?: true),
            "updatedAt" to now()
        )
        col("categories").upsert(id, doc)
        return CategorieInfo(code, doc["nom"] as String, doc["isCore"] as Boolean, doc["ordre"] as Int, doc["isActive"] as Boolean)
    }

    suspend fun listCategories(): List<CategorieInfo> {
        return runCatching {
            val result = scope.query("SELECT META().id, * FROM `categories` WHERE `type` = 'categorie' ORDER BY `ordre` ASC").execute()
            result.rows.map { row ->
                val d = row.contentAs<Map<String, Any>>()
                val inner = d["categories"] as? Map<*, *> ?: d
                CategorieInfo(
                    code     = inner["code"] as? String ?: "",
                    nom      = inner["nom"] as? String ?: "",
                    isCore   = inner["isCore"] as? Boolean ?: false,
                    ordre    = (inner["ordre"] as? Number)?.toInt() ?: 0,
                    isActive = inner["isActive"] as? Boolean ?: true
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun countCategories(): Long {
        return runCatching {
            val result = scope.query("SELECT COUNT(*) AS cnt FROM `categories` WHERE `type` = 'categorie'").execute()
            result.rows.firstOrNull()?.contentAs<Map<String, Any>>()?.let {
                (it["cnt"] as? Number)?.toLong() ?: 0L
            } ?: 0L
        }.getOrDefault(0L)
    }

    // ── Modules (Update/Delete) ───────────────────────────────

    suspend fun updateModule(moduleId: String, req: UpdateModuleRequest): ModuleResponse? {
        val existing = runCatching { col("modules").get(moduleId).contentAs<Map<String, Any>>() }.getOrNull() ?: return null
        val doc = mapOf(
            "type"          to "module",
            "code"          to (existing["code"] as? String ?: ""),
            "nom"           to (req.nom ?: existing["nom"] as? String ?: ""),
            "categorieCode" to (req.categorieCode ?: existing["categorieCode"] as? String ?: ""),
            "description"   to (req.description ?: existing["description"] as? String ?: ""),
            "isActive"      to (req.isActive ?: existing["isActive"] as? Boolean ?: true),
            "updatedAt"     to now()
        )
        col("modules").upsert(moduleId, doc)
        return ModuleResponse(
            id = moduleId,
            code = doc["code"] as String,
            nom = doc["nom"] as String,
            categorieCode = doc["categorieCode"] as String,
            description = doc["description"] as String,
            isCore = existing["isCore"] as? Boolean ?: false,
            requiredPlan = existing["requiredPlan"] as? String ?: "gratuit",
            isActive = doc["isActive"] as Boolean
        )
    }

    // ── Groupes (Update/Lifecycle) ─────────────────────────────

    suspend fun updateGroupe(groupeId: String, req: UpdateGroupeRequest): GroupeResponse? {
        val existing = runCatching { col(GROUPS_COLLECTION).get(groupeId).contentAs<Map<String, Any>>() }.getOrNull() ?: return null
        val doc = existing.toMutableMap().apply {
            req.nom?.let { put("nom", it.trim()) }
            req.email?.let { value -> value.trim().takeIf { it.isNotEmpty() }?.let { put("email", it) } ?: remove("email") }
            req.phone?.let { value -> value.trim().takeIf { it.isNotEmpty() }?.let { put("phone", it) } ?: remove("phone") }
            req.department?.let { value -> value.trim().takeIf { it.isNotEmpty() }?.let { put("department", it) } ?: remove("department") }
            req.city?.let { value -> value.trim().takeIf { it.isNotEmpty() }?.let { put("city", it) } ?: remove("city") }
            req.address?.let { value -> value.trim().takeIf { it.isNotEmpty() }?.let { put("address", it) } ?: remove("address") }
            req.logo?.let { value -> value.trim().takeIf { it.isNotEmpty() }?.let { put("logo", it) } ?: remove("logo") }
            req.description?.let { value -> value.trim().takeIf { it.isNotEmpty() }?.let { put("description", it) } ?: remove("description") }
            req.foundedYear?.let { put("foundedYear", it) }
            req.website?.let { value -> value.trim().takeIf { it.isNotEmpty() }?.let { put("website", it) } ?: remove("website") }
            req.planId?.let { put("planId", it) }
            req.isActive?.let { put("isActive", it) }
            put("updatedAt", now())
        }
        col(GROUPS_COLLECTION).upsert(groupeId, doc)
        return getGroupeById(groupeId)
    }

    suspend fun deleteGroupe(groupeId: String): Boolean = runCatching {
        col(GROUPS_COLLECTION).remove(groupeId)
        true
    }.getOrDefault(false)

    suspend fun countGroupesActifs(): Long {
        return runCatching {
            val result = scope.query("SELECT COUNT(*) AS cnt FROM `${GROUPS_COLLECTION}` WHERE `type` IN ['school_group', 'groupe'] AND (`isActive` = true OR `is_active` = true)").execute()
            result.rows.firstOrNull()?.contentAs<Map<String, Any>>()?.let {
                (it["cnt"] as? Number)?.toLong() ?: 0L
            } ?: 0L
        }.getOrDefault(0L)
    }

    // ── Abonnements ─────────────────────────────────────────

    suspend fun createSubscription(req: CreateSubscriptionRequest, plan: PlanResponse): SubscriptionResponse {
        val id = newId("sub")
        val debut = now()
        val fin = debut + (plan.dureeJours.toLong() * 86_400_000L)
        val doc = mapOf(
            "type"                to "subscription",
            "groupeId"            to req.groupeId,
            "planId"              to req.planId,
            "statut"              to "active",
            "dateDebut"           to debut,
            "dateFin"             to fin,
            "renouvellementAuto"  to req.renouvellementAuto,
            "createdAt"           to debut,
            "updatedAt"           to debut
        )
        col("subscriptions").upsert(id, doc)
        return SubscriptionResponse(id, req.groupeId, req.planId, "active", debut, fin, req.renouvellementAuto, debut)
    }

    suspend fun listSubscriptions(): List<SubscriptionResponse> {
        val result = scope.query("SELECT META().id, * FROM `subscriptions` WHERE `type` = 'subscription'").execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d["subscriptions"] as? Map<*, *> ?: d
            SubscriptionResponse(
                id                  = d["id"] as? String ?: "",
                groupeId            = inner["groupeId"] as? String ?: "",
                planId              = inner["planId"] as? String ?: "",
                statut              = inner["statut"] as? String ?: "active",
                dateDebut           = (inner["dateDebut"] as? Number)?.toLong() ?: 0L,
                dateFin             = (inner["dateFin"] as? Number)?.toLong() ?: 0L,
                renouvellementAuto  = inner["renouvellementAuto"] as? Boolean ?: false,
                createdAt           = (inner["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    suspend fun getSubscriptionById(id: String): SubscriptionResponse? = runCatching {
        val doc = col("subscriptions").get(id).contentAs<Map<String, Any>>()
        SubscriptionResponse(
            id                  = id,
            groupeId            = doc["groupeId"] as? String ?: "",
            planId              = doc["planId"] as? String ?: "",
            statut              = doc["statut"] as? String ?: "active",
            dateDebut           = (doc["dateDebut"] as? Number)?.toLong() ?: 0L,
            dateFin             = (doc["dateFin"] as? Number)?.toLong() ?: 0L,
            renouvellementAuto  = doc["renouvellementAuto"] as? Boolean ?: false,
            createdAt           = (doc["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }.getOrNull()

    suspend fun getActiveSubscriptionByGroupe(groupeId: String): SubscriptionResponse? {
        val result = scope.query(
            "SELECT META().id, * FROM `subscriptions` WHERE `type` = 'subscription' AND `groupeId` = \$gid AND `statut` = 'active' LIMIT 1",
            parameters = com.couchbase.client.kotlin.query.QueryParameters.named("gid" to groupeId)
        ).execute()
        return result.rows.firstOrNull()?.let { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d["subscriptions"] as? Map<*, *> ?: d
            SubscriptionResponse(
                id                  = d["id"] as? String ?: "",
                groupeId            = inner["groupeId"] as? String ?: "",
                planId              = inner["planId"] as? String ?: "",
                statut              = inner["statut"] as? String ?: "active",
                dateDebut           = (inner["dateDebut"] as? Number)?.toLong() ?: 0L,
                dateFin             = (inner["dateFin"] as? Number)?.toLong() ?: 0L,
                renouvellementAuto  = inner["renouvellementAuto"] as? Boolean ?: false,
                createdAt           = (inner["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    suspend fun updateSubscriptionStatus(id: String, statut: String): SubscriptionResponse? {
        val existing = runCatching { col("subscriptions").get(id).contentAs<MutableMap<String, Any?>>() }.getOrNull() ?: return null
        existing["statut"] = statut
        existing["updatedAt"] = now()
        col("subscriptions").upsert(id, existing)
        return getSubscriptionById(id)
    }

    // ── Factures Plateforme ───────────────────────────────────

    suspend fun createInvoice(req: CreateInvoiceRequest): InvoiceResponse {
        val id = newId("inv_plat")
        val ref = "INV-${System.currentTimeMillis().toString().takeLast(8)}"
        val doc = mapOf(
            "type"             to "invoice_platform",
            "groupeId"         to req.groupeId,
            "subscriptionId"   to req.subscriptionId,
            "montantXAF"       to req.montantXAF,
            "statut"           to "draft",
            "dateEmission"     to now(),
            "dateEcheance"     to req.dateEcheance,
            "datePaiement"     to null,
            "reference"        to ref,
            "notes"            to req.notes,
            "createdAt"        to now(),
            "updatedAt"        to now()
        )
        col("invoices_platform").upsert(id, doc)
        return InvoiceResponse(id, req.groupeId, req.subscriptionId, req.montantXAF, "draft", now(), req.dateEcheance, null, ref, req.notes)
    }

    suspend fun listInvoices(): List<InvoiceResponse> {
        val result = scope.query("SELECT META().id, * FROM `invoices_platform` WHERE `type` = 'invoice_platform'").execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d["invoices_platform"] as? Map<*, *> ?: d
            InvoiceResponse(
                id               = d["id"] as? String ?: "",
                groupeId         = inner["groupeId"] as? String ?: "",
                subscriptionId   = inner["subscriptionId"] as? String ?: "",
                montantXAF       = (inner["montantXAF"] as? Number)?.toLong() ?: 0L,
                statut           = inner["statut"] as? String ?: "draft",
                dateEmission     = (inner["dateEmission"] as? Number)?.toLong() ?: 0L,
                dateEcheance     = (inner["dateEcheance"] as? Number)?.toLong() ?: 0L,
                datePaiement     = (inner["datePaiement"] as? Number)?.toLong(),
                reference        = inner["reference"] as? String ?: "",
                notes            = inner["notes"] as? String ?: ""
            )
        }
    }

    suspend fun updateInvoiceStatus(id: String, statut: String, datePaiement: Long? = null): InvoiceResponse? {
        val existing = runCatching { col("invoices_platform").get(id).contentAs<MutableMap<String, Any?>>() }.getOrNull() ?: return null
        existing["statut"] = statut
        if (datePaiement != null) existing["datePaiement"] = datePaiement
        existing["updatedAt"] = now()
        col("invoices_platform").upsert(id, existing)
        val d = existing
        return InvoiceResponse(
            id = id,
            groupeId = d["groupeId"] as? String ?: "",
            subscriptionId = d["subscriptionId"] as? String ?: "",
            montantXAF = (d["montantXAF"] as? Number)?.toLong() ?: 0L,
            statut = statut,
            dateEmission = (d["dateEmission"] as? Number)?.toLong() ?: 0L,
            dateEcheance = (d["dateEcheance"] as? Number)?.toLong() ?: 0L,
            datePaiement = datePaiement ?: (d["datePaiement"] as? Number)?.toLong(),
            reference = d["reference"] as? String ?: "",
            notes = d["notes"] as? String ?: ""
        )
    }

    // ── Annonces Globales ─────────────────────────────────────

    suspend fun createAnnouncement(req: CreateAnnouncementRequest, createdBy: String): AnnouncementResponse {
        val id = newId("ann")
        val doc = mapOf(
            "type"      to "announcement_platform",
            "titre"     to req.titre,
            "contenu"   to req.contenu,
            "cible"     to req.cible,
            "createdBy" to createdBy,
            "createdAt" to now(),
            "updatedAt" to now()
        )
        col("announcements_platform").upsert(id, doc)
        return AnnouncementResponse(id, req.titre, req.contenu, req.cible, createdBy, now())
    }

    suspend fun listAnnouncements(): List<AnnouncementResponse> {
        val result = scope.query("SELECT META().id, * FROM `announcements_platform` WHERE `type` = 'announcement_platform' ORDER BY `createdAt` DESC").execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d["announcements_platform"] as? Map<*, *> ?: d
            AnnouncementResponse(
                id        = d["id"] as? String ?: "",
                titre     = inner["titre"] as? String ?: "",
                contenu   = inner["contenu"] as? String ?: "",
                cible     = inner["cible"] as? String ?: "all",
                createdBy = inner["createdBy"] as? String ?: "",
                createdAt = (inner["createdAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    suspend fun countSubscriptions(): Long {
        return runCatching {
            val result = scope.query("SELECT COUNT(*) AS cnt FROM `${SUBSCRIPTIONS_COLLECTION}` WHERE `type` = 'subscription'").execute()
            result.rows.firstOrNull()?.contentAs<Map<String, Any>>()?.let {
                (it["cnt"] as? Number)?.toLong() ?: 0L
            } ?: 0L
        }.getOrDefault(0L)
    }

    suspend fun countActiveSubscriptions(): Long {
        return runCatching {
            val result = scope.query("SELECT COUNT(*) AS cnt FROM `${SUBSCRIPTIONS_COLLECTION}` WHERE `type` = 'subscription' AND `statut` = 'active'").execute()
            result.rows.firstOrNull()?.contentAs<Map<String, Any>>()?.let {
                (it["cnt"] as? Number)?.toLong() ?: 0L
            } ?: 0L
        }.getOrDefault(0L)
    }

    // ── Dashboard Analytics ────────────────────────────────────

    suspend fun groupesByProvince(): List<ProvinceStats> {
        val groupes = listGroupes()
        val ecoles = scope.query("SELECT `groupId`, `groupeId`, `province` FROM `schools` WHERE `type` = 'school'").execute()
        val ecolesByGroupe = mutableMapOf<String, Int>()
        ecoles.rows.forEach { row ->
            val d = row.contentAs<Map<String, Any>>()
            val gid = d["groupId"] as? String ?: d["groupeId"] as? String ?: ""
            if (gid.isNotEmpty()) ecolesByGroupe[gid] = (ecolesByGroupe[gid] ?: 0) + 1
        }
        return groupes.groupBy { (it.department ?: "Non renseigné").ifBlank { "Non renseigné" } }.map { (prov, gs) ->
            ProvinceStats(
                province = prov,
                groupesCount = gs.size.toLong(),
                ecolesCount = gs.sumOf { (ecolesByGroupe[it.id] ?: 0).toLong() }
            )
        }.sortedByDescending { it.groupesCount }
    }

    suspend fun planDistribution(): List<PlanDistribution> {
        val groupes = listGroupes()
        val plans = listPlans().associateBy { it.id }
        return groupes.groupBy { it.planId }.map { (planId, gs) ->
            PlanDistribution(
                planId = planId,
                planNom = plans[planId]?.nom ?: planId,
                groupesCount = gs.size.toLong()
            )
        }.sortedByDescending { it.groupesCount }
    }

    suspend fun subscriptionsByStatus(): Map<String, Long> {
        return runCatching {
            val result = scope.query(
                "SELECT `statut`, COUNT(*) AS cnt FROM `${SUBSCRIPTIONS_COLLECTION}` WHERE `type` = 'subscription' GROUP BY `statut`"
            ).execute()
            result.rows.associate { row ->
                val d = row.contentAs<Map<String, Any>>()
                (d["statut"] as? String ?: "unknown") to ((d["cnt"] as? Number)?.toLong() ?: 0L)
            }
        }.getOrDefault(emptyMap())
    }

    suspend fun countInvoices(): Long {
        val result = scope.query("SELECT COUNT(*) AS cnt FROM `${INVOICES_COLLECTION}` WHERE (`type` = 'invoice_platform' OR `type` = 'invoice')").execute()
        return result.rows.firstOrNull()?.contentAs<Map<String, Any>>()?.let {
            (it["cnt"] as? Number)?.toLong() ?: 0L
        } ?: 0L
    }

    suspend fun revenueTotal(): Long {
        val result = scope.query("SELECT IFNULL(SUM(`montantXAF`), 0) AS total FROM `${INVOICES_COLLECTION}` WHERE (`type` = 'invoice_platform' OR `type` = 'invoice')").execute()
        return result.rows.firstOrNull()?.contentAs<Map<String, Any>>()?.let {
            (it["total"] as? Number)?.toLong() ?: 0L
        } ?: 0L
    }

    suspend fun revenuePaid(): Long {
        val result = scope.query("SELECT IFNULL(SUM(`montantXAF`), 0) AS total FROM `${INVOICES_COLLECTION}` WHERE (`type` = 'invoice_platform' OR `type` = 'invoice') AND `statut` = 'paid'").execute()
        return result.rows.firstOrNull()?.contentAs<Map<String, Any>>()?.let {
            (it["total"] as? Number)?.toLong() ?: 0L
        } ?: 0L
    }

    suspend fun countInvoicesOverdue(): Long {
        val result = scope.query(
            "SELECT COUNT(*) AS cnt FROM `${INVOICES_COLLECTION}` WHERE (`type` = 'invoice_platform' OR `type` = 'invoice') AND `statut` = 'overdue'"
        ).execute()
        return result.rows.firstOrNull()?.contentAs<Map<String, Any>>()?.let {
            (it["cnt"] as? Number)?.toLong() ?: 0L
        } ?: 0L
    }

    suspend fun recentGroupes(limit: Int = 5): List<GroupeResponse> {
        val result = scope.query("SELECT META().id, * FROM `${GROUPS_COLLECTION}` WHERE `type` IN ['school_group', 'groupe'] ORDER BY `createdAt` DESC LIMIT $limit").execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d[GROUPS_COLLECTION] as? Map<*, *> ?: d[LEGACY_GROUPS_COLLECTION] as? Map<*, *> ?: d
            mapToGroupeResponse(d["id"] as? String ?: "", inner)
        }
    }

    suspend fun recentInvoices(limit: Int = 5): List<InvoiceResponse> {
        val result = scope.query("SELECT META().id, * FROM `${INVOICES_COLLECTION}` WHERE (`type` = 'invoice_platform' OR `type` = 'invoice') ORDER BY `createdAt` DESC LIMIT $limit").execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d[INVOICES_COLLECTION] as? Map<*, *> ?: d[LEGACY_INVOICES_COLLECTION] as? Map<*, *> ?: d
            InvoiceResponse(
                id               = d["id"] as? String ?: "",
                groupeId         = inner["groupeId"] as? String ?: "",
                subscriptionId   = inner["subscriptionId"] as? String ?: "",
                montantXAF       = (inner["montantXAF"] as? Number)?.toLong() ?: 0L,
                statut           = inner["statut"] as? String ?: "draft",
                dateEmission     = (inner["dateEmission"] as? Number)?.toLong() ?: 0L,
                dateEcheance     = (inner["dateEcheance"] as? Number)?.toLong() ?: 0L,
                datePaiement     = (inner["datePaiement"] as? Number)?.toLong(),
                reference        = inner["reference"] as? String ?: "",
                notes            = inner["notes"] as? String ?: ""
            )
        }
    }

    // ── Admin Users (Super Admin scope) ──────────────────────────

    private fun mapToAdminUserResponse(id: String, inner: Map<*, *>): AdminUserResponse {
        val ts = fun(field: String): Long? = (inner[field] as? Number)?.toLong()
        return AdminUserResponse(
            id                = id,
            username          = inner["username"] as? String ?: "",
            firstName         = inner["prenom"] as? String ?: inner["firstName"] as? String ?: "",
            lastName          = inner["nom"] as? String ?: inner["lastName"] as? String ?: "",
            email             = inner["email"] as? String ?: "",
            phone             = inner["phone"] as? String,
            role              = inner["role"] as? String ?: "USER",
            status            = inner["status"] as? String ?: if (inner["isActive"] as? Boolean != false) "active" else "suspended",
            gender            = inner["gender"] as? String,
            dateOfBirth       = inner["dateOfBirth"] as? String ?: inner["date_of_birth"] as? String,
            groupId           = inner["groupId"] as? String ?: inner["groupeId"] as? String,
            schoolId          = inner["schoolId"] as? String ?: inner["ecoleId"] as? String,
            avatar            = inner["avatar"] as? String,
            address           = inner["address"] as? String,
            birthPlace        = inner["birthPlace"] as? String ?: inner["birth_place"] as? String,
            mustChangePassword = inner["mustChangePassword"] as? Boolean ?: inner["must_change_password"] as? Boolean ?: false,
            lastLoginAt       = ts("lastLoginAt") ?: ts("last_login_at"),
            loginAttempts     = (inner["loginAttempts"] as? Number)?.toInt() ?: (inner["login_attempts"] as? Number)?.toInt() ?: 0,
            isActive          = inner["isActive"] as? Boolean ?: true,
            createdAt         = ts("createdAt") ?: 0L,
            updatedAt         = ts("updatedAt") ?: ts("updated_at") ?: 0L
        )
    }

    suspend fun listAllAdmins(): List<AdminUserResponse> {
        val result = scope.query(
            "SELECT META().id, * FROM `users` WHERE `type` = 'user' AND `role` IN ['ADMIN_GROUPE', 'SUPER_ADMIN', 'admin_groupe', 'super_admin']"
        ).execute()
        return result.rows.map { row ->
            val d = row.contentAs<Map<String, Any>>()
            val inner = d["users"] as? Map<*, *> ?: d
            mapToAdminUserResponse(d["id"] as? String ?: "", inner)
        }
    }

    suspend fun createAdmin(req: CreateAdminRequest, passwordHash: String): AdminUserResponse {
        val id = newId("user")
        val username = req.email.substringBefore("@").ifBlank { "admin_${System.currentTimeMillis()}" }
        val roleNorm = req.role.uppercase()
        val doc = mutableMapOf<String, Any?>(
            "type"               to "user",
            "username"           to username,
            "passwordHash"       to passwordHash,
            "nom"                to req.nom,
            "prenom"             to req.prenom,
            "email"              to req.email,
            "role"               to roleNorm,
            "isActive"           to true,
            "status"             to "active",
            "mustChangePassword" to req.mustChangePassword,
            "loginAttempts"      to 0,
            "createdAt"          to now(),
            "updatedAt"          to now()
        )
        req.phone?.let { doc["phone"] = it }
        req.gender?.let { doc["gender"] = it }
        req.dateOfBirth?.let { doc["dateOfBirth"] = it }
        req.address?.let { doc["address"] = it }
        req.birthPlace?.let { doc["birthPlace"] = it }
        req.avatar?.let { doc["avatar"] = it }
        if (roleNorm == "ADMIN_GROUPE" && req.groupId != null) {
            doc["groupId"] = req.groupId
        }
        col("users").upsert(id, doc)
        return AdminUserResponse(
            id = id, username = username, firstName = req.prenom, lastName = req.nom,
            email = req.email, phone = req.phone, role = roleNorm, status = "active",
            gender = req.gender, dateOfBirth = req.dateOfBirth, groupId = req.groupId,
            schoolId = null, avatar = req.avatar, address = req.address, birthPlace = req.birthPlace,
            mustChangePassword = req.mustChangePassword, lastLoginAt = null, loginAttempts = 0,
            isActive = true, createdAt = now(), updatedAt = now()
        )
    }

    suspend fun updateAdmin(userId: String, req: UpdateAdminRequest): AdminUserResponse? {
        val existing = runCatching { col("users").get(userId).contentAs<MutableMap<String, Any?>>() }.getOrNull() ?: return null
        req.nom?.let { existing["nom"] = it }
        req.prenom?.let { existing["prenom"] = it }
        req.email?.let { existing["email"] = it }
        req.phone?.let { existing["phone"] = it }
        req.gender?.let { existing["gender"] = it }
        req.dateOfBirth?.let { existing["dateOfBirth"] = it }
        req.address?.let { existing["address"] = it }
        req.birthPlace?.let { existing["birthPlace"] = it }
        req.avatar?.let { existing["avatar"] = it }
        req.mustChangePassword?.let { existing["mustChangePassword"] = it }
        existing["updatedAt"] = now()
        col("users").upsert(userId, existing)
        val inner = existing
        return mapToAdminUserResponse(userId, inner)
    }

    suspend fun deleteAdmin(userId: String): Boolean {
        return runCatching { col("users").remove(userId) }.isSuccess
    }

    suspend fun toggleAdminStatus(userId: String, newStatus: String): AdminUserResponse? {
        val existing = runCatching { col("users").get(userId).contentAs<MutableMap<String, Any?>>() }.getOrNull() ?: return null
        existing["status"] = newStatus
        existing["isActive"] = newStatus == "active"
        existing["updatedAt"] = now()
        col("users").upsert(userId, existing)
        return mapToAdminUserResponse(userId, existing)
    }
}
