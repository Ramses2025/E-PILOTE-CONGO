package cg.epilote.desktop.data
import java.util.logging.Logger

import cg.epilote.desktop.ui.screens.AdminUserDto
import cg.epilote.desktop.ui.screens.CategorieDto
import cg.epilote.desktop.ui.screens.GroupeDto
import cg.epilote.desktop.ui.screens.ModuleDto
import cg.epilote.desktop.ui.screens.PlanDto
import cg.epilote.desktop.ui.screens.UserDto
import cg.epilote.desktop.ui.screens.superadmin.AdminStats
import cg.epilote.desktop.ui.screens.superadmin.toAdminStats
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminDataRepository(
    private val client: DesktopAdminClient
) {
    private val log = Logger.getLogger(AdminDataRepository::class.java.name)
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val inFlightRefreshes = ConcurrentHashMap<String, AtomicBoolean>()

    private val _dashboardStats = MutableStateFlow(DashboardStatsDto())
    val dashboardStats: StateFlow<DashboardStatsDto> = _dashboardStats.asStateFlow()

    private val _adminStats = MutableStateFlow(AdminStats())
    val adminStats: StateFlow<AdminStats> = _adminStats.asStateFlow()

    private val _groupes = MutableStateFlow<List<GroupeDto>>(emptyList())
    val groupes: StateFlow<List<GroupeDto>> = _groupes.asStateFlow()

    private val _adminGroupAdmins = MutableStateFlow<Map<String, List<UserDto>>>(emptyMap())
    val adminGroupAdmins: StateFlow<Map<String, List<UserDto>>> = _adminGroupAdmins.asStateFlow()

    private val _plans = MutableStateFlow<List<PlanDto>>(emptyList())
    val plans: StateFlow<List<PlanDto>> = _plans.asStateFlow()

    private val _modules = MutableStateFlow<List<ModuleDto>>(emptyList())
    val modules: StateFlow<List<ModuleDto>> = _modules.asStateFlow()

    private val _categories = MutableStateFlow<List<CategorieDto>>(emptyList())
    val categories: StateFlow<List<CategorieDto>> = _categories.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<AdminUserDto>>(emptyList())
    val adminUsers: StateFlow<List<AdminUserDto>> = _adminUsers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _rawSubscriptions = MutableStateFlow<List<SubscriptionApiDto>>(emptyList())
    val rawSubscriptions: StateFlow<List<SubscriptionApiDto>> = _rawSubscriptions.asStateFlow()

    private val _rawInvoices = MutableStateFlow<List<InvoiceApiDto>>(emptyList())
    val rawInvoices: StateFlow<List<InvoiceApiDto>> = _rawInvoices.asStateFlow()

    private val _messages = MutableStateFlow<List<AdminMessageApiDto>>(emptyList())
    val messages: StateFlow<List<AdminMessageApiDto>> = _messages.asStateFlow()

    private val _announcements = MutableStateFlow<List<AnnouncementApiDto>>(emptyList())
    val announcements: StateFlow<List<AnnouncementApiDto>> = _announcements.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditEventApiDto>>(emptyList())
    val auditLogs: StateFlow<List<AuditEventApiDto>> = _auditLogs.asStateFlow()

    private val _auditTotal = MutableStateFlow(0L)
    val auditTotal: StateFlow<Long> = _auditTotal.asStateFlow()

    private val _paymentReceipts = MutableStateFlow<List<PaymentReceiptDto>>(emptyList())
    val paymentReceipts: StateFlow<List<PaymentReceiptDto>> = _paymentReceipts.asStateFlow()

    private val _messagingReloadTick = MutableStateFlow(0)
    val messagingReloadTick: StateFlow<Int> = _messagingReloadTick.asStateFlow()

    var lastEventId: String? = null

    fun refreshAllAsync(showLoading: Boolean = true) {
        launchRefreshOnce("all") { refreshAll(showLoading = showLoading) }
    }

    fun refreshAllInBackgroundAsync() {
        refreshAllAsync(showLoading = false)
    }

    fun refreshGroupesAsync() {
        launchRefreshOnce("groupes") { refreshGroupes() }
    }

    fun refreshPlansAsync() {
        launchRefreshOnce("plans") { refreshPlans() }
    }

    fun refreshModulesAsync() {
        launchRefreshOnce("modules") { refreshModules() }
    }

    fun refreshCategoriesAsync() {
        launchRefreshOnce("categories") { refreshCategories() }
    }

    fun refreshAdminsAsync() {
        launchRefreshOnce("admins") { refreshAdmins() }
    }

    fun notifyMessagingReload() {
        _messagingReloadTick.value++
        refreshMessagingAsync()
    }

    fun refreshSubscriptionsAsync() {
        launchRefreshOnce("subscriptions") { refreshSubscriptions() }
    }

    fun refreshInvoicesAsync() {
        launchRefreshOnce("invoices") { refreshInvoices() }
    }

    fun refreshMessagingAsync() {
        launchRefreshOnce("messaging") { refreshMessaging() }
    }

    fun refreshAuditAsync() {
        launchRefreshOnce("audit") { refreshAudit() }
    }

    fun refreshPaymentsAsync() {
        launchRefreshOnce("payments") { refreshPayments() }
    }

    fun refreshDashboardStatsAsync() {
        launchRefreshOnce("dashboard") { refreshDashboardStats() }
    }

    fun upsertGroupe(groupe: GroupeApiDto) {
        val mapped = groupe.toGroupeDto()
        _groupes.value = _groupes.value.upsertBy(mapped, GroupeDto::id)
    }

    fun removeGroupe(groupId: String) {
        _groupes.value = _groupes.value.filterNot { it.id == groupId }
        _adminGroupAdmins.value = _adminGroupAdmins.value - groupId
        _adminUsers.value = _adminUsers.value.filterNot { it.groupId == groupId }
    }

    fun upsertAdminGroupe(groupId: String, admin: UserApiDto) {
        val userDto = admin.toUserDto()
        _adminGroupAdmins.value = _adminGroupAdmins.value.toMutableMap().apply {
            val current = this[groupId].orEmpty()
            this[groupId] = current.upsertBy(userDto, UserDto::id)
        }
        _adminUsers.value = _adminUsers.value.upsertBy(admin.toAdminUserDto()) { it.id }
    }

    fun upsertPlan(plan: PlanApiDto) {
        val mapped = plan.toPlanDto()
        _plans.value = _plans.value.upsertBy(mapped, PlanDto::id)
    }

    fun upsertModule(module: ModuleApiDto) {
        val mapped = module.toModuleDto()
        _modules.value = _modules.value.upsertBy(mapped, ModuleDto::id)
    }

    fun upsertCategory(category: CategorieApiDto) {
        val mapped = category.toCategorieDto()
        _categories.value = _categories.value.upsertBy(mapped, CategorieDto::code)
    }

    fun upsertAdmin(admin: AdminUserApiDto) {
        val mapped = admin.toAdminUserDto()
        _adminUsers.value = _adminUsers.value.upsertBy(mapped, AdminUserDto::id)
        syncAdminGroupMapping(mapped)
    }

    fun removeAdmin(userId: String) {
        _adminUsers.value = _adminUsers.value.filterNot { it.id == userId }
        _adminGroupAdmins.value = _adminGroupAdmins.value.mapValues { (_, admins) ->
            admins.filterNot { it.id == userId }
        }
    }

    suspend fun refreshAll(showLoading: Boolean = true) {
        log.info("[AdminRepo] refreshAll START")
        if (showLoading) {
            _isLoading.value = true
        }
        try {
            coroutineScope {
                val dStats = async { runCatching { client.getDashboardStats() }.getOrNull() }
                val dGroupes = async { runCatching { client.listGroupes() }.getOrNull()?.map { it.toGroupeDto() } }
                val dPlans = async { runCatching { client.listPlans() }.getOrNull()?.map {
                    PlanDto(id = it.id, nom = it.nom, type = it.type, prixXAF = it.prixXAF,
                        currency = it.currency, maxStudents = it.maxStudents, maxPersonnel = it.maxPersonnel,
                        modulesIncluded = it.modulesIncluded, isActive = it.isActive)
                } }
                val dModules = async { runCatching { client.listModules() }.getOrNull()?.map {
                    ModuleDto(it.id, it.code, it.nom, it.categorieCode, it.description, it.isCore, it.requiredPlan, it.isActive, it.ordre)
                } }
                val dCategories = async { runCatching { client.listCategories() }.getOrNull()?.map {
                    CategorieDto(code = it.code, nom = it.nom, isCore = it.isCore, ordre = it.ordre, isActive = it.isActive)
                } }
                val dAdmins = async { runCatching { client.listAllAdmins() }.getOrNull()?.map { it.toAdminUserDto() } }
                val dSubs = async { runCatching { client.listSubscriptions() }.getOrNull() }
                val dInvoices = async { runCatching { client.listInvoices() }.getOrNull() }
                val dMessages = async { runCatching { client.listMessages() }.getOrNull() }
                val dAnnouncements = async { runCatching { client.listAnnouncements() }.getOrNull() }
                val dAudit = async { runCatching { client.listAuditLogs(page = 1, pageSize = 50) }.getOrNull() }
                val dPayments = async { runCatching { client.listPaymentReceipts() }.getOrNull() }

                awaitAll(dStats, dGroupes, dPlans, dModules, dCategories, dAdmins, dSubs, dInvoices, dMessages, dAnnouncements, dAudit, dPayments)

                val stats = dStats.await() ?: _dashboardStats.value
                val groupes = dGroupes.await() ?: _groupes.value
                val admins = dAdmins.await() ?: _adminUsers.value

                _dashboardStats.value = stats
                _adminStats.value = stats.toAdminStats()
                _groupes.value = groupes
                _adminGroupAdmins.value = buildAdminsByGroup(groupes, admins)
                _plans.value = dPlans.await() ?: _plans.value
                _modules.value = dModules.await() ?: _modules.value
                _categories.value = dCategories.await() ?: _categories.value
                _adminUsers.value = admins
                dSubs.await()?.let { _rawSubscriptions.value = it }
                dInvoices.await()?.let { _rawInvoices.value = it }
                dMessages.await()?.let { _messages.value = it }
                dAnnouncements.await()?.let { _announcements.value = it }
                dAudit.await()?.let { _auditLogs.value = it.items; _auditTotal.value = it.total }
                dPayments.await()?.let { _paymentReceipts.value = it }
            }
        } catch (e: Exception) {
            log.warning("[AdminRepo] refreshAll FAILED: ${e.message}")
        }
        if (showLoading) {
            _isLoading.value = false
        }
        log.info("[AdminRepo] refreshAll DONE — groupes=${_groupes.value.size} plans=${_plans.value.size} admins=${_adminUsers.value.size}")
    }

    suspend fun refreshGroupes() {
        val groupes = runCatching { client.listGroupes() }.getOrNull()?.map { it.toGroupeDto() } ?: _groupes.value
        val admins = runCatching { client.listAllAdmins() }.getOrNull()?.map { it.toAdminUserDto() } ?: _adminUsers.value
        _groupes.value = groupes
        _adminGroupAdmins.value = buildAdminsByGroup(groupes, admins)
        _adminUsers.value = admins
        refreshDashboardStats()
    }

    suspend fun refreshPlans() {
        val plans = runCatching { client.listPlans() }.getOrNull()?.map {
            PlanDto(id = it.id, nom = it.nom, type = it.type, prixXAF = it.prixXAF,
                currency = it.currency, maxStudents = it.maxStudents, maxPersonnel = it.maxPersonnel,
                modulesIncluded = it.modulesIncluded, isActive = it.isActive)
        } ?: _plans.value
        _plans.value = plans
        refreshDashboardStats()
    }

    suspend fun refreshModules() {
        val modules = runCatching { client.listModules() }.getOrNull()?.map {
            ModuleDto(it.id, it.code, it.nom, it.categorieCode, it.description, it.isCore, it.requiredPlan, it.isActive, it.ordre)
        } ?: _modules.value
        _modules.value = modules
        refreshDashboardStats()
    }

    suspend fun refreshCategories() {
        val categories = runCatching { client.listCategories() }.getOrNull()?.map {
            CategorieDto(code = it.code, nom = it.nom, isCore = it.isCore, ordre = it.ordre, isActive = it.isActive)
        } ?: _categories.value
        _categories.value = categories
    }

    suspend fun refreshAdmins() {
        val admins = runCatching { client.listAllAdmins() }.getOrNull()?.map { it.toAdminUserDto() } ?: _adminUsers.value
        _adminUsers.value = admins
        _adminGroupAdmins.value = buildAdminsByGroup(_groupes.value, admins)
        refreshDashboardStats()
    }

    suspend fun refreshSubscriptions() {
        val subs = runCatching { client.listSubscriptions() }.getOrNull()
        if (subs != null) _rawSubscriptions.value = subs
    }

    suspend fun refreshInvoices() {
        val invoices = runCatching { client.listInvoices() }.getOrNull()
        if (invoices != null) _rawInvoices.value = invoices
    }

    suspend fun refreshMessaging() {
        val msgs = runCatching { client.listMessages() }.getOrNull()
        val anns = runCatching { client.listAnnouncements() }.getOrNull()
        if (msgs != null) _messages.value = msgs
        if (anns != null) _announcements.value = anns
    }

    suspend fun refreshAudit() {
        val page = runCatching { client.listAuditLogs(page = 1, pageSize = 50) }.getOrNull()
        if (page != null) {
            _auditLogs.value = page.items
            _auditTotal.value = page.total
        }
    }

    suspend fun refreshPayments() {
        val list = runCatching { client.listPaymentReceipts() }.getOrNull()
        if (list != null) _paymentReceipts.value = list
    }

    suspend fun refreshDashboardStats() {
        val stats = runCatching { client.getDashboardStats() }.getOrNull() ?: _dashboardStats.value
        _dashboardStats.value = stats
        _adminStats.value = stats.toAdminStats()
    }

    fun onSseEvent(event: AdminRealtimeEventDto) {
        lastEventId = event.eventId
        log.info("[AdminRepo] SSE event: type=${event.eventType} entity=${event.entityType} action=${event.action} hasPayload=${event.payload != null}")
        if (applyRealtimePayload(event)) return

        when (event.entityType) {
            "groupe" -> refreshGroupesAsync()
            "plan" -> refreshPlansAsync()
            "module" -> refreshModulesAsync()
            "category" -> refreshCategoriesAsync()
            "admin" -> refreshAdminsAsync()
            "subscription" -> { refreshSubscriptionsAsync(); refreshDashboardStatsAsync() }
            "invoice" -> { refreshInvoicesAsync(); refreshDashboardStatsAsync() }
            "announcement", "message" -> refreshMessagingAsync()
            "audit" -> refreshAuditAsync()
            "payment", "payment_receipt" -> refreshPaymentsAsync()
            else -> refreshAllAsync(showLoading = false)
        }
    }

    private fun launchRefreshOnce(key: String, block: suspend () -> Unit) {
        val guard = inFlightRefreshes.computeIfAbsent(key) { AtomicBoolean(false) }
        if (!guard.compareAndSet(false, true)) return

        repoScope.launch {
            try {
                block()
            } finally {
                guard.set(false)
            }
        }
    }

    private fun buildAdminsByGroup(
        groupes: List<GroupeDto>,
        admins: List<AdminUserDto>
    ): Map<String, List<UserDto>> = groupes.associate { groupe ->
        groupe.id to admins
            .asSequence()
            .filter { it.role.equals("ADMIN_GROUPE", ignoreCase = true) && it.groupId == groupe.id }
            .map { it.toUserDto() }
            .toList()
    }

    private fun syncAdminGroupMapping(admin: AdminUserDto) {
        val updated = _adminGroupAdmins.value.toMutableMap().mapValuesTo(mutableMapOf()) { (_, admins) ->
            admins.filterNot { it.id == admin.id }
        }

        if (admin.role.equals("ADMIN_GROUPE", ignoreCase = true) && !admin.groupId.isNullOrBlank()) {
            val groupId = admin.groupId
            val mapped = admin.toUserDto()
            val current = updated[groupId].orEmpty()
            updated[groupId] = current.upsertBy(mapped, UserDto::id)
        }

        _adminGroupAdmins.value = updated
    }

    private fun <T, K> List<T>.upsertBy(item: T, keySelector: (T) -> K): List<T> {
        val key = keySelector(item)
        val existingIndex = indexOfFirst { keySelector(it) == key }
        if (existingIndex == -1) return this + item

        return toMutableList().also { it[existingIndex] = item }
    }

}
