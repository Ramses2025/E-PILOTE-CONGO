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

    private val _messagingReloadTick = MutableStateFlow(0)
    val messagingReloadTick: StateFlow<Int> = _messagingReloadTick.asStateFlow()

    var lastEventId: String? = null

    fun refreshAllAsync() {
        launchRefreshOnce("all") { refreshAll() }
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

    suspend fun refreshAll() {
        log.info("[AdminRepo] refreshAll START")
        _isLoading.value = true
        try {
            coroutineScope {
                val dStats = async { runCatching { client.getDashboardStats() }.getOrNull() ?: DashboardStatsDto() }
                val dGroupes = async { runCatching { client.listGroupes() }.getOrNull().orEmpty() }
                val dPlans = async { runCatching { client.listPlans() }.getOrNull()?.map {
                    PlanDto(id = it.id, nom = it.nom, type = it.type, prixXAF = it.prixXAF,
                        currency = it.currency, maxStudents = it.maxStudents, maxPersonnel = it.maxPersonnel,
                        modulesIncluded = it.modulesIncluded, isActive = it.isActive)
                }.orEmpty() }
                val dModules = async { runCatching { client.listModules() }.getOrNull()?.map {
                    ModuleDto(it.id, it.code, it.nom, it.categorieCode, it.description, it.isCore, it.requiredPlan, it.isActive, it.ordre)
                }.orEmpty() }
                val dCategories = async { runCatching { client.listCategories() }.getOrNull()?.map {
                    CategorieDto(code = it.code, nom = it.nom, isCore = it.isCore, ordre = it.ordre, isActive = it.isActive)
                }.orEmpty() }
                val dAdmins = async { runCatching { client.listAllAdmins() }.getOrNull().orEmpty() }

                awaitAll(dStats, dGroupes, dPlans, dModules, dCategories, dAdmins)

                val stats = dStats.await()
                val groupesApi = dGroupes.await()
                val groupes = groupesApi.map { it.toGroupeDto() }
                val adminsApi = dAdmins.await()
                val adminsByGroup = groupesApi.associate { groupe ->
                    groupe.id to adminsApi
                        .asSequence()
                        .filter { it.role.equals("ADMIN_GROUPE", ignoreCase = true) && it.groupId == groupe.id }
                        .map { it.toUserDto() }
                        .toList()
                }

                _dashboardStats.value = stats
                _adminStats.value = stats.toAdminStats()
                _groupes.value = groupes
                _adminGroupAdmins.value = adminsByGroup
                _plans.value = dPlans.await()
                _modules.value = dModules.await()
                _categories.value = dCategories.await()
                _adminUsers.value = adminsApi.map { it.toAdminUserDto() }
            }
        } catch (e: Exception) {
            log.warning("[AdminRepo] refreshAll FAILED: ${e.message}")
        }
        _isLoading.value = false
        log.info("[AdminRepo] refreshAll DONE — groupes=${_groupes.value.size} plans=${_plans.value.size} admins=${_adminUsers.value.size}")
    }

    suspend fun refreshGroupes() {
        val groupesApi = runCatching { client.listGroupes() }.getOrNull().orEmpty()
        val adminsApi = runCatching { client.listAllAdmins() }.getOrNull().orEmpty()
        val groupes = groupesApi.map { it.toGroupeDto() }
        val adminsByGroup = groupesApi.associate { groupe ->
            groupe.id to adminsApi
                .asSequence()
                .filter { it.role.equals("ADMIN_GROUPE", ignoreCase = true) && it.groupId == groupe.id }
                .map { it.toUserDto() }
                .toList()
        }
        _groupes.value = groupes
        _adminGroupAdmins.value = adminsByGroup
        _adminUsers.value = adminsApi.map { it.toAdminUserDto() }
        refreshDashboardStats()
    }

    suspend fun refreshPlans() {
        val plans = runCatching { client.listPlans() }.getOrNull()?.map {
            PlanDto(id = it.id, nom = it.nom, type = it.type, prixXAF = it.prixXAF,
                currency = it.currency, maxStudents = it.maxStudents, maxPersonnel = it.maxPersonnel,
                modulesIncluded = it.modulesIncluded, isActive = it.isActive)
        }.orEmpty()
        _plans.value = plans
        refreshDashboardStats()
    }

    suspend fun refreshModules() {
        val modules = runCatching { client.listModules() }.getOrNull()?.map {
            ModuleDto(it.id, it.code, it.nom, it.categorieCode, it.description, it.isCore, it.requiredPlan, it.isActive, it.ordre)
        }.orEmpty()
        _modules.value = modules
        refreshDashboardStats()
    }

    suspend fun refreshCategories() {
        val categories = runCatching { client.listCategories() }.getOrNull()?.map {
            CategorieDto(code = it.code, nom = it.nom, isCore = it.isCore, ordre = it.ordre, isActive = it.isActive)
        }.orEmpty()
        _categories.value = categories
    }

    suspend fun refreshAdmins() {
        val adminsApi = runCatching { client.listAllAdmins() }.getOrNull().orEmpty()
        _adminUsers.value = adminsApi.map { it.toAdminUserDto() }
        refreshDashboardStats()
    }

    suspend fun refreshDashboardStats() {
        val stats = runCatching { client.getDashboardStats() }.getOrNull() ?: DashboardStatsDto()
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
            "invoice", "subscription", "announcement", "message" -> refreshDashboardStatsAsync()
            else -> refreshAllAsync()
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

    private fun GroupeApiDto.toGroupeDto() = GroupeDto(
        id = id, nom = nom, slug = slug, email = email, phone = phone,
        department = department, city = city, address = address, country = country,
        logo = logo, description = description, foundedYear = foundedYear,
        website = website, planId = planId, ecolesCount = ecolesCount,
        usersCount = usersCount, isActive = isActive, createdAt = createdAt
    )

    private fun PlanApiDto.toPlanDto() = PlanDto(
        id = id, nom = nom, type = type, prixXAF = prixXAF,
        currency = currency, maxStudents = maxStudents, maxPersonnel = maxPersonnel,
        modulesIncluded = modulesIncluded, isActive = isActive
    )

    private fun ModuleApiDto.toModuleDto() = ModuleDto(
        id, code, nom, categorieCode, description, isCore, requiredPlan, isActive, ordre
    )

    private fun CategorieApiDto.toCategorieDto() = CategorieDto(
        code = code, nom = nom, isCore = isCore, ordre = ordre, isActive = isActive
    )

    private fun AdminUserApiDto.toAdminUserDto() = AdminUserDto(
        id = id, username = username, firstName = firstName, lastName = lastName,
        email = email, phone = phone, role = role, status = status,
        gender = gender, dateOfBirth = dateOfBirth, groupId = groupId,
        schoolId = schoolId, avatar = avatar, address = address,
        birthPlace = birthPlace, mustChangePassword = mustChangePassword,
        lastLoginAt = lastLoginAt, loginAttempts = loginAttempts,
        isActive = isActive, createdAt = createdAt, updatedAt = updatedAt
    )

    private fun UserApiDto.toAdminUserDto() = AdminUserDto(
        id = id,
        username = username,
        firstName = firstName,
        lastName = lastName,
        email = email,
        phone = null,
        role = role,
        status = if (isActive) "active" else "inactive",
        gender = null,
        dateOfBirth = null,
        groupId = groupId,
        schoolId = schoolId,
        avatar = null,
        address = null,
        birthPlace = null,
        mustChangePassword = false,
        lastLoginAt = null,
        loginAttempts = 0,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = createdAt
    )

    private fun AdminUserApiDto.toUserDto() = UserDto(
        id = id, username = username, firstName = firstName, lastName = lastName,
        email = email, schoolId = schoolId ?: "", groupId = groupId ?: "",
        profilId = "", role = role, isActive = isActive, createdAt = createdAt
    )

    private fun UserApiDto.toUserDto() = UserDto(
        id = id, username = username, firstName = firstName, lastName = lastName,
        email = email, schoolId = schoolId ?: "", groupId = groupId,
        profilId = profilId ?: "", role = role, isActive = isActive, createdAt = createdAt
    )

    private fun AdminUserDto.toUserDto() = UserDto(
        id = id, username = username, firstName = firstName, lastName = lastName,
        email = email, schoolId = schoolId ?: "", groupId = groupId ?: "",
        profilId = "", role = role, isActive = isActive, createdAt = createdAt
    )
}
