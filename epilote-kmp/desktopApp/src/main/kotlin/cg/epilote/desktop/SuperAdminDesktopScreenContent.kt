package cg.epilote.desktop

import androidx.compose.runtime.Composable
import cg.epilote.desktop.data.AdminDataRepository
import cg.epilote.desktop.data.DashboardStatsDto
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.ui.components.DesktopScreen
import cg.epilote.desktop.ui.screens.AdminAdminsScreen
import cg.epilote.desktop.ui.screens.AdminMessagingMailbox
import cg.epilote.desktop.ui.screens.AdminCategoriesScreen
import cg.epilote.desktop.ui.screens.AdminGroupesScreen
import cg.epilote.desktop.ui.screens.AdminSupportScreen
import cg.epilote.desktop.ui.screens.AdminInvoicesScreen
import cg.epilote.desktop.ui.screens.AdminMessagingScreen
import cg.epilote.desktop.ui.screens.AdminNotificationsScreen
import cg.epilote.desktop.ui.screens.AdminPlatformSettingsScreen
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
    adminRepo: AdminDataRepository,
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
                onNavigateInvoices = { onScreenChange(DesktopScreen.ADMIN_INVOICES) },
                onNavigateNotifications = { onScreenChange(DesktopScreen.ADMIN_NOTIFICATIONS) },
                onNavigateAnnouncements = { onScreenChange(DesktopScreen.ADMIN_ANNOUNCEMENTS) },
                onRefresh = { adminRepo.refreshAllAsync() }
            )

        DesktopScreen.ADMIN_GROUPES ->
            AdminGroupesScreen(
                groupes = adminGroupes,
                adminGroupesByGroup = adminGroupAdmins,
                plans = adminPlans,
                totalEcoles = dashboardStatsDto.totalEcoles.toInt(),
                totalUtilisateurs = dashboardStatsDto.totalUtilisateurs.toInt(),
                isLoading = adminLoading,
                onCreateGroupe = { dto, onResult -> appScope.createGroupeAndRefresh(adminClient, adminRepo, dto, onResult) },
                onUpdateGroupe = { gId, dto, onResult -> appScope.updateGroupeAndRefresh(adminClient, adminRepo, gId, dto, onResult) },
                onDeleteGroupe = { gId, onResult -> appScope.deleteGroupeAndRefresh(adminClient, adminRepo, gId, onResult) },
                onToggleGroupeStatus = { gId, active, onResult -> appScope.toggleGroupeStatusAndRefresh(adminClient, adminRepo, gId, active, onResult) },
                onCreateAdminGroupe = { gId, pw, n, p, e, onResult -> appScope.createAdminGroupeAndRefresh(adminClient, adminRepo, gId, pw, n, p, e, onResult) },
                onRefresh = { adminRepo.refreshGroupesAsync() }
            )

        DesktopScreen.ADMIN_PLANS ->
            cg.epilote.desktop.ui.screens.AdminPlansScreen(
                plans = adminPlans,
                modules = adminModules,
                isLoading = adminLoading,
                scope = appScope,
                client = adminClient,
                adminRepo = adminRepo,
                onRefresh = { adminRepo.refreshPlansAsync() }
            )

        DesktopScreen.ADMIN_MODULES ->
            cg.epilote.desktop.ui.screens.AdminModulesScreen(
                modules = adminModules,
                categories = adminCategories,
                isLoading = adminLoading,
                onCreateModule = { dto, onResult -> appScope.createModuleAndRefresh(adminClient, adminRepo, dto, onResult) },
                onUpdateModule = { moduleId, dto, onResult -> appScope.updateModuleAndRefresh(adminClient, adminRepo, moduleId, dto, onResult) },
                onToggleModuleStatus = { moduleId, active, onResult -> appScope.toggleModuleStatusAndRefresh(adminClient, adminRepo, moduleId, active, onResult) },
                onRefresh = { adminRepo.refreshModulesAsync() }
            )

        DesktopScreen.ADMIN_CATEGORIES ->
            AdminCategoriesScreen(
                categories = adminCategories,
                modules = adminModules,
                isLoading = adminLoading,
                onCreateCategory = { dto, onResult -> appScope.createCategoryAndRefresh(adminClient, adminRepo, dto, onResult) },
                onUpdateCategory = { code, dto, onResult -> appScope.updateCategoryAndRefresh(adminClient, adminRepo, code, dto, onResult) },
                onToggleCategoryStatus = { code, active, onResult -> appScope.toggleCategoryStatusAndRefresh(adminClient, adminRepo, code, active, onResult) },
                onRefresh = { adminRepo.refreshCategoriesAsync() }
            )

        DesktopScreen.ADMIN_SUBSCRIPTIONS ->
            cg.epilote.desktop.ui.screens.AdminSubscriptionsScreen(
                groupes = adminGroupes,
                plans = adminPlans,
                isLoading = adminLoading,
                scope = appScope,
                client = adminClient,
                onRefresh = { adminRepo.refreshDashboardStatsAsync() }
            )

        DesktopScreen.ADMIN_INVOICES ->
            AdminInvoicesScreen(
                groupes = adminGroupes,
                plans = adminPlans,
                isLoading = adminLoading,
                scope = appScope,
                client = adminClient,
                onRefresh = { adminRepo.refreshDashboardStatsAsync() }
            )

        DesktopScreen.ADMIN_ADMINISTRATORS ->
            AdminAdminsScreen(
                admins = adminUsers,
                groupes = adminGroupes,
                isLoading = adminLoading,
                onCreateAdmin = { dto, onResult -> appScope.createAdminAndRefresh(adminClient, adminRepo, dto, onResult) },
                onUpdateAdmin = { uId, dto, onResult -> appScope.updateAdminAndRefresh(adminClient, adminRepo, uId, dto, onResult) },
                onDeleteAdmin = { uId, onResult -> appScope.deleteAdminAndRefresh(adminClient, adminRepo, uId, onResult) },
                onToggleAdminStatus = { uId, status, onResult -> appScope.toggleAdminStatusAndRefresh(adminClient, adminRepo, uId, status, onResult) },
                onRefresh = { adminRepo.refreshAdminsAsync() }
            )

        DesktopScreen.ADMIN_ANNOUNCEMENTS ->
            AdminMessagingScreen(
                session = session,
                groupes = adminGroupes,
                admins = adminUsers,
                isLoading = adminLoading,
                scope = appScope,
                client = adminClient,
                onRefresh = { adminRepo.refreshDashboardStatsAsync() },
                initialMailbox = AdminMessagingMailbox.ANNOUNCEMENTS
            )

        DesktopScreen.ADMIN_NOTIFICATIONS ->
            AdminNotificationsScreen(
                groupes = adminGroupes,
                adminGroupesByGroup = adminGroupAdmins,
                admins = adminUsers,
                isLoading = adminLoading,
                scope = appScope,
                client = adminClient,
                onRefresh = { adminRepo.refreshDashboardStatsAsync() }
            )

        DesktopScreen.ADMIN_MESSAGING ->
            AdminMessagingScreen(
                session = session,
                groupes = adminGroupes,
                admins = adminUsers,
                isLoading = adminLoading,
                scope = appScope,
                client = adminClient,
                onRefresh = { adminRepo.refreshDashboardStatsAsync() }
            )

        DesktopScreen.ADMIN_TICKETS ->
            AdminSupportScreen(
                groupes = adminGroupes,
                adminGroupesByGroup = adminGroupAdmins,
                admins = adminUsers,
                isLoading = adminLoading,
                scope = appScope,
                client = adminClient,
                onRefresh = { adminRepo.refreshDashboardStatsAsync() }
            )

        DesktopScreen.ADMIN_AUDIT_LOG ->
            cg.epilote.desktop.ui.screens.AdminAuditScreen(
                groupes = adminGroupes,
                admins = adminUsers,
                isLoading = adminLoading,
                client = adminClient,
                onRefresh = { adminRepo.refreshDashboardStatsAsync() }
            )

        DesktopScreen.ADMIN_PLATFORM_SETTINGS ->
            AdminPlatformSettingsScreen(client = adminClient)

        else -> Unit
    }
}
