package cg.epilote.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.*
import cg.epilote.desktop.ui.components.AppHeader
import cg.epilote.desktop.ui.components.DesktopScreen
import cg.epilote.desktop.ui.components.Sidebar
import cg.epilote.desktop.ui.screens.*
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.EpiloteTheme
import cg.epilote.shared.data.local.*
import cg.epilote.shared.data.remote.ApiClient
import cg.epilote.shared.data.remote.AuthApiService
import cg.epilote.shared.data.sync.SyncManager
import cg.epilote.shared.domain.model.BulletinEleve
import cg.epilote.shared.domain.model.UserSession
import cg.epilote.shared.domain.usecase.absences.JustifyAbsenceUseCase
import cg.epilote.shared.domain.usecase.absences.SaveAbsenceUseCase
import cg.epilote.shared.domain.usecase.auth.LoginUseCase
import cg.epilote.shared.domain.usecase.notes.LockBulletinUseCase
import cg.epilote.shared.domain.usecase.notes.ResolveConflictUseCase
import cg.epilote.shared.presentation.viewmodel.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun EpiloteDesktopApp() {
    var session by remember { mutableStateOf<UserSession?>(null) }
    var currentScreen by remember { mutableStateOf(DesktopScreen.DASHBOARD) }
    val isSuperAdmin   = session?.role == "SUPER_ADMIN"
    val isAdminGroupe  = session?.role == "ADMIN_GROUPE"

    EpiloteTheme {
        // ── Session DB (always needed) ──
        val sessionsDb = remember {
            if (!EpiloteDatabase.isSessionsOpen()) EpiloteDatabase.initSessions(null)
            EpiloteDatabase.sessionsInstance
        }
        val sessionRepo = remember { UserSessionRepository(sessionsDb) }

        // ── Auto-login: restore session if rememberMe was set AND token not expired ──
        if (session == null) {
            val restoredSession = remember {
                sessionRepo.getSession()?.takeIf { s ->
                    s.rememberMe && s.offlineTokenExpiresAt > System.currentTimeMillis()
                }
            }
            if (restoredSession != null) {
                if (!EpiloteDatabase.isDataOpen()) {
                    EpiloteDatabase.initUserData(null, restoredSession.userId)
                }
                session = restoredSession
                currentScreen = when (restoredSession.role) {
                    "SUPER_ADMIN"  -> DesktopScreen.ADMIN_DASHBOARD
                    "ADMIN_GROUPE" -> DesktopScreen.GROUPE_DASHBOARD
                    else           -> DesktopScreen.DASHBOARD
                }
            } else {
                // Clear any stale/expired session
                remember { sessionRepo.clearSession(); true }
            }
        }

        if (session == null) {
            val apiClient = remember {
                ApiClient(
                    baseUrl        = desktopBackendBaseUrl,
                    tokenProvider  = { null },
                    onTokenExpired = {}
                )
            }
            val syncManager    = remember {
                SyncManager("wss://x0by7zekx39pidsy.apps.cloud.couchbase.com:4984/epilote")
            }
            val authService    = remember { AuthApiService(apiClient) }
            val loginUseCase   = remember { LoginUseCase(authService, sessionRepo, syncManager, null) }
            val loginViewModel = remember { LoginViewModel(loginUseCase) }

            LoginScreen(
                viewModel      = loginViewModel,
                onLoginSuccess = { s ->
                    session = s
                    currentScreen = when (s.role) {
                        "SUPER_ADMIN"  -> DesktopScreen.ADMIN_DASHBOARD
                        "ADMIN_GROUPE" -> DesktopScreen.GROUPE_DASHBOARD
                        else           -> DesktopScreen.DASHBOARD
                    }
                }
            )
        } else if (session!!.mustChangePassword) {
            // ── Mot de passe initial à usage unique ──────────────────
            // Le backend a indiqué `mustChangePassword=true` sur la LoginResponse :
            // on bloque l'accès aux écrans applicatifs jusqu'à ce que l'utilisateur
            // ait défini un nouveau mot de passe (politique de mot de passe initial
            // côté Super Admin). Aucun chargement de données admin n'est lancé
            // tant que cette étape n'est pas franchie : on monte donc un client
            // admin minimal dédié uniquement à la route /api/auth/change-password.
            //
            // Sur 401 (token expiré après auto-login depuis la session
            // persistée), on tente un refresh ; en cas d'échec on efface la
            // session locale et on retombe sur LoginScreen — sans cela le
            // dialogue forcé non-fermable restait verrouillé indéfiniment.
            val s = session!!
            val forcedUnauthorizedHandler = remember(sessionRepo) {
                buildDesktopAdminUnauthorizedHandler(desktopBackendBaseUrl, sessionRepo) { refreshedSession ->
                    session = refreshedSession
                }
            }
            val forcedTokenProvider = remember(sessionRepo) {
                buildDesktopAdminTokenProvider(sessionRepo) { session }
            }
            val forcedAdminClient = remember(sessionRepo) {
                DesktopAdminClient(
                    baseUrl = desktopBackendBaseUrl,
                    tokenProvider = forcedTokenProvider,
                    onUnauthorized = forcedUnauthorizedHandler
                )
            }
            ForcedChangePasswordDialog(
                adminClient = forcedAdminClient,
                onPasswordChanged = {
                    // Relit la session courante pour ne pas écraser un éventuel
                    // refresh de token déclenché entre-temps.
                    val current = sessionRepo.getSession() ?: s
                    val cleared = current.copy(mustChangePassword = false)
                    sessionRepo.saveSession(cleared)
                    session = cleared
                    currentScreen = when (cleared.role) {
                        "SUPER_ADMIN"  -> DesktopScreen.ADMIN_DASHBOARD
                        "ADMIN_GROUPE" -> DesktopScreen.GROUPE_DASHBOARD
                        else           -> DesktopScreen.DASHBOARD
                    }
                }
            )
        } else {
            val s  = session!!
            val db = remember(s.userId) {
                if (!EpiloteDatabase.isDataOpen()) {
                    EpiloteDatabase.initUserData(null, s.userId)
                }
                EpiloteDatabase.instance
            }

            val noteRepo    = remember { NoteRepository(db) }
            val classeRepo  = remember { ClasseRepository(db) }
            val matiereRepo = remember { MatiereRepository(db) }
            val absenceRepo = remember { AbsenceRepository(db) }
            val eleveRepo   = remember { EleveRepository(db) }

            val syncManager = remember {
                SyncManager("wss://x0by7zekx39pidsy.apps.cloud.couchbase.com:4984/epilote")
            }

            val aiClient = remember {
                DesktopAIClient(desktopBackendBaseUrl) { s.accessToken }
            }

            val appScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

            val adminUnauthorizedHandler = remember(sessionRepo) {
                buildDesktopAdminUnauthorizedHandler(desktopBackendBaseUrl, sessionRepo) { refreshedSession ->
                    session = refreshedSession
                }
            }

            val adminClient = remember {
                DesktopAdminClient(
                    baseUrl = desktopBackendBaseUrl,
                    tokenProvider = buildDesktopAdminTokenProvider(sessionRepo) { session },
                    onUnauthorized = adminUnauthorizedHandler
                )
            }

            val adminRepo = remember { AdminDataRepository(adminClient) }

            val groupId = s.groupId
            val groupeClient = remember(groupId) {
                if (groupId != null) DesktopGroupeClient(
                    baseUrl        = desktopBackendBaseUrl,
                    tokenProvider  = buildDesktopAdminTokenProvider(sessionRepo) { session },
                    onUnauthorized = adminUnauthorizedHandler
                ) else null
            }
            val groupeRepo = remember(groupId) {
                if (groupId != null) GroupeAdminDataRepository(groupeClient!!, groupId)
                else null
            }

            val adminRealtimeClient = remember {
                AdminRealtimeClient(
                    baseUrl = desktopBackendBaseUrl,
                    tokenProvider = buildDesktopAdminTokenProvider(sessionRepo) { session },
                    onUnauthorized = adminUnauthorizedHandler,
                    lastEventIdProvider = { adminRepo.lastEventId },
                    onReconnectNeeded = { adminRepo.refreshAll() }
                )
            }

            // ── Admin state from repository ───────────────────────────
            val adminStats by adminRepo.adminStats.collectAsState()
            val dashboardStatsDto by adminRepo.dashboardStats.collectAsState()
            val adminGroupes by adminRepo.groupes.collectAsState()
            val adminGroupAdmins by adminRepo.adminGroupAdmins.collectAsState()
            val adminPlans by adminRepo.plans.collectAsState()
            val adminModules by adminRepo.modules.collectAsState()
            val adminCategories by adminRepo.categories.collectAsState()
            val adminUsers by adminRepo.adminUsers.collectAsState()
            val adminLoading by adminRepo.isLoading.collectAsState()
            val adminRawSubscriptions by adminRepo.rawSubscriptions.collectAsState()
            val adminRawInvoices by adminRepo.rawInvoices.collectAsState()
            val adminMessages by adminRepo.messages.collectAsState()
            val adminAnnouncements by adminRepo.announcements.collectAsState()
            val adminAuditLogs by adminRepo.auditLogs.collectAsState()
            val adminAuditTotal by adminRepo.auditTotal.collectAsState()
            val adminPaymentReceipts by adminRepo.paymentReceipts.collectAsState()
            var sidebarExpanded by remember { mutableStateOf(true) }
            val groupeDynamicCategories by (groupeRepo?.categoriesWithModules
                ?: MutableStateFlow(emptyList<CategorieWithModulesDto>())).collectAsState()

            // ── Initial data load + periodic refresh ──
            LaunchedEffect(isSuperAdmin) {
                if (isSuperAdmin) {
                    // Always refresh token before first load (access token may be expired)
                    val refreshed = refreshDesktopAdminSession(desktopBackendBaseUrl, sessionRepo) { session = it }
                    if (!refreshed) {
                        // Refresh failed — force re-login
                        session = null
                        return@LaunchedEffect
                    }
                    adminRepo.refreshAll()
                    while (true) {
                        delay(60_000)
                        adminRepo.refreshAll(showLoading = false)
                    }
                }
            }

            LaunchedEffect(isAdminGroupe) {
                if (isAdminGroupe && groupeRepo != null) {
                    val refreshed = refreshDesktopAdminSession(desktopBackendBaseUrl, sessionRepo) { session = it }
                    if (!refreshed) { session = null; return@LaunchedEffect }
                    groupeRepo.refreshAll()
                    while (true) {
                        delay(120_000)
                        groupeRepo.refreshAll(showLoading = false)
                    }
                }
            }

            // ── SSE real-time updates ──
            SuperAdminRealtimeEffect(
                enabled = isSuperAdmin,
                client = adminRealtimeClient,
                currentUserId = s.userId,
                onEvent = { event ->
                    // Self-originated events: patch locally (mutation helpers already did it)
                    // Other users' events: apply payload or refresh
                    adminRepo.onSseEvent(event)
                }
            )

            val syncVm      = remember { SyncIndicatorViewModel(syncManager) }
            val classesVm   = remember { ClassesViewModel(classeRepo, matiereRepo) }
            val notesVm     = remember { NotesViewModel(noteRepo, syncManager) }
            val absencesVm  = remember {
                AbsencesViewModel(
                    absenceRepo,
                    SaveAbsenceUseCase(absenceRepo),
                    JustifyAbsenceUseCase(absenceRepo)
                )
            }
            val bulletinVm  = remember {
                BulletinViewModel(eleveRepo, noteRepo, matiereRepo, absenceRepo, LockBulletinUseCase(noteRepo))
            }
            val resolveConflictUseCase = remember { ResolveConflictUseCase(noteRepo) }

            var appreciationResult by remember { mutableStateOf<AppreciationResult?>(null) }
            var isAILoading by remember { mutableStateOf(false) }

            val logoutAction = {
                syncManager.stop()
                sessionRepo.clearSession()
                EpiloteDatabase.closeUserData()
                currentScreen = DesktopScreen.DASHBOARD
                session = null
            }

            Row(modifier = Modifier.fillMaxSize()) {
                Sidebar(
                    session            = s,
                    currentScreen      = currentScreen,
                    onScreenSelected   = { currentScreen = it },
                    isExpanded         = sidebarExpanded,
                    onToggleExpanded   = { sidebarExpanded = !sidebarExpanded },
                    dynamicCategories  = groupeDynamicCategories
                )

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    // ── Header fixe ──
                    AppHeader(
                        session       = s,
                        currentScreen = currentScreen,
                        syncViewModel = syncVm,
                        onLogout      = logoutAction,
                        onNavigate    = { currentScreen = it }
                    )

                    // ── Contenu de la page ──
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (currentScreen.requiredRole == "SUPER_ADMIN") {
                            SuperAdminDesktopScreenContent(
                                currentScreen = currentScreen,
                                session = s,
                                adminStats = adminStats,
                                dashboardStatsDto = dashboardStatsDto,
                                adminGroupes = adminGroupes,
                                adminGroupAdmins = adminGroupAdmins,
                                adminPlans = adminPlans,
                                adminModules = adminModules,
                                adminCategories = adminCategories,
                                adminUsers = adminUsers,
                                adminLoading = adminLoading,
                                adminRawSubscriptions = adminRawSubscriptions,
                                adminRawInvoices = adminRawInvoices,
                                adminMessages = adminMessages,
                                adminAnnouncements = adminAnnouncements,
                                adminAuditLogs = adminAuditLogs,
                                adminAuditTotal = adminAuditTotal,
                                adminPaymentReceipts = adminPaymentReceipts,
                                appScope = appScope,
                                adminClient = adminClient,
                                adminRepo = adminRepo,
                                onScreenChange = { currentScreen = it }
                            )
                        } else {
                            NonAdminScreenContent(
                                currentScreen          = currentScreen,
                                session                = s,
                                groupeRepo             = groupeRepo,
                                classesVm              = classesVm,
                                notesVm                = notesVm,
                                absencesVm             = absencesVm,
                                bulletinVm             = bulletinVm,
                                appScope               = appScope,
                                aiClient               = aiClient,
                                noteRepo               = noteRepo,
                                resolveConflictUseCase = resolveConflictUseCase,
                                onScreenChange         = { currentScreen = it },
                                appreciationResult     = appreciationResult,
                                onAppreciationResult   = { appreciationResult = it },
                                onAILoadingChange      = { isAILoading = it }
                            )
                        }
                    }
                }
            }
        }
    }
}
