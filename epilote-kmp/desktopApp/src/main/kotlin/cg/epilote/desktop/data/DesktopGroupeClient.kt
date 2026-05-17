package cg.epilote.desktop.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.util.logging.Logger

class DesktopGroupeClient(
    private val baseUrl: String,
    private val tokenProvider: () -> String?,
    private val onUnauthorized: suspend () -> Boolean = { false }
) {
    private val log = Logger.getLogger(DesktopGroupeClient::class.java.name)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val httpClient = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis  = 30_000
        }
    }

    private fun shouldAttemptRefresh(status: HttpStatusCode) =
        status == HttpStatusCode.Unauthorized

    private suspend inline fun <reified T> execute(
        method: String,
        path: String,
        crossinline request: suspend () -> HttpResponse
    ): T? {
        val first = runCatching { request() }.getOrElse { e ->
            log.warning("GroupeClient $method $path failed: ${e.message}")
            return null
        }
        if (shouldAttemptRefresh(first.status)) {
            val refreshed = runCatching { onUnauthorized() }.getOrDefault(false)
            if (refreshed) {
                val retry = runCatching { request() }.getOrElse { return null }
                if (retry.status.value in 200..299) {
                    return runCatching { retry.body<T>() }.getOrNull()
                }
            }
            return null
        }
        if (first.status.value !in 200..299) {
            val body = runCatching { first.bodyAsText() }.getOrNull().orEmpty().take(300)
            log.warning("GroupeClient $method $path returned ${first.status} — $body")
            return null
        }
        return runCatching { first.body<T>() }.getOrElse { e ->
            log.warning("GroupeClient $method $path decode error: ${e.message}")
            null
        }
    }

    private suspend fun executeStatus(
        method: String,
        path: String,
        request: suspend () -> HttpResponse
    ): Boolean {
        val first = runCatching { request() }.getOrElse { e ->
            log.warning("GroupeClient $method $path failed: ${e.message}")
            return false
        }
        if (shouldAttemptRefresh(first.status)) {
            val refreshed = runCatching { onUnauthorized() }.getOrDefault(false)
            if (refreshed) {
                val retry = runCatching { request() }.getOrElse { return false }
                return retry.status.value in 200..299
            }
            return false
        }
        if (first.status.value !in 200..299) {
            log.warning("GroupeClient $method $path returned ${first.status}")
        }
        return first.status.value in 200..299
    }

    private suspend inline fun <reified T> get(path: String): T? =
        execute("GET", path) {
            httpClient.get("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
        }

    private suspend inline fun <reified T, reified B : Any> post(path: String, body: B): T? =
        execute("POST", path) {
            httpClient.post("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(body)
            }
        }

    private suspend inline fun <reified B : Any> postStatus(path: String, body: B): Boolean =
        executeStatus("POST", path) {
            httpClient.post("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(body)
            }
        }

    // ── Dashboard Stats ───────────────────────────────────────────

    suspend fun getGroupeDashboardStats(groupeId: String): GroupeDashboardStatsDto? =
        get("/api/groupes/$groupeId/dashboard-stats")

    // ── Subscription Request ──────────────────────────────────────

    suspend fun createSubscriptionRequest(groupeId: String, dto: SubscriptionRequestDto): Boolean =
        postStatus("/api/groupes/$groupeId/subscription-request", dto)

    // ── Écoles ────────────────────────────────────────────────────

    suspend fun listEcoles(groupeId: String): List<EcoleApiDto>? =
        get("/api/groupes/$groupeId/ecoles")

    suspend fun createEcole(groupeId: String, dto: CreateEcoleDto): EcoleApiDto? =
        post("/api/groupes/$groupeId/ecoles", dto)

    // ── Profils ───────────────────────────────────────────────────

    suspend fun listProfils(groupeId: String): List<ProfilApiDto>? =
        get("/api/groupes/$groupeId/profils")

    suspend fun createProfil(groupeId: String, dto: CreateProfilDto): ProfilApiDto? =
        post("/api/groupes/$groupeId/profils", dto)

    // ── Utilisateurs ──────────────────────────────────────────────

    suspend fun listUsersByEcole(schoolId: String): List<UserApiDto>? =
        get("/api/schools/$schoolId/users")

    suspend fun createUser(groupeId: String, dto: CreateUserGroupeDto): UserApiDto? =
        post("/api/groupes/$groupeId/users", dto)

    suspend fun assignProfil(groupeId: String, userId: String, dto: AssignProfilDto): Boolean =
        executeStatus("PUT", "/api/groupes/$groupeId/users/$userId/assign-profil") {
            httpClient.put("$baseUrl/api/groupes/$groupeId/users/$userId/assign-profil") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(dto)
            }
        }

    // ── Modules disponibles ───────────────────────────────────────

    suspend fun listModulesDisponibles(groupeId: String): List<ModuleApiDto>? =
        get("/api/groupes/$groupeId/modules-disponibles")

    // ── Catégories disponibles (sidebar dynamique) ──────────────

    suspend fun listCategoriesDisponibles(groupeId: String): List<CategorieWithModulesDto>? =
        get("/api/groupes/$groupeId/categories-disponibles")

    // ── Invoice timeline (graphe évolution) ─────────────────────

    suspend fun getInvoiceTimeline(groupeId: String): List<MonthlyInvoiceStatsDto>? =
        get("/api/groupes/$groupeId/invoice-timeline")

    // ── KPI par catégorie de module ───────────────────────────────

    suspend fun getModuleKpi(groupeId: String, category: String): ModuleKpiDto? =
        get("/api/groupes/$groupeId/kpi/$category")

    // ── Timeline activité (graphe utilisateurs/mois) ─────────────

    suspend fun getActivityTimeline(groupeId: String): List<MonthlyActivityDto>? =
        get("/api/groupes/$groupeId/activity-timeline")

    // ── Notifications groupe ─────────────────────────────────────

    suspend fun getNotifications(groupeId: String): List<GroupeNotificationDto>? =
        get("/api/groupes/$groupeId/notifications")
}
