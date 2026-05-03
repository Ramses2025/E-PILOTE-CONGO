package cg.epilote.backend.admin

import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Repository
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Repository
class AdminModuleRepository(private val bucket: Bucket) {
    private val scope = runBlocking { bucket.defaultScope() }

    private companion object {
        const val CONFIG_COLLECTION = "config"
        const val MODULES_DOC_ID = "config::modules"
    }

    private val collections = java.util.concurrent.ConcurrentHashMap<String, Collection>()
    private fun col(name: String): Collection = collections.getOrPut(name) { runBlocking { scope.collection(name) } }

    private fun now(): String = Instant.now().toString()

    private fun parseModule(raw: Map<*, *>): ModuleResponse {
        val slug = raw["slug"] as? String ?: raw["code"] as? String ?: ""
        return ModuleResponse(
            id = "module::$slug",
            code = slug,
            nom = raw["nom"] as? String ?: "",
            categorieCode = raw["categorieCode"] as? String ?: "",
            description = raw["description"] as? String ?: "",
            isCore = raw["isCore"] as? Boolean ?: false,
            requiredPlan = raw["requiredPlan"] as? String ?: "gratuit",
            isActive = raw["isActive"] as? Boolean ?: true,
            ordre = (raw["ordre"] as? Number)?.toInt() ?: 0
        )
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun readDocument(): MutableMap<String, Any?> =
        runCatching {
            col(CONFIG_COLLECTION).get(MODULES_DOC_ID).contentAs<MutableMap<String, Any?>>()
        }.getOrElse {
            mutableMapOf(
                "id" to MODULES_DOC_ID,
                "type" to "config_modules",
                "_schemaVersion" to 2,
                "modules" to emptyList<Map<String, Any?>>(),
                "updatedAt" to now()
            )
        }

    @Suppress("UNCHECKED_CAST")
    private fun readMutableModules(document: Map<String, Any?>): MutableList<MutableMap<String, Any?>> =
        ((document["modules"] as? List<*>) ?: emptyList<Any>())
            .mapNotNull { item ->
                (item as? Map<*, *>)?.entries?.associate { entry ->
                    entry.key.toString() to entry.value
                }?.toMutableMap()
            }
            .toMutableList()

    private suspend fun writeDocument(document: MutableMap<String, Any?>) {
        document["id"] = MODULES_DOC_ID
        document["type"] = document["type"] ?: "config_modules"
        document["_schemaVersion"] = document["_schemaVersion"] ?: 2
        document["updatedAt"] = now()
        col(CONFIG_COLLECTION).upsert(MODULES_DOC_ID, document)
    }

    suspend fun listModules(): List<ModuleResponse> =
        readMutableModules(readDocument())
            .map(::parseModule)
            .sortedWith(compareBy<ModuleResponse> { it.ordre }.thenBy { it.nom.lowercase() })

    suspend fun createModule(req: CreateModuleRequest): ModuleResponse {
        val document = readDocument()
        val modules = readMutableModules(document)
        val existing = modules.any { module ->
            val slug = module["slug"] as? String ?: module["code"] as? String
            slug?.equals(req.code, ignoreCase = true) == true
        }
        if (existing) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Le module ${req.code} existe déjà.")
        }

        val module = mutableMapOf<String, Any?>(
            "slug" to req.code,
            "nom" to req.nom,
            "categorieCode" to req.categorieCode,
            "description" to req.description,
            "requiredPlan" to req.requiredPlan,
            "isCore" to req.isCore,
            "ordre" to req.ordre,
            "isActive" to true
        )
        modules += module
        document["modules"] = modules.sortedBy { (it["ordre"] as? Number)?.toInt() ?: 0 }
        writeDocument(document)
        return parseModule(module)
    }

    suspend fun updateModule(moduleId: String, req: UpdateModuleRequest): ModuleResponse? {
        val document = readDocument()
        val modules = readMutableModules(document)
        val index = modules.indexOfFirst { module ->
            val slug = module["slug"] as? String ?: module["code"] as? String
            slug?.equals(moduleId, ignoreCase = true) == true
        }
        if (index < 0) return null

        val current = modules[index]
        current["nom"] = req.nom ?: current["nom"] as? String ?: ""
        current["categorieCode"] = req.categorieCode ?: current["categorieCode"] as? String ?: ""
        current["description"] = req.description ?: current["description"] as? String ?: ""
        current["requiredPlan"] = req.requiredPlan ?: current["requiredPlan"] as? String ?: "gratuit"
        current["isCore"] = req.isCore ?: current["isCore"] as? Boolean ?: false
        current["ordre"] = req.ordre ?: (current["ordre"] as? Number)?.toInt() ?: 0
        current["isActive"] = req.isActive ?: current["isActive"] as? Boolean ?: true
        modules[index] = current

        document["modules"] = modules.sortedBy { (it["ordre"] as? Number)?.toInt() ?: 0 }
        writeDocument(document)
        return parseModule(current)
    }

    suspend fun countModules(): Long = listModules().size.toLong()
}
