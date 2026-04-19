package cg.epilote.desktop.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class DesktopAdminClient(
    private val baseUrl: String,
    private val tokenProvider: () -> String?
) {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    private suspend inline fun <reified T> get(path: String): T? =
        runCatching {
            httpClient.get("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body<T>()
        }.getOrNull()

    private suspend inline fun <reified T, reified B : Any> post(path: String, body: B): T? =
        runCatching {
            httpClient.post("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(body)
            }.body<T>()
        }.getOrNull()

    private suspend inline fun <reified T> delete(path: String): T? =
        runCatching {
            httpClient.delete("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body<T>()
        }.getOrNull()

    private suspend inline fun <reified T, reified B : Any> put(path: String, body: B): T? =
        runCatching {
            httpClient.put("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(body)
            }.body<T>()
        }.getOrNull()

    suspend fun getDashboardStats(): DashboardStatsDto? =
        get("/api/super-admin/dashboard-stats")

    suspend fun listGroupes(): List<GroupeApiDto>? =
        get("/api/super-admin/groupes")

    suspend fun listPlans(): List<PlanApiDto>? =
        get("/api/super-admin/plans")

    suspend fun listModules(): List<ModuleApiDto>? =
        get("/api/super-admin/modules")

    suspend fun listAdminGroupes(groupId: String): List<UserApiDto>? =
        get("/api/super-admin/groupes/$groupId/admins")

    suspend fun createGroupe(dto: CreateGroupeDto): GroupeApiDto? =
        post("/api/super-admin/groupes", dto)

    suspend fun updateGroupe(groupId: String, dto: UpdateGroupeDto): GroupeApiDto? =
        put("/api/super-admin/groupes/$groupId", dto)

    suspend fun deleteGroupe(groupId: String): Boolean =
        runCatching {
            httpClient.delete("$baseUrl/api/super-admin/groupes/$groupId") {
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.status.value in 200..299
        }.getOrDefault(false)

    suspend fun createAdminGroupe(
        groupId: String,
        password: String,
        nom: String,
        prenom: String,
        email: String
    ): UserApiDto? = post(
        "/api/super-admin/groupes/$groupId/admins",
        CreateAdminGroupeDto(password, nom, prenom, email)
    )

    // ── Admin Users (Super Admin scope) ──────────────────────────

    suspend fun listAllAdmins(): List<AdminUserApiDto>? =
        get("/api/super-admin/admins")

    suspend fun createAdminUser(dto: CreateAdminUserDto): AdminUserApiDto? =
        post("/api/super-admin/admins", dto)

    suspend fun updateAdminUser(userId: String, dto: UpdateAdminUserDto): AdminUserApiDto? =
        put("/api/super-admin/admins/$userId", dto)

    suspend fun deleteAdminUser(userId: String): Boolean =
        runCatching {
            httpClient.delete("$baseUrl/api/super-admin/admins/$userId") {
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.status.value in 200..299
        }.getOrDefault(false)

    suspend fun toggleAdminStatus(userId: String, status: String): AdminUserApiDto? =
        put("/api/super-admin/admins/$userId/status", ToggleAdminStatusDto(status))
}
