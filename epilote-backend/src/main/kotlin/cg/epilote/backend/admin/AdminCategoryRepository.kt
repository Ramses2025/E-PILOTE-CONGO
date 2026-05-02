package cg.epilote.backend.admin

import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Repository
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Repository
class AdminCategoryRepository(private val bucket: Bucket) {
    private val scope = runBlocking { bucket.defaultScope() }

    private companion object {
        const val CONFIG_COLLECTION = "config"
        const val CATEGORIES_DOC_ID = "config::categories"
    }

    private val collections = mutableMapOf<String, Collection>()
    private fun col(name: String): Collection = collections.getOrPut(name) { runBlocking { scope.collection(name) } }

    private fun now(): String = Instant.now().toString()

    private fun parseCategory(raw: Map<*, *>): CategorieInfo = CategorieInfo(
        code = raw["code"] as? String ?: "",
        nom = raw["nom"] as? String ?: "",
        isCore = raw["isCore"] as? Boolean ?: false,
        ordre = (raw["ordre"] as? Number)?.toInt() ?: 0,
        isActive = raw["isActive"] as? Boolean ?: true
    )

    @Suppress("UNCHECKED_CAST")
    private suspend fun readDocument(): MutableMap<String, Any?> =
        runCatching {
            col(CONFIG_COLLECTION).get(CATEGORIES_DOC_ID).contentAs<MutableMap<String, Any?>>()
        }.getOrElse {
            mutableMapOf(
                "id" to CATEGORIES_DOC_ID,
                "type" to "config_categories",
                "_schemaVersion" to 1,
                "categories" to emptyList<Map<String, Any?>>(),
                "updatedAt" to now()
            )
        }

    @Suppress("UNCHECKED_CAST")
    private fun readMutableCategories(document: Map<String, Any?>): MutableList<MutableMap<String, Any?>> =
        ((document["categories"] as? List<*>) ?: emptyList<Any>())
            .mapNotNull { item ->
                (item as? Map<*, *>)?.entries?.associate { entry ->
                    entry.key.toString() to entry.value
                }?.toMutableMap()
            }
            .toMutableList()

    private suspend fun writeDocument(document: MutableMap<String, Any?>) {
        document["id"] = CATEGORIES_DOC_ID
        document["type"] = document["type"] ?: "config_categories"
        document["_schemaVersion"] = document["_schemaVersion"] ?: 1
        document["updatedAt"] = now()
        col(CONFIG_COLLECTION).upsert(CATEGORIES_DOC_ID, document)
    }

    suspend fun listCategories(): List<CategorieInfo> =
        readMutableCategories(readDocument())
            .map(::parseCategory)
            .sortedBy { it.ordre }

    suspend fun createCategorie(req: CreateCategorieRequest): CategorieInfo {
        val document = readDocument()
        val categories = readMutableCategories(document)
        val existing = categories.any { (it["code"] as? String)?.equals(req.code, ignoreCase = true) == true }
        if (existing) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "La catégorie ${req.code} existe déjà.")
        }

        val category = mutableMapOf<String, Any?>(
            "code" to req.code,
            "nom" to req.nom,
            "isCore" to req.isCore,
            "ordre" to req.ordre,
            "isActive" to true
        )
        categories += category
        document["categories"] = categories.sortedBy { (it["ordre"] as? Number)?.toInt() ?: 0 }
        writeDocument(document)
        return parseCategory(category)
    }

    suspend fun updateCategorie(code: String, req: UpdateCategorieRequest): CategorieInfo? {
        val document = readDocument()
        val categories = readMutableCategories(document)
        val index = categories.indexOfFirst { (it["code"] as? String)?.equals(code, ignoreCase = true) == true }
        if (index < 0) return null

        val current = categories[index]
        current["nom"] = req.nom ?: current["nom"] as? String ?: ""
        current["isCore"] = req.isCore ?: current["isCore"] as? Boolean ?: false
        current["ordre"] = req.ordre ?: (current["ordre"] as? Number)?.toInt() ?: 0
        current["isActive"] = req.isActive ?: current["isActive"] as? Boolean ?: true
        categories[index] = current

        document["categories"] = categories.sortedBy { (it["ordre"] as? Number)?.toInt() ?: 0 }
        writeDocument(document)
        return parseCategory(current)
    }

    suspend fun countCategories(): Long = listCategories().size.toLong()
}
