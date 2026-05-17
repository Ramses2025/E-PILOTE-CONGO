package cg.epilote.desktop.data

import io.ktor.client.request.*
import io.ktor.http.*

suspend fun DesktopAdminClient.getDashboardStats(): DashboardStatsDto? =
    get("/api/super-admin/dashboard-stats")

suspend fun DesktopAdminClient.listGroupes(): List<GroupeApiDto>? =
    get("/api/super-admin/groupes")

suspend fun DesktopAdminClient.listPlans(): List<PlanApiDto>? =
    get("/api/super-admin/plans")

suspend fun DesktopAdminClient.createPlan(dto: CreatePlanDto): PlanApiDto? =
    post("/api/super-admin/plans", dto)

suspend fun DesktopAdminClient.updatePlan(planId: String, dto: UpdatePlanDto): PlanApiDto? =
    put("/api/super-admin/plans/$planId", dto)

suspend fun DesktopAdminClient.listSubscriptions(limit: Int = 500, offset: Int = 0): List<SubscriptionApiDto>? =
    get("/api/super-admin/subscriptions?limit=$limit&offset=$offset")

suspend fun DesktopAdminClient.createSubscription(dto: CreateSubscriptionDto): SubscriptionApiDto? =
    post("/api/super-admin/subscriptions", dto)

