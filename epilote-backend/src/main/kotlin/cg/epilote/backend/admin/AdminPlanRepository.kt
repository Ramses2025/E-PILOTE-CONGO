package cg.epilote.backend.admin

import com.couchbase.client.kotlin.Bucket
import com.couchbase.client.kotlin.Collection
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Repository
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Repository
class AdminPlanRepository(private val bucket: Bucket) {
    private val scope = runBlocking { bucket.defaultScope() }

    private companion object {
        const val CONFIG_COLLECTION = "config"
        const val PLANS_DOC_ID = "config::plans"
    }

    private val collections = java.util.concurrent.ConcurrentHashMap<String, Collection>()
    private fun col(name: String): Collection = collections.getOrPut(name) { runBlocking { scope.collection(name) } }

    private fun now(): String = Instant.now().toString()

    private fun builtInPlans(): List<Map<String, Any?>> = listOf(
        mapOf(
            "id" to "plan::gratuit",
            "name" to "Gratuit",
            "type" to "gratuit",
            "price" to 0L,
            "maxStudents" to 100,
            "maxPersonnel" to 10,
            "modulesIncluded" to listOf(
                "inscriptions", "eleves", "classes", "notes", "matieres",
                "presences-eleves", "annonces", "notifications"
            ),
            "isActive" to true
        ),
        mapOf(
            "id" to "plan::premium",
            "name" to "Premium",
            "type" to "premium",
            "price" to 150_000L,
            "maxStudents" to 2_000,
            "maxPersonnel" to 200,
            "modulesIncluded" to listOf(
                "inscriptions", "eleves", "classes", "notes", "matieres", "presences-eleves",
                "annonces", "notifications", "bulletins", "emploi-du-temps", "cahier-textes",
                "evaluations", "transferts", "documents", "finances", "facturation", "depenses",
                "personnel", "presences-personnel", "conges", "discipline", "bibliotheque",
                "cantine", "infirmerie", "messagerie", "evenements"
            ),
            "isActive" to true
        ),
        mapOf(
            "id" to "plan::pro",
            "name" to "Pro",
            "type" to "pro",
            "price" to 350_000L,
            "maxStudents" to 10_000,
            "maxPersonnel" to 1_000,
            "modulesIncluded" to listOf(
                "inscriptions", "eleves", "classes", "notes", "matieres", "presences-eleves",
                "annonces", "notifications", "bulletins", "emploi-du-temps", "cahier-textes",
                "evaluations", "transferts", "documents", "finances", "facturation", "depenses",
                "personnel", "presences-personnel", "conges", "discipline", "bibliotheque",
                "cantine", "infirmerie", "messagerie", "evenements", "conseils", "budget",
                "comptabilite", "paie"
            ),
            "isActive" to true
        ),
        mapOf(
            "id" to "plan::institutionnel",
            "name" to "Institutionnel",
            "type" to "institutionnel",
            "price" to 900_000L,
            "maxStudents" to 50_000,
            "maxPersonnel" to 5_000,
            "modulesIncluded" to listOf(
                "inscriptions", "eleves", "classes", "notes", "matieres", "presences-eleves",
                "annonces", "notifications", "bulletins", "emploi-du-temps", "cahier-textes",
                "evaluations", "transferts", "documents", "finances", "facturation", "depenses",
                "personnel", "presences-personnel", "conges", "discipline", "bibliotheque",
                "cantine", "infirmerie", "messagerie", "evenements", "conseils", "budget",
                "comptabilite", "paie"
            ),
            "isActive" to true
        )
    )

    @Suppress("UNCHECKED_CAST")
    private suspend fun readDocument(): MutableMap<String, Any?> =
        runCatching {
            col(CONFIG_COLLECTION).get(PLANS_DOC_ID).contentAs<MutableMap<String, Any?>>()
        }.getOrElse {
            mutableMapOf(
                "id" to PLANS_DOC_ID,
                "type" to "config_plans",
                "_schemaVersion" to 2,
                "currency" to "XAF",
                "plans" to emptyList<Map<String, Any?>>(),
                "updatedAt" to now()
            )
        }

    @Suppress("UNCHECKED_CAST")
    private fun readMutablePlans(document: Map<String, Any?>): MutableList<MutableMap<String, Any?>> =
        ((document["plans"] as? List<*>) ?: emptyList<Any>())
            .mapNotNull { item ->
                (item as? Map<*, *>)?.entries?.associate { entry ->
                    entry.key.toString() to entry.value
                }?.toMutableMap()
            }
            .toMutableList()

    private fun mergeRequiredPlans(document: MutableMap<String, Any?>): Boolean {
        val plans = readMutablePlans(document)
        val existingKeys = plans.map { (it["id"] as? String ?: it["type"] as? String ?: "").lowercase() }.toMutableSet()
        var changed = false
        builtInPlans().forEach { basePlan ->
            val id = (basePlan["id"] as? String ?: "").lowercase()
            val type = (basePlan["type"] as? String ?: "").lowercase()
            if (id !in existingKeys && type !in existingKeys) {
                plans += basePlan.toMutableMap()
                existingKeys += id
                existingKeys += type
                changed = true
            }
        }
        if (changed) {
            document["plans"] = plans.sortedBy { ((it["price"] as? Number)?.toLong() ?: 0L) }
        }
        return changed
    }

