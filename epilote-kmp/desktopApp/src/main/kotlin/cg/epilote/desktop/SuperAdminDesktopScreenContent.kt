package cg.epilote.desktop

import androidx.compose.runtime.Composable
import cg.epilote.desktop.data.DashboardStatsDto
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.ui.components.DesktopScreen
import cg.epilote.desktop.ui.screens.AdminAdminsScreen
import cg.epilote.desktop.ui.screens.AdminCategoriesScreen
import cg.epilote.desktop.ui.screens.AdminGroupesScreen
import cg.epilote.desktop.ui.screens.AdminInvoicesScreen
import cg.epilote.desktop.ui.screens.AdminUserDto
import cg.epilote.desktop.ui.screens.CategorieDto
import cg.epilote.desktop.ui.screens.GroupeDto
import cg.epilote.desktop.ui.screens.ModuleDto
import cg.epilote.desktop.ui.screens.PlanDto
import cg.epilote.desktop.ui.screens.SuperAdminDashboardScreen
import cg.epilote.desktop.ui.screens.UserDto
import cg.epilote.desktop.ui.screens.superadmin.AdminStats
import cg.epilote.shared.domain.model.UserSession
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun SuperAdminDesktopScreenContent(
    currentScreen: DesktopScreen,
    session: UserSession,
    adminStats: AdminStats,
    dashboardStatsDto: DashboardStatsDto,
    adminGroupes: List<GroupeDto>,
    adminGroupAdmins: Map<String, List<UserDto>>,
    adminPlans: List<PlanDto>,
    adminModules: List<ModuleDto>,
    adminCategories: List<CategorieDto>,
    adminUsers: List<AdminUserDto>,
    adminLoading: Boolean,
    appScope: CoroutineScope,
    adminClient: DesktopAdminClient,
    onLoadAdminData: (Boolean) -> Unit,
    onScreenChange: (DesktopScreen) -> Unit
) {
    when (currentScreen) {
        DesktopScreen.ADMIN_DASHBOARD ->
            SuperAdminDashboardScreen(
                session = session,
                stats = adminStats,
                isLoading = adminLoading,
                onNavigateGroupes = { onScreenChange(DesktopScreen.ADMIN_GROUPES) },
                onNavigatePlans = { onScreenChange(DesktopScreen.ADMIN_PLANS) },
                onNavigateModules = { onScreenChange(DesktopScreen.ADMIN_MODULES) },
                onRefresh = { onLoadAdminData(false) }
            )

        DesktopScreen.ADMIN_GROUPES ->
            AdminGroupesScreen(
                groupes = adminGroupes,
                adminGroupesByGroup = adminGroupAdmins,
                plans = adminPlans,
                totalEcoles = dashboardStatsDto.totalEcoles.toInt(),
                totalUtilisateurs = dashboardStatsDto.totalUtilisateurs.toInt(),
                isLoading = adminLoading,
                onCreateGroupe = { dto, onResult -> appScope.createGroupeAndRefresh(adminClient, dto, { onLoadAdminData(false) }, onResult) },
                onUpdateGroupe = { gId, dto, onResult -> appScope.updateGroupeAndRefresh(adminClient, gId, dto, { onLoadAdminData(false) }, onResult) },
                onDeleteGroupe = { gId, onResult -> appScope.deleteGroupeAndRefresh(adminClient, gId, { onLoadAdminData(false) }, onResult) },
                onToggleGroupeStatus = { gId, active, onResult -> appScope.toggleGroupeStatusAndRefresh(adminClient, gId, active, { onLoadAdminData(false) }, onResult) },
                onCreateAdminGroupe = { gId, pw, n, p, e, onResult -> appScope.createAdminGroupeAndRefresh(adminClient, gId, pw, n, p, e, { onLoadAdminData(false) }, onResult) },
                onRefresh = { onLoadAdminData(true) }
            )

        DesktopScreen.ADMIN_PLANS ->
            cg.epilote.desktop.ui.screens.AdminPlansScreen(
                plans = adminPlans,
                modules = adminModules,
                isLoading = adminLoading,
                scope = appScope,
                client = adminClient,
                onRefresh = { onLoadAdminData(false) }
            )

        DesktopScreen.ADMIN_MODULES ->
            cg.epilote.desktop.ui.screens.AdminModulesScreen(
                modules = adminModules,
                categories = adminCategories,
                isLoading = adminLoading,
                onCreateModule = { dto, onResult -> appScope.createModuleAndRefresh(adminClient, dto, { onLoadAdminData(false) }, onResult) },
                onUpdateModule = { moduleId, dto, onResult -> appScope.updateModuleAndRefresh(adminClient, moduleId, dto, { onLoadAdminData(false) }, onResult) },
                onToggleModuleStatus = { moduleId, active, onResult -> appScope.toggleModuleStatusAndRefresh(adminClient, moduleId, active, { onLoadAdminData(false) }, onResult) },
                onRefresh = { onLoadAdminData(true) }
            )

        DesktopScreen.ADMIN_CATEGORIES ->
            AdminCategoriesScreen(
                categories = adminCategories,
                modules = adminModules,
                isLoading = adminLoading,
                onCreateCategory = { dto, onResult -> appScope.createCategoryAndRefresh(adminClient, dto, { onLoadAdminData(false) }, onResult) },
                onUpdateCategory = { code, dto, onResult -> appScope.updateCategoryAndRefresh(adminClient, code, dto, { onLoadAdminData(false) }, onResult) },
                onToggleCategoryStatus = { code, active, onResult -> appScope.toggleCategoryStatusAndRefresh(adminClient, code, active, { onLoadAdminData(false) }, onResult) },
                onRefresh = { onLoadAdminData(true) }
            )

        DesktopScreen.ADMIN_SUBSCRIPTIONS ->
            cg.epilote.desktop.ui.screens.AdminSubscriptionsScreen(
                groupes = adminGroupes,
                plans = adminPlans,
                isLoading = adminLoading,
                scope = appScope,
                client = adminClient,
                onRefresh = { onLoadAdminData(false) }
            )

        DesktopScreen.ADMIN_INVOICES ->
            AdminInvoicesScreen(
                groupes = adminGroupes,
                plans = adminPlans,
                isLoading = adminLoading,
                scope = appScope,
                client = adminClient,
                onRefresh = { onLoadAdminData(false) }
            )

        DesktopScreen.ADMIN_ADMINISTRATORS ->
            AdminAdminsScreen(
                admins = adminUsers,
                groupes = adminGroupes,
                isLoading = adminLoading,
                onCreateAdmin = { dto, onResult -> appScope.createAdminAndRefresh(adminClient, dto, { onLoadAdminData(false) }, onResult) },
                onUpdateAdmin = { uId, dto, onResult -> appScope.updateAdminAndRefresh(adminClient, uId, dto, { onLoadAdminData(false) }, onResult) },
                onDeleteAdmin = { uId, onResult -> appScope.deleteAdminAndRefresh(adminClient, uId, { onLoadAdminData(false) }, onResult) },
                onToggleAdminStatus = { uId, status, onResult -> appScope.toggleAdminStatusAndRefresh(adminClient, uId, status, { onLoadAdminData(false) }, onResult) },
                onRefresh = { onLoadAdminData(true) }
            )

        DesktopScreen.ADMIN_ANNOUNCEMENTS ->
            PlaceholderScreen("Annonces", "Diffusion d'annonces vers tous les groupes")

        DesktopScreen.ADMIN_NOTIFICATIONS ->
            PlaceholderScreen("Notifications", "Notifications plateforme — alertes et rappels")

        DesktopScreen.ADMIN_MESSAGING ->
            PlaceholderScreen("Messagerie", "Messagerie interne plateforme")

        DesktopScreen.ADMIN_TICKETS ->
            PlaceholderScreen("Signalements", "Tickets de support et signalements")

        DesktopScreen.ADMIN_AUDIT_LOG ->
            cg.epilote.desktop.ui.screens.AuditLogScreen()

        else -> Unit
    }
}