suspend fun DesktopAdminClient.updateSubscriptionStatus(subId: String, statut: String): SubscriptionApiDto? =
    execute("PUT", "/api/super-admin/subscriptions/$subId/status?statut=$statut") {
        httpClient.put("$baseUrl/api/super-admin/subscriptions/$subId/status") {
            contentType(ContentType.Application.Json)
            parameter("statut", statut)
            tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }

suspend fun DesktopAdminClient.listInvoices(): List<InvoiceApiDto>? =
    get("/api/super-admin/invoices")

suspend fun DesktopAdminClient.createInvoice(dto: CreateInvoiceDto): InvoiceApiDto? =
    post("/api/super-admin/invoices", dto)

suspend fun DesktopAdminClient.downloadInvoicePdf(invoiceId: String): ByteArray? =
    executeBytes("GET", "/api/super-admin/invoices/$invoiceId/pdf") {
        httpClient.get("$baseUrl/api/super-admin/invoices/$invoiceId/pdf") {
            tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }

suspend fun DesktopAdminClient.updateInvoiceStatus(invoiceId: String, statut: String, datePaiement: Long? = null): InvoiceApiDto? =
    execute("PUT", "/api/super-admin/invoices/$invoiceId/status?statut=$statut") {
        httpClient.put("$baseUrl/api/super-admin/invoices/$invoiceId/status") {
            contentType(ContentType.Application.Json)
            parameter("statut", statut)
            datePaiement?.let { parameter("datePaiement", it) }
            tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }

suspend fun DesktopAdminClient.listAnnouncements(): List<AnnouncementApiDto>? =
    get("/api/super-admin/communications/announcements")

suspend fun DesktopAdminClient.createAnnouncement(dto: CreateAnnouncementDto): AnnouncementApiDto? =
    post("/api/super-admin/communications/announcements", dto)

suspend fun DesktopAdminClient.listMessages(): List<AdminMessageApiDto>? =
    get("/api/super-admin/communications/messages")

suspend fun DesktopAdminClient.createMessage(dto: CreateAdminMessageDto): AdminMessageApiDto? =
    post("/api/super-admin/communications/messages", dto)

suspend fun DesktopAdminClient.updateMessageStatus(messageId: String, status: String): AdminMessageApiDto? =
    execute("PUT", "/api/super-admin/communications/messages/$messageId/status?status=$status") {
        httpClient.put("$baseUrl/api/super-admin/communications/messages/$messageId/status") {
            parameter("status", status)
            tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }

suspend fun DesktopAdminClient.listModules(): List<ModuleApiDto>? =
    get("/api/super-admin/modules")

suspend fun DesktopAdminClient.createModule(dto: CreateModuleDto): ModuleApiDto? =
    post("/api/super-admin/modules", dto)

suspend fun DesktopAdminClient.updateModule(moduleId: String, dto: UpdateModuleDto): ModuleApiDto? =
    put("/api/super-admin/modules/$moduleId", dto)

suspend fun DesktopAdminClient.listCategories(): List<CategorieApiDto>? =
    get("/api/super-admin/categories")

suspend fun DesktopAdminClient.listAdminGroupes(groupId: String): List<UserApiDto>? =
    get("/api/super-admin/groupes/$groupId/admins")

suspend fun DesktopAdminClient.createGroupe(dto: CreateGroupeDto): GroupeApiDto? =
    post("/api/super-admin/groupes", dto)

suspend fun DesktopAdminClient.updateGroupe(groupId: String, dto: UpdateGroupeDto): GroupeApiDto? =
    put("/api/super-admin/groupes/$groupId", dto)

suspend fun DesktopAdminClient.deleteGroupe(groupId: String): Boolean =
    executeStatus("DELETE", "/api/super-admin/groupes/$groupId") {
        httpClient.delete("$baseUrl/api/super-admin/groupes/$groupId") {
            tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }

suspend fun DesktopAdminClient.createAdminGroupe(
    groupId: String,
    password: String,
    nom: String,
    prenom: String,
    email: String
): UserApiDto? = post(
    "/api/super-admin/groupes/$groupId/admins",
    CreateAdminGroupeDto(password, nom, prenom, email)
)

suspend fun DesktopAdminClient.createCategorie(dto: CreateCategorieDto): CategorieApiDto? =
    post("/api/super-admin/categories", dto)

suspend fun DesktopAdminClient.updateCategorie(code: String, dto: UpdateCategorieDto): CategorieApiDto? =
    put("/api/super-admin/categories/$code", dto)

suspend fun DesktopAdminClient.listAllAdmins(): List<AdminUserApiDto>? =
    get("/api/super-admin/admins")

suspend fun DesktopAdminClient.createAdminUser(dto: CreateAdminUserDto): AdminUserApiDto? =
    post("/api/super-admin/admins", dto)

suspend fun DesktopAdminClient.updateAdminUser(userId: String, dto: UpdateAdminUserDto): AdminUserApiDto? =
    put("/api/super-admin/admins/$userId", dto)

suspend fun DesktopAdminClient.deleteAdminUser(userId: String): Boolean =
    executeStatus("DELETE", "/api/super-admin/admins/$userId") {
        httpClient.delete("$baseUrl/api/super-admin/admins/$userId") {
            tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }

suspend fun DesktopAdminClient.toggleAdminStatus(userId: String, status: String): AdminUserApiDto? =
    put("/api/super-admin/admins/$userId/status", ToggleAdminStatusDto(status))

suspend fun DesktopAdminClient.getPlatformIdentity(): PlatformIdentityDto? =
    get("/api/super-admin/platform-identity")

suspend fun DesktopAdminClient.updatePlatformIdentity(dto: UpdatePlatformIdentityDto): PlatformIdentityDto? =
    put("/api/super-admin/platform-identity", dto)

suspend fun DesktopAdminClient.listPaymentMethods(): List<PaymentMethodDto>? =
    get("/api/super-admin/payment-methods")

suspend fun DesktopAdminClient.recordPayment(dto: RecordPaymentDto): PaymentReceiptDto? =
    post("/api/super-admin/payment-receipts", dto)

suspend fun DesktopAdminClient.recordPaymentResult(dto: RecordPaymentDto): DesktopAdminClient.ApiCallResult<PaymentReceiptDto> =
    postResult("/api/super-admin/payment-receipts", dto)

suspend fun DesktopAdminClient.listPaymentReceipts(limit: Int = 500, offset: Int = 0): List<PaymentReceiptDto>? =
    get("/api/super-admin/payment-receipts?limit=$limit&offset=$offset")

suspend fun DesktopAdminClient.listPaymentReceiptsByGroupe(groupeId: String, limit: Int = 200, offset: Int = 0): List<PaymentReceiptDto>? =
    get("/api/super-admin/groupes/$groupeId/payment-receipts?limit=$limit&offset=$offset")

suspend fun DesktopAdminClient.deletePaymentReceipt(receiptId: String): DesktopAdminClient.ApiCallResult<Unit> =
    deleteResult("/api/super-admin/payment-receipts/$receiptId")

suspend fun DesktopAdminClient.deleteInvoice(invoiceId: String): DesktopAdminClient.ApiCallResult<Unit> =
    deleteResult("/api/super-admin/invoices/$invoiceId")

suspend fun DesktopAdminClient.listAuditLogs(
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

suspend fun DesktopAdminClient.listAuditActions(): List<AuditActionDto>? =
    get("/api/super-admin/audit-logs/actions")

suspend fun DesktopAdminClient.changePassword(dto: ChangePasswordRequestDto): ChangePasswordResponseDto? =
    post("/api/auth/change-password", dto)

suspend fun DesktopAdminClient.listSubscriptionRequests(status: String? = null, limit: Int = 200, offset: Int = 0): List<SubscriptionRequestApiDto>? {
    val prefix = if (status != null) "?status=$status&" else "?"
    return get("/api/super-admin/subscription-requests${prefix}limit=$limit&offset=$offset")
}

suspend fun DesktopAdminClient.resolveSubscriptionRequest(id: String, action: String, notes: String? = null): Boolean =
    executeStatus("POST", "/api/super-admin/subscription-requests/$id/resolve") {
        httpClient.post("$baseUrl/api/super-admin/subscription-requests/$id/resolve") {
            parameter("action", action)
            if (!notes.isNullOrBlank()) parameter("notes", notes)
            tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
