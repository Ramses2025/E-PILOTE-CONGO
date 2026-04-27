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

    /**
     * Sémantique HTTP officielle (RFC 7235 / RFC 7231) :
     *  - 401 Unauthorized : credentials manquants ou expirés → tenter un refresh.
     *  - 403 Forbidden    : authentifié mais non autorisé → un refresh ne changera
     *                       pas le rôle, donc inutile (et masque l'erreur métier
     *                       réelle). On laisse la 403 remonter.
     */
    private fun shouldAttemptRefresh(status: HttpStatusCode): Boolean =
        status == HttpStatusCode.Unauthorized

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

    private suspend fun executeBytes(
        method: String,
        path: String,
        request: suspend () -> HttpResponse
    ): ByteArray? {
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
                    return runCatching { retryResponse.body<ByteArray>() }.getOrElse { error ->
                        println("DesktopAdminClient $method $path byte decode failed: ${error.message}")
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

        return runCatching { firstResponse.body<ByteArray>() }.getOrElse { error ->
            println("DesktopAdminClient $method $path byte decode failed: ${error.message}")
            null
        }
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

    suspend fun downloadInvoicePdf(invoiceId: String): ByteArray? =
        executeBytes("GET", "/api/super-admin/invoices/$invoiceId/pdf") {
            httpClient.get("$baseUrl/api/super-admin/invoices/$invoiceId/pdf") {
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
        }

    suspend fun updateInvoiceStatus(invoiceId: String, statut: String, datePaiement: Long? = null): InvoiceApiDto? =
        execute("PUT", "/api/super-admin/invoices/$invoiceId/status?statut=$statut") {
            httpClient.put("$baseUrl/api/super-admin/invoices/$invoiceId/status") {
                contentType(ContentType.Application.Json)
                parameter("statut", statut)
                datePaiement?.let { parameter("datePaiement", it) }
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
        }

    suspend fun listAnnouncements(): List<AnnouncementApiDto>? =
        get("/api/super-admin/communications/announcements")

    suspend fun createAnnouncement(dto: CreateAnnouncementDto): AnnouncementApiDto? =
        post("/api/super-admin/communications/announcements", dto)

    suspend fun listMessages(): List<AdminMessageApiDto>? =
        get("/api/super-admin/communications/messages")

    suspend fun createMessage(dto: CreateAdminMessageDto): AdminMessageApiDto? =
        post("/api/super-admin/communications/messages", dto)

    suspend fun updateMessageStatus(messageId: String, status: String): AdminMessageApiDto? =
        execute("PUT", "/api/super-admin/communications/messages/$messageId/status?status=$status") {
            httpClient.put("$baseUrl/api/super-admin/communications/messages/$messageId/status") {
                parameter("status", status)
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

    // ── Paramètres plateforme (identité juridique) ─────────────────
    //
    // Références officielles :
    // • Spring Web (Kotlin) — REST clients : https://docs.spring.io/spring-framework/reference/web/webflux-client.html
    // • Ktor client : https://ktor.io/docs/client-requests.html

    suspend fun getPlatformIdentity(): PlatformIdentityDto? =
        get("/api/super-admin/platform-identity")

    suspend fun updatePlatformIdentity(dto: UpdatePlatformIdentityDto): PlatformIdentityDto? =
        put("/api/super-admin/platform-identity", dto)

    // ── Paiements présentiels & abonnements ────────────────────────
    suspend fun listPaymentMethods(): List<PaymentMethodDto>? =
        get("/api/super-admin/payment-methods")

    suspend fun recordPayment(dto: RecordPaymentDto): PaymentReceiptDto? =
        post("/api/super-admin/payment-receipts", dto)

    suspend fun listPaymentReceipts(): List<PaymentReceiptDto>? =
        get("/api/super-admin/payment-receipts")

    suspend fun listPaymentReceiptsByGroupe(groupeId: String): List<PaymentReceiptDto>? =
        get("/api/super-admin/groupes/$groupeId/payment-receipts")

    // ── Audit Logs (journal serveur cloud-only) ────────────────────
    //
    // Le backend retourne `AuditLogPage` (items + total + page + pageSize).
    // Référence Spring : https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/requestparam.html

    suspend fun listAuditLogs(
        page: Int = 1,
        pageSize: Int = 50,
        category: String? = null,
        action: String? = null,
        outcome: String? = null,
        actorId: String? = null,
        targetId: String? = null,
        since: Long? = null,
        until: Long? = null,
        search: String? = null
    ): AuditLogPageDto? = execute("GET", "/api/super-admin/audit-logs?page=$page") {
        httpClient.get("$baseUrl/api/super-admin/audit-logs") {
            contentType(ContentType.Application.Json)
            parameter("page", page)
            parameter("pageSize", pageSize)
            category?.let { parameter("category", it) }
            action?.let { parameter("action", it) }
            outcome?.let { parameter("outcome", it) }
            actorId?.let { parameter("actorId", it) }
            targetId?.let { parameter("targetId", it) }
            since?.let { parameter("since", it) }
            until?.let { parameter("until", it) }
            search?.takeIf { it.isNotBlank() }?.let { parameter("search", it) }
            tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }

    suspend fun listAuditActions(): List<AuditActionDto>? =
        get("/api/super-admin/audit-logs/actions")

    // ── Mot de passe (self-service + reset administratif) ──────────

    suspend fun changePassword(dto: ChangePasswordRequestDto): ChangePasswordResponseDto? =
        post("/api/auth/change-password", dto)
}