    private suspend fun writeDocument(document: MutableMap<String, Any?>) {
        document["id"] = PLANS_DOC_ID
        document["type"] = document["type"] ?: "config_plans"
        document["_schemaVersion"] = document["_schemaVersion"] ?: 2
        document["currency"] = document["currency"] ?: "XAF"
        document["updatedAt"] = now()
        col(CONFIG_COLLECTION).upsert(PLANS_DOC_ID, document)
    }

    private fun parsePlan(raw: Map<*, *>, currency: String): PlanResponse {
        val planId = raw["id"] as? String ?: ""
        return PlanResponse(
            id = planId,
            nom = raw["name"] as? String ?: raw["nom"] as? String ?: "",
            type = raw["type"] as? String ?: planId.substringAfterLast("::", "gratuit"),
            prixXAF = (raw["price"] as? Number)?.toLong() ?: (raw["prixXAF"] as? Number)?.toLong() ?: 0L,
            currency = currency,
            maxStudents = (raw["maxStudents"] as? Number)?.toInt() ?: 0,
            maxPersonnel = (raw["maxPersonnel"] as? Number)?.toInt() ?: 0,
            modulesIncluded = (raw["modulesIncluded"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            isActive = raw["isActive"] as? Boolean ?: true
        )
    }

    suspend fun getPlanById(planId: String): PlanResponse? {
        val document = readDocument()
        if (mergeRequiredPlans(document)) {
            writeDocument(document)
        }
        val currency = document["currency"] as? String ?: "XAF"
        val raw = readMutablePlans(document).firstOrNull { plan ->
            (plan["id"] as? String)?.equals(planId, ignoreCase = true) == true
        } ?: return null
        return parsePlan(raw, currency)
    }

    suspend fun listPlans(): List<PlanResponse> {
        val document = readDocument()
        if (mergeRequiredPlans(document)) {
            writeDocument(document)
        }
        val currency = document["currency"] as? String ?: "XAF"
        return readMutablePlans(document)
            .map { parsePlan(it, currency) }
            .sortedWith(compareBy<PlanResponse> { it.prixXAF }.thenBy { it.nom.lowercase() })
    }

    suspend fun createPlan(req: CreatePlanRequest): PlanResponse {
        val document = readDocument()
        mergeRequiredPlans(document)
        val plans = readMutablePlans(document)
        val normalizedType = req.type.trim().lowercase()
        val id = "plan::$normalizedType"
        val existing = plans.any { plan ->
            (plan["id"] as? String)?.equals(id, ignoreCase = true) == true ||
                (plan["type"] as? String)?.equals(normalizedType, ignoreCase = true) == true
        }
        if (existing) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Le plan ${req.nom} existe déjà.")
        }

        val plan = mutableMapOf<String, Any?>(
            "id" to id,
            "name" to req.nom,
            "type" to normalizedType,
            "price" to req.prixXAF,
            "maxStudents" to req.maxStudents,
            "maxPersonnel" to req.maxPersonnel,
            "modulesIncluded" to req.modulesIncluded,
            "isActive" to req.isActive
        )
        plans += plan
        document["plans"] = plans.sortedBy { ((it["price"] as? Number)?.toLong() ?: 0L) }
        writeDocument(document)
        return parsePlan(plan, document["currency"] as? String ?: "XAF")
    }

    suspend fun updatePlan(planId: String, req: UpdatePlanRequest): PlanResponse? {
        val document = readDocument()
        if (mergeRequiredPlans(document)) {
            writeDocument(document)
        }
        val plans = readMutablePlans(document)
        val index = plans.indexOfFirst { plan ->
            (plan["id"] as? String)?.equals(planId, ignoreCase = true) == true
        }
        if (index < 0) return null

        val current = plans[index]
        current["name"] = req.nom ?: current["name"] as? String ?: current["nom"] as? String ?: ""
        current["type"] = req.type ?: current["type"] as? String ?: planId.substringAfterLast("::", "gratuit")
        current["price"] = req.prixXAF ?: (current["price"] as? Number)?.toLong() ?: (current["prixXAF"] as? Number)?.toLong() ?: 0L
        current["maxStudents"] = req.maxStudents ?: (current["maxStudents"] as? Number)?.toInt() ?: 0
        current["maxPersonnel"] = req.maxPersonnel ?: (current["maxPersonnel"] as? Number)?.toInt() ?: 0
        current["modulesIncluded"] = req.modulesIncluded ?: (current["modulesIncluded"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList<String>()
        current["isActive"] = req.isActive ?: current["isActive"] as? Boolean ?: true
        plans[index] = current

        document["plans"] = plans.sortedBy { ((it["price"] as? Number)?.toLong() ?: 0L) }
        writeDocument(document)
        return parsePlan(current, document["currency"] as? String ?: "XAF")
    }
}
