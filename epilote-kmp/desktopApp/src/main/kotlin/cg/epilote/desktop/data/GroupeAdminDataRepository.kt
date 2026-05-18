package cg.epilote.desktop.data

import java.util.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GroupeAdminDataRepository(
    private val client: DesktopGroupeClient,
    private val groupeId: String
) {
    private val log = Logger.getLogger(GroupeAdminDataRepository::class.java.name)
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _dashboardStats = MutableStateFlow<GroupeDashboardStatsDto?>(null)
    val dashboardStats: StateFlow<GroupeDashboardStatsDto?> = _dashboardStats.asStateFlow()

    private val _ecoles = MutableStateFlow<List<EcoleApiDto>>(emptyList())
    val ecoles: StateFlow<List<EcoleApiDto>> = _ecoles.asStateFlow()

    private val _profils = MutableStateFlow<List<ProfilApiDto>>(emptyList())
    val profils: StateFlow<List<ProfilApiDto>> = _profils.asStateFlow()

    private val _usersByEcole = MutableStateFlow<Map<String, List<UserApiDto>>>(emptyMap())
    val usersByEcole: StateFlow<Map<String, List<UserApiDto>>> = _usersByEcole.asStateFlow()

    private val _modules = MutableStateFlow<List<ModuleApiDto>>(emptyList())
    val modules: StateFlow<List<ModuleApiDto>> = _modules.asStateFlow()

    private val _categoriesWithModules = MutableStateFlow<List<CategorieWithModulesDto>>(emptyList())
    val categoriesWithModules: StateFlow<List<CategorieWithModulesDto>> = _categoriesWithModules.asStateFlow()

    private val _invoiceTimeline = MutableStateFlow<List<MonthlyInvoiceStatsDto>>(emptyList())
    val invoiceTimeline: StateFlow<List<MonthlyInvoiceStatsDto>> = _invoiceTimeline.asStateFlow()

    private val _moduleKpis = MutableStateFlow<Map<String, ModuleKpiDto>>(emptyMap())
    val moduleKpis: StateFlow<Map<String, ModuleKpiDto>> = _moduleKpis.asStateFlow()

    private val _activityTimeline = MutableStateFlow<List<MonthlyActivityDto>>(emptyList())
    val activityTimeline: StateFlow<List<MonthlyActivityDto>> = _activityTimeline.asStateFlow()

    private val _notifications = MutableStateFlow<List<GroupeNotificationDto>>(emptyList())
    val notifications: StateFlow<List<GroupeNotificationDto>> = _notifications.asStateFlow()

    private val _selectedEcoleId = MutableStateFlow<String?>(null)
    val selectedEcoleId: StateFlow<String?> = _selectedEcoleId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private var anySucceeded = false

    private fun normalizeDashboardCategoryCode(code: String): String = when (code.trim().lowercase()) {
        "scolarite", "scolarisation" -> "scolarisation"
        "finance", "finances" -> "finance"
        "rh", "personnel", "ressources-humaines", "ressources_humaines" -> "rh"
        else -> code.trim().lowercase()
    }

    private fun normalizeCategories(categories: List<CategorieWithModulesDto>): List<CategorieWithModulesDto> =
        categories
            .map { category -> category.copy(code = normalizeDashboardCategoryCode(category.code)) }
            .groupBy { category -> category.code }
            .values
            .map { grouped ->
                val first = grouped.minByOrNull { category -> category.ordre } ?: grouped.first()
                first.copy(
                    modules = grouped
                        .flatMap { category -> category.modules }
                        .distinctBy { module -> module.code }
                        .sortedBy { module -> module.ordre }
                )
            }
            .sortedBy { category -> category.ordre }

    suspend fun refreshAll(showLoading: Boolean = true) {
        if (showLoading && _dashboardStats.value == null) {
            _isLoading.value = true
        }
        try {
            val dStats      = repoScope.async { client.getGroupeDashboardStats(groupeId) }
            val dProfils    = repoScope.async { client.listProfils(groupeId) }
            val dModules    = repoScope.async { client.listModulesDisponibles(groupeId) }
            val dCategories = repoScope.async { client.listCategoriesDisponibles(groupeId) }
            val dTimeline   = repoScope.async { client.getInvoiceTimeline(groupeId) }
            val dActivity   = repoScope.async { client.getActivityTimeline(groupeId) }
            val dNotifs     = repoScope.async { client.getNotifications(groupeId) }
            awaitAll(dStats, dProfils, dModules, dCategories, dTimeline, dActivity, dNotifs)

            val stats = dStats.await()
            if (stats != null) {
                _dashboardStats.value = stats
                _ecoles.value = stats.ecoles
                anySucceeded = true
                _isOffline.value = false
            } else if (!anySucceeded) {
                _isOffline.value = true
            }

            dProfils.await()?.let { _profils.value = it }
            dModules.await()?.let { _modules.value = it }
            val cats = dCategories.await()
            if (cats != null) {
                val normalizedCategories = normalizeCategories(cats)
                _categoriesWithModules.value = normalizedCategories
                fetchModuleKpis(normalizedCategories.map { it.code })
            }
            dTimeline.await()?.let { _invoiceTimeline.value = it }
            dActivity.await()?.let { _activityTimeline.value = it }
            dNotifs.await()?.let { _notifications.value = it }

            if (stats != null || dProfils.await() != null) {
                _isOffline.value = false
            } else if (!anySucceeded) {
                _isOffline.value = true
                log.warning("GroupeAdminDataRepository: all REST calls failed — offline mode")
            }
        } catch (e: Exception) {
            log.warning("GroupeAdminDataRepository.refreshAll error: ${e.message}")
            if (!anySucceeded) _isOffline.value = true
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun refreshUsersForEcole(schoolId: String) {
        val users = runCatching { client.listUsersByEcole(schoolId) }.getOrNull() ?: return
        _usersByEcole.value = _usersByEcole.value.toMutableMap().also { it[schoolId] = users }
    }

    suspend fun createEcole(dto: CreateEcoleDto): EcoleApiDto? {
        val result = runCatching { client.createEcole(groupeId, dto) }.getOrNull() ?: return null
        _ecoles.value = _ecoles.value + result
        return result
    }

    suspend fun createProfil(dto: CreateProfilDto): ProfilApiDto? {
        val result = runCatching { client.createProfil(groupeId, dto) }.getOrNull() ?: return null
        _profils.value = _profils.value + result
        return result
    }

    suspend fun createUser(dto: CreateUserGroupeDto): UserApiDto? =
        runCatching { client.createUser(groupeId, dto) }.getOrNull()

    suspend fun assignProfil(userId: String, dto: AssignProfilDto): Boolean =
        runCatching { client.assignProfil(groupeId, userId, dto) }.getOrDefault(false)

    suspend fun createSubscriptionRequest(dto: SubscriptionRequestDto): Boolean =
        runCatching { client.createSubscriptionRequest(groupeId, dto) }.getOrDefault(false)

    fun selectEcole(ecoleId: String?) {
        _selectedEcoleId.value = ecoleId
    }

    private suspend fun fetchModuleKpis(categoryCodes: List<String>) {
        val known = setOf("scolarisation", "finance", "rh")
        val targets = categoryCodes
            .map { code -> normalizeDashboardCategoryCode(code) }
            .filter { code -> code in known }
            .distinct()
        if (targets.isEmpty()) return
        val results = targets.map { code ->
            repoScope.async { code to runCatching { client.getModuleKpi(groupeId, code) }.getOrNull() }
        }.awaitAll()
        val updated = _moduleKpis.value.toMutableMap()
        results.forEach { (code, dto) -> if (dto != null) updated[code] = dto }
        _moduleKpis.value = updated
    }
}
