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
import cg.epilote.desktop.ui.screens.superadmin.AdminStats
import cg.epilote.desktop.ui.screens.superadmin.toAdminStats
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.EpiloteTheme
import kotlinx.coroutines.SupervisorJob
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val desktopBackendBaseUrl: String
    get() = System.getProperty("epilote.backend.url")
        ?: System.getenv("EPILOTE_BACKEND_URL")
        ?: "http://localhost:8080"

@Composable
fun EpiloteDesktopApp() {
    var session by remember { mutableStateOf<UserSession?>(null) }
    var currentScreen by remember { mutableStateOf(DesktopScreen.DASHBOARD) }
    val isSuperAdmin = session?.role == "SUPER_ADMIN"

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

            val adminClient = remember {
                DesktopAdminClient(
                    baseUrl = desktopBackendBaseUrl,
                    tokenProvider = buildDesktopAdminTokenProvider(sessionRepo) { session },
                    onUnauthorized = {
                        val refreshed = refreshDesktopAdminSession(desktopBackendBaseUrl, sessionRepo) { refreshedSession ->
                            session = refreshedSession
                        }
                        if (!refreshed) {
                            sessionRepo.clearSession()
                            session = null
                        }
                        refreshed
                    }
                )
            }

            // ── Admin state ──────────────────────────────────────────
            var adminStats by remember { mutableStateOf(AdminStats()) }
            var dashboardStatsDto by remember { mutableStateOf(DashboardStatsDto()) }
            var adminGroupes by remember { mutableStateOf<List<GroupeDto>>(emptyList()) }
            var adminGroupAdmins by remember { mutableStateOf<Map<String, List<UserDto>>>(emptyMap()) }
            var adminPlans by remember { mutableStateOf<List<PlanDto>>(emptyList()) }
            var adminModules by remember { mutableStateOf<List<ModuleDto>>(emptyList()) }
            var adminCategories by remember { mutableStateOf<List<CategorieDto>>(emptyList()) }
            var adminUsers by remember { mutableStateOf<List<AdminUserDto>>(emptyList()) }
            var adminLoading by remember { mutableStateOf(false) }
            var sidebarExpanded by remember { mutableStateOf(true) }

            fun loadAdminData(isInitial: Boolean = false) {
                if (isInitial) adminLoading = true
                appScope.launch {
                    try {
                        val snapshot = adminClient.loadAdminSnapshot()
                        println(
                            "loadAdminData snapshot groupes=${snapshot.groupes.size}, plans=${snapshot.plans.size}, modules=${snapshot.modules.size}, admins=${snapshot.adminUsers.size}"
                        )
                        dashboardStatsDto = snapshot.dashboardStats
                        adminStats = snapshot.adminStats
                        val canAcceptEmptyGroupes = snapshot.dashboardStats.totalGroupes == 0L
                        if (snapshot.groupes.isNotEmpty() || adminGroupes.isEmpty() || canAcceptEmptyGroupes) {
                            adminGroupes = snapshot.groupes
                        }
                        if (snapshot.adminGroupAdmins.isNotEmpty() || adminGroupAdmins.isEmpty()) {
                            adminGroupAdmins = snapshot.adminGroupAdmins
                        }
                        if (snapshot.plans.isNotEmpty() || adminPlans.isEmpty()) {
                            adminPlans = snapshot.plans
                        }
                        if (snapshot.modules.isNotEmpty() || adminModules.isEmpty()) {
                            adminModules = snapshot.modules
                        }
                        if (snapshot.categories.isNotEmpty() || adminCategories.isEmpty()) {
                            adminCategories = snapshot.categories
                        }
                        if (snapshot.adminUsers.isNotEmpty() || adminUsers.isEmpty()) {
                            adminUsers = snapshot.adminUsers
                        }
                    } catch (_: Exception) {
                        // partial load failure — keep existing data
                    }
                    adminLoading = false
                }
            }

            LaunchedEffect(isSuperAdmin) {
                if (isSuperAdmin) {
                    loadAdminData(isInitial = true)
                    while (true) {
                        delay(30_000)
                        loadAdminData()
                    }
                }
            }

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
                    session          = s,
                    currentScreen    = currentScreen,
                    onScreenSelected = { currentScreen = it },
                    isExpanded       = sidebarExpanded,
                    onToggleExpanded = { sidebarExpanded = !sidebarExpanded }
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
                                appScope = appScope,
                                adminClient = adminClient,
                                onLoadAdminData = { loadAdminData(isInitial = it) },
                                onScreenChange = { currentScreen = it }
                            )
                        } else when (currentScreen) {
                            DesktopScreen.GROUPE_DASHBOARD ->
                                PlaceholderScreen("Dashboard Groupe", "Vue d'ensemble du groupe scolaire")

                            DesktopScreen.GROUPE_ECOLES ->
                                PlaceholderScreen("Écoles", "Gestion des écoles du groupe")

                            DesktopScreen.GROUPE_UTILISATEURS ->
                                PlaceholderScreen("Utilisateurs", "Gestion de tous les utilisateurs du groupe")

                            DesktopScreen.GROUPE_PROFILS ->
                                PlaceholderScreen("Profils d'accès", "Gestion des profils et permissions du groupe")

                            // ── Regular Screens ──────────────────────
                            DesktopScreen.DASHBOARD ->
                                DashboardScreen(s)

                            DesktopScreen.CLASSES ->
                                ClassesScreen(s, classesVm)

                            DesktopScreen.NOTES ->
                                NotesScreen(s, classesVm, notesVm)

                            DesktopScreen.ABSENCES ->
                                AbsencesScreen(s, absencesVm)

                            DesktopScreen.BULLETINS ->
                                BulletinScreen(
                                    session               = s,
                                    classesViewModel      = classesVm,
                                    bulletinViewModel     = bulletinVm,
                                    appreciationResult    = appreciationResult,
                                    onDismissAppreciation = { appreciationResult = null },
                                    onRequestAppreciation = { bulletin ->
                                        isAILoading = true
                                        appScope.launch {
                                            val resp = aiClient.generateAppreciation(
                                                AIAppreciationRequestDto(
                                                    eleveNom        = "${bulletin.eleveNom} ${bulletin.elevePrenom}",
                                                    moyenneGenerale = bulletin.moyenneGenerale,
                                                    rang            = bulletin.rang,
                                                    effectif        = bulletin.totalEleves,
                                                    absences        = bulletin.absencesCount
                                                )
                                            )
                                            appreciationResult = if (resp != null) {
                                                AppreciationResult(
                                                    eleveNom     = "${bulletin.eleveNom} ${bulletin.elevePrenom}",
                                                    appreciation = resp.appreciation,
                                                    mention      = resp.mention,
                                                    conseil      = resp.conseil,
                                                    fallback     = resp.fallback
                                                )
                                            } else {
                                                AppreciationResult(
                                                    eleveNom     = "${bulletin.eleveNom} ${bulletin.elevePrenom}",
                                                    appreciation = "Service IA indisponible. Veuillez réessayer.",
                                                    mention      = "—",
                                                    conseil      = "",
                                                    fallback     = true
                                                )
                                            }
                                            isAILoading = false
                                        }
                                    }
                                )

                            DesktopScreen.CAHIER ->
                                CahierTextesScreen(
                                    session                = s,
                                    onRequestGenerateContent = { titre, niveau, matiere, type ->
                                        appScope.launch {
                                            aiClient.generateContent(
                                                AIContentRequestDto(
                                                    titre  = titre,
                                                    niveau = niveau,
                                                    matiere = matiere,
                                                    type   = type
                                                )
                                            )
                                        }
                                    }
                                )

                            DesktopScreen.CONFLICTS ->
                                ConflictsScreen(
                                    session                = s,
                                    noteRepo               = noteRepo,
                                    resolveConflictUseCase = resolveConflictUseCase
                                )

                            DesktopScreen.ELEVES ->
                                PlaceholderScreen("Élèves", "Dossiers et inscriptions des élèves")

                            DesktopScreen.INSCRIPTIONS ->
                                PlaceholderScreen("Inscriptions", "Gestion des inscriptions scolaires")

                            DesktopScreen.FINANCES ->
                                PlaceholderScreen("Finances", "Gestion financière et facturation")

                            DesktopScreen.PERSONNEL ->
                                PlaceholderScreen("Personnel", "Gestion des employés")

                            DesktopScreen.DISCIPLINE ->
                                PlaceholderScreen("Discipline", "Suivi disciplinaire des élèves")

                            DesktopScreen.ANNONCES ->
                                PlaceholderScreen("Annonces", "Annonces et communications")

                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}
