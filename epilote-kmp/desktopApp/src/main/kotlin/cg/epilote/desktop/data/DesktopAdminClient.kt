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

class DesktopAdminClient(
    private val baseUrl: String,
    private val tokenProvider: () -> String?,
    private val onUnauthorized: suspend () -> Boolean = { false }
) {
    private val httpClient = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
    }

    private fun shouldAttemptRefresh(status: HttpStatusCode): Boolean =
        status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden

    private suspend inline fun <reified T> execute(
        method: String,
        path: String,
        crossinline request: suspend () -> HttpResponse
    ): T? {
        val firstResponse = runCatching { request() }.getOrElse { error ->
            println("DesktopAdminClient $method $path request failed: ${error.message}")
            return null
        }

        if (shouldAttemptRefresh(firstResponse.status)) {
            println("DesktopAdminClient $method $path returned ${firstResponse.status}; attempting token refresh")
            val refreshed = runCatching { onUnauthorized() }.getOrDefault(false)
            if (refreshed) {
                val retryResponse = runCatching { request() }.getOrElse { error ->
                    println("DesktopAdminClient $method $path retry failed: ${error.message}")
                    return null
                }
                if (retryResponse.status.value in 200..299) {
                    return runCatching {
                        retryResponse.body<T>()
                    }.getOrElse { error ->
                        println("DesktopAdminClient $method $path decode failed: ${error.message}")
                        null
                    }
                }
                println("DesktopAdminClient $method $path retry returned ${retryResponse.status}")
                return null
            }
            return null
        }

        if (firstResponse.status.value !in 200..299) {
            println("DesktopAdminClient $method $path returned ${firstResponse.status}")
            return null
        }

        return runCatching {
            firstResponse.body<T>()
        }.getOrElse { error ->
            println("DesktopAdminClient $method $path decode failed: ${error.message}")
            null
        }
    }

    private suspend fun executeStatus(
        method: String,
        path: String,
        request: suspend () -> HttpResponse
    ): Boolean {
        val firstResponse = runCatching { request() }.getOrElse { error ->
            println("DesktopAdminClient $method $path request failed: ${error.message}")
            return false
        }

        if (shouldAttemptRefresh(firstResponse.status)) {
            println("DesktopAdminClient $method $path returned ${firstResponse.status}; attempting token refresh")
            val refreshed = runCatching { onUnauthorized() }.getOrDefault(false)
            if (refreshed) {
                val retryResponse = runCatching { request() }.getOrElse { error ->
                    println("DesktopAdminClient $method $path retry failed: ${error.message}")
                    return false
                }
                return retryResponse.status.value in 200..299
            }
            return false
        }

        return firstResponse.status.value in 200..299
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

    private suspend inline fun <reified T> delete(path: String): T? =
        execute("DELETE", path) {
            httpClient.delete("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
        }

    private suspend inline fun <reified T, reified B : Any> put(path: String, body: B): T? =
        execute("PUT", path) {
            httpClient.put("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(body)
            }
        }

    suspend fun getDashboardStats(): DashboardStatsDto? =
        get("/api/super-admin/dashboard-stats")

    suspend fun listGroupes(): List<GroupeApiDto>? =
        get("/api/super-admin/groupes")

    suspend fun listPlans(): List<PlanApiDto>? =
        get("/api/super-admin/plans")

    suspend fun createPlan(dto: CreatePlanDto): PlanApiDto? =
        post("/api/super-admin/plans", dto)

    suspend fun updatePlan(planId: String, dto: UpdatePlanDto): PlanApiDto? =
        put("/api/super-admin/plans/$planId", dto)

    suspend fun listSubscriptions(): List<SubscriptionApiDto>? =
        get("/api/super-admin/subscriptions")

    suspend fun createSubscription(dto: CreateSubscriptionDto): SubscriptionApiDto? =
        post("/api/super-admin/subscriptions", dto)

    suspend fun updateSubscriptionStatus(subId: String, statut: String): SubscriptionApiDto? =
        execute("PUT", "/api/super-admin/subscriptions/$subId/status?statut=$statut") {
            httpClient.put("$baseUrl/api/super-admin/subscriptions/$subId/status") {
                contentType(ContentType.Application.Json)
                parameter("statut", statut)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
        }

    suspend fun listInvoices(): List<InvoiceApiDto>? =
        get("/api/super-admin/invoices")

    suspend fun createInvoice(dto: CreateInvoiceDto): InvoiceApiDto? =
        post("/api/super-admin/invoices", dto)

    suspend fun updateInvoiceStatus(invoiceId: String, statut: String, datePaiement: Long? = null): InvoiceApiDto? =
        execute("PUT", "/api/super-admin/invoices/$invoiceId/status?statut=$statut") {
            httpClient.put("$baseUrl/api/super-admin/invoices/$invoiceId/status") {
                contentType(ContentType.Application.Json)
                parameter("statut", statut)
                datePaiement?.let { parameter("datePaiement", it) }
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
        }

    suspend fun listModules(): List<ModuleApiDto>? =
        get("/api/super-admin/modules")

    suspend fun createModule(dto: CreateModuleDto): ModuleApiDto? =
        post("/api/super-admin/modules", dto)

    suspend fun updateModule(moduleId: String, dto: UpdateModuleDto): ModuleApiDto? =
        put("/api/super-admin/modules/$moduleId", dto)

    suspend fun listCategories(): List<CategorieApiDto>? =
        get("/api/super-admin/categories")

    suspend fun listAdminGroupes(groupId: String): List<UserApiDto>? =
        get("/api/super-admin/groupes/$groupId/admins")

    suspend fun createGroupe(dto: CreateGroupeDto): GroupeApiDto? =
        post("/api/super-admin/groupes", dto)

    suspend fun updateGroupe(groupId: String, dto: UpdateGroupeDto): GroupeApiDto? =
        put("/api/super-admin/groupes/$groupId", dto)

    suspend fun deleteGroupe(groupId: String): Boolean =
        executeStatus("DELETE", "/api/super-admin/groupes/$groupId") {
            httpClient.delete("$baseUrl/api/super-admin/groupes/$groupId") {
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
        }

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

    suspend fun createCategorie(dto: CreateCategorieDto): CategorieApiDto? =
        post("/api/super-admin/categories", dto)

    suspend fun updateCategorie(code: String, dto: UpdateCategorieDto): CategorieApiDto? =
        put("/api/super-admin/categories/$code", dto)

    // ── Admin Users (Super Admin scope) ──────────────────────────

    suspend fun listAllAdmins(): List<AdminUserApiDto>? =
        get("/api/super-admin/admins")

    suspend fun createAdminUser(dto: CreateAdminUserDto): AdminUserApiDto? =
        post("/api/super-admin/admins", dto)

    suspend fun updateAdminUser(userId: String, dto: UpdateAdminUserDto): AdminUserApiDto? =
        put("/api/super-admin/admins/$userId", dto)

    suspend fun deleteAdminUser(userId: String): Boolean =
        executeStatus("DELETE", "/api/super-admin/admins/$userId") {
            httpClient.delete("$baseUrl/api/super-admin/admins/$userId") {
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
        }

    suspend fun toggleAdminStatus(userId: String, status: String): AdminUserApiDto? =
        put("/api/super-admin/admins/$userId/status", ToggleAdminStatusDto(status))
}
