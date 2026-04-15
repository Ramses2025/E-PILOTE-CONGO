package cg.epilote.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.components.DesktopScreen
import cg.epilote.desktop.ui.components.Sidebar
import cg.epilote.desktop.ui.components.SyncStatusBar
import cg.epilote.desktop.ui.screens.*
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
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val desktopBackendBaseUrl: String
    get() = System.getProperty("epilote.backend.url")
        ?: System.getenv("EPILOTE_BACKEND_URL")
        ?: "http://localhost:8080"

// ── DTOs IA pour appels REST backend ─────────────────────────────────────────

@Serializable
data class AIContentRequestDto(
    val titre: String,
    val niveau: String,
    val filiere: String = "",
    val type: String = "COURS",
    val matiere: String = "",
    val dureeMinutes: Int = 60,
    val context: String = "Congo/MEPSA"
)

@Serializable
data class AIContentResponseDto(
    val contenu: String,
    val titre: String,
    val type: String,
    val niveau: String,
    val tokensUtilises: Int = 0,
    val modele: String = "",
    val fallback: Boolean = false
)

@Serializable
data class AIAppreciationRequestDto(
    val eleveNom: String,
    val genre: String = "M",
    val moyenneGenerale: Double,
    val moyenneMin: Double = 0.0,
    val moyenneMax: Double = 20.0,
    val rang: Int = 0,
    val effectif: Int = 0,
    val absences: Int = 0,
    val comportement: String = "correct"
)

@Serializable
data class AIAppreciationResponseDto(
    val appreciation: String,
    val mention: String,
    val conseil: String = "",
    val fallback: Boolean = false
)

// ── Client Admin Desktop ──────────────────────────────────────────────────────

class DesktopAdminClient(private val baseUrl: String, private val tokenProvider: () -> String?) {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    private suspend inline fun <reified T> get(path: String): T? =
        runCatching {
            httpClient.get("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }.body<T>()
        }.getOrNull()

    private suspend inline fun <reified T, reified B : Any> post(path: String, body: B): T? =
        runCatching {
            httpClient.post("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(body)
            }.body<T>()
        }.getOrNull()

    suspend fun getDashboardStats(): DashboardStatsDto? = get("/api/super-admin/dashboard-stats")
    suspend fun listGroupes(): List<GroupeApiDto>? = get("/api/super-admin/groupes")
    suspend fun listPlans(): List<PlanApiDto>? = get("/api/super-admin/plans")
    suspend fun listModules(): List<ModuleApiDto>? = get("/api/super-admin/modules")
    suspend fun listAdminGroupes(groupId: String): List<UserApiDto>? = get("/api/super-admin/groupes/$groupId/admins")

    suspend fun createGroupe(nom: String, province: String, planId: String): GroupeApiDto? =
        post("/api/super-admin/groupes", CreateGroupeDto(nom, province, planId))

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
}

@Serializable data class DashboardStatsDto(
    val totalGroupes: Long = 0, val totalEcoles: Long = 0,
    val totalUtilisateurs: Long = 0, val totalModules: Long = 0,
    val totalPlans: Long = 0, val totalCategories: Long = 0,
    val totalSubscriptions: Long = 0, val activeSubscriptions: Long = 0,
    val totalInvoices: Long = 0, val revenueTotal: Long = 0,
    val revenuePaid: Long = 0, val invoicesOverdue: Long = 0,
    val groupesByProvince: List<ProvinceStatsDto> = emptyList(),
    val planDistribution: List<PlanDistributionDto> = emptyList(),
    val subscriptionsByStatus: Map<String, Long> = emptyMap(),
    val recentGroupes: List<GroupeApiDto> = emptyList(),
    val recentInvoices: List<InvoiceApiDto> = emptyList()
)
@Serializable data class ProvinceStatsDto(
    val province: String = "", val groupesCount: Long = 0, val ecolesCount: Long = 0
)
@Serializable data class PlanDistributionDto(
    val planId: String = "", val planNom: String = "", val groupesCount: Long = 0
)
@Serializable data class InvoiceApiDto(
    val id: String = "", val groupeId: String = "", val subscriptionId: String = "",
    val montantXAF: Long = 0, val statut: String = "draft",
    val dateEmission: Long = 0, val dateEcheance: Long = 0,
    val datePaiement: Long? = null, val reference: String = "", val notes: String = ""
)
@Serializable data class GroupeApiDto(val id: String = "", val nom: String = "", val province: String = "", val planId: String = "", val ecolesCount: Int = 0, val createdAt: Long = 0)
@Serializable data class PlanApiDto(val id: String = "", val nom: String = "", val prixXAF: Long = 0, val maxEcoles: Int = 0, val maxUtilisateurs: Int = 0, val modulesIncluded: List<String> = emptyList(), val categoriesIncluded: List<String> = emptyList(), val dureeJours: Int = 365)
@Serializable data class ModuleApiDto(val id: String = "", val code: String = "", val nom: String = "", val categorieCode: String = "", val description: String = "", val isCore: Boolean = false, val requiredPlan: String = "gratuit", val isActive: Boolean = true)
@Serializable data class UserApiDto(val id: String = "", val username: String = "", val firstName: String = "", val lastName: String = "", val email: String = "", val schoolId: String? = null, val groupId: String = "", val profilId: String? = null, val role: String = "USER", val isActive: Boolean = true, val createdAt: Long = 0)
@Serializable data class CreateGroupeDto(val nom: String, val province: String, val planId: String)
@Serializable data class CreateAdminGroupeDto(val password: String, val nom: String, val prenom: String, val email: String)

// ── Client IA Desktop ─────────────────────────────────────────────────────────

class DesktopAIClient(private val baseUrl: String, private val tokenProvider: () -> String?) {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    suspend fun generateContent(req: AIContentRequestDto): AIContentResponseDto? =
        runCatching {
            httpClient.post("$baseUrl/api/ai/generate-content") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(req)
            }.body<AIContentResponseDto>()
        }.getOrNull()

    suspend fun generateAppreciation(req: AIAppreciationRequestDto): AIAppreciationResponseDto? =
        runCatching {
            httpClient.post("$baseUrl/api/ai/generate-appreciation") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(req)
            }.body<AIAppreciationResponseDto>()
        }.getOrNull()
}

// ── Composable principal ──────────────────────────────────────────────────────

@Composable
fun EpiloteDesktopApp() {
    var session by remember { mutableStateOf<UserSession?>(null) }
    var currentScreen by remember { mutableStateOf(DesktopScreen.DASHBOARD) }
    val isSuperAdmin = session?.role == "SUPER_ADMIN"

    EpiloteTheme {
        if (session == null) {
            val sessionsDb = remember {
                if (!EpiloteDatabase.isSessionsOpen()) EpiloteDatabase.initSessions(null)
                EpiloteDatabase.sessionsInstance
            }
            val apiClient = remember {
                ApiClient(
                    baseUrl        = desktopBackendBaseUrl,
                    tokenProvider  = { null },
                    onTokenExpired = {}
                )
            }
            val sessionRepo    = remember { UserSessionRepository(sessionsDb) }
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
            val db = EpiloteDatabase.instance

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
                DesktopAdminClient(desktopBackendBaseUrl) { s.accessToken }
            }

            // ── Admin state ──────────────────────────────────────────
            var adminStats by remember { mutableStateOf(AdminStats()) }
            var dashboardStatsDto by remember { mutableStateOf(DashboardStatsDto()) }
            var adminGroupes by remember { mutableStateOf<List<GroupeDto>>(emptyList()) }
            var adminGroupAdmins by remember { mutableStateOf<Map<String, List<UserDto>>>(emptyMap()) }
            var adminPlans by remember { mutableStateOf<List<PlanDto>>(emptyList()) }
            var adminModules by remember { mutableStateOf<List<ModuleDto>>(emptyList()) }
            var adminLoading by remember { mutableStateOf(false) }
            var sidebarExpanded by remember { mutableStateOf(true) }

            fun loadAdminData() {
                adminLoading = true
                appScope.launch {
                    val stats = adminClient.getDashboardStats()
                    if (stats != null) {
                        dashboardStatsDto = stats
                        adminStats = AdminStats(
                            totalGroupes = stats.totalGroupes,
                            totalEcoles = stats.totalEcoles,
                            totalUtilisateurs = stats.totalUtilisateurs,
                            totalModules = stats.totalModules,
                            totalPlans = stats.totalPlans,
                            totalCategories = stats.totalCategories,
                            totalSubscriptions = stats.totalSubscriptions,
                            activeSubscriptions = stats.activeSubscriptions,
                            totalInvoices = stats.totalInvoices,
                            revenueTotal = stats.revenueTotal,
                            revenuePaid = stats.revenuePaid,
                            invoicesOverdue = stats.invoicesOverdue,
                            groupesByProvince = stats.groupesByProvince,
                            planDistribution = stats.planDistribution,
                            subscriptionsByStatus = stats.subscriptionsByStatus,
                            recentGroupes = stats.recentGroupes,
                            recentInvoices = stats.recentInvoices
                        )
                    }
                    val g = adminClient.listGroupes()
                    if (g != null) {
                        adminGroupes = g.map { GroupeDto(it.id, it.nom, it.province, it.planId, it.ecolesCount, it.createdAt) }
                        adminGroupAdmins = g.associate { groupe ->
                            val admins = adminClient.listAdminGroupes(groupe.id)
                                ?.map {
                                    UserDto(
                                        id = it.id,
                                        username = it.username,
                                        firstName = it.firstName,
                                        lastName = it.lastName,
                                        email = it.email,
                                        schoolId = it.schoolId ?: "",
                                        groupId = it.groupId,
                                        profilId = it.profilId ?: "",
                                        role = it.role,
                                        isActive = it.isActive,
                                        createdAt = it.createdAt
                                    )
                                }
                                ?: emptyList()
                            groupe.id to admins
                        }
                    }
                    val p = adminClient.listPlans()
                    if (p != null) adminPlans = p.map { PlanDto(it.id, it.nom, it.maxEcoles, it.maxUtilisateurs, it.modulesIncluded, it.categoriesIncluded, it.dureeJours) }
                    val m = adminClient.listModules()
                    if (m != null) adminModules = m.map { ModuleDto(it.id, it.code, it.nom, it.categorieCode, it.description, it.isCore, it.requiredPlan, it.isActive) }
                    adminLoading = false
                }
            }

            LaunchedEffect(isSuperAdmin) {
                if (isSuperAdmin) loadAdminData()
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

            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f)) {
                    Sidebar(
                        session          = s,
                        currentScreen    = currentScreen,
                        onScreenSelected = { currentScreen = it },
                        onLogout         = {
                            syncManager.stop()
                            UserSessionRepository(EpiloteDatabase.sessionsInstance).clearSession()
                            EpiloteDatabase.closeAll()
                            session = null
                        },
                        isExpanded       = sidebarExpanded,
                        onToggleExpanded = { sidebarExpanded = !sidebarExpanded }
                    )

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when (currentScreen) {
                            // ── Admin Screens ────────────────────────
                            DesktopScreen.ADMIN_DASHBOARD ->
                                SuperAdminDashboardScreen(
                                    session = s,
                                    stats = adminStats,
                                    isLoading = adminLoading,
                                    onNavigateGroupes = { currentScreen = DesktopScreen.ADMIN_GROUPES },
                                    onNavigatePlans = { currentScreen = DesktopScreen.ADMIN_PLANS },
                                    onNavigateModules = { currentScreen = DesktopScreen.ADMIN_MODULES },
                                    onRefresh = { loadAdminData() }
                                )

                            DesktopScreen.ADMIN_GROUPES ->
                                AdminGroupesScreen(
                                    groupes = adminGroupes,
                                    adminGroupesByGroup = adminGroupAdmins,
                                    isLoading = adminLoading,
                                    onCreateGroupe = { nom, province, planId ->
                                        appScope.launch {
                                            adminClient.createGroupe(nom, province, planId)
                                            loadAdminData()
                                        }
                                    },
                                    onCreateAdminGroupe = { groupId, password, nom, prenom, email ->
                                        appScope.launch {
                                            adminClient.createAdminGroupe(groupId, password, nom, prenom, email)
                                            loadAdminData()
                                        }
                                    },
                                    onRefresh = { loadAdminData() }
                                )

                            DesktopScreen.ADMIN_PLANS ->
                                AdminPlansScreen(
                                    plans = adminPlans,
                                    isLoading = adminLoading,
                                    onRefresh = { loadAdminData() }
                                )

                            DesktopScreen.ADMIN_MODULES ->
                                AdminModulesScreen(
                                    modules = adminModules,
                                    isLoading = adminLoading,
                                    onRefresh = { loadAdminData() }
                                )

                            DesktopScreen.ADMIN_CATEGORIES ->
                                PlaceholderScreen("Catégories", "CRUD des catégories dynamiques de la plateforme")

                            DesktopScreen.ADMIN_SUBSCRIPTIONS ->
                                PlaceholderScreen("Abonnements", "Gestion des abonnements par groupe scolaire")

                            DesktopScreen.ADMIN_INVOICES ->
                                PlaceholderScreen("Factures", "Facturation plateforme — émission, suivi, relances")

                            DesktopScreen.ADMIN_ADMINISTRATORS ->
                                PlaceholderScreen("Administrateurs", "Gestion des administrateurs de groupes scolaires")

                            DesktopScreen.ADMIN_ANNOUNCEMENTS ->
                                PlaceholderScreen("Annonces", "Diffusion d'annonces vers tous les groupes")

                            DesktopScreen.ADMIN_NOTIFICATIONS ->
                                PlaceholderScreen("Notifications", "Notifications plateforme — alertes et rappels")

                            DesktopScreen.ADMIN_MESSAGING ->
                                PlaceholderScreen("Messagerie", "Messagerie interne plateforme")

                            DesktopScreen.ADMIN_TICKETS ->
                                PlaceholderScreen("Signalements", "Tickets de support et signalements")

                            DesktopScreen.ADMIN_AUDIT_LOG ->
                                AuditLogScreen()

                            // ── Admin Groupe Screens ──────────────────
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
                        }
                    }
                }

                SyncStatusBar(syncVm)
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, description: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = EpiloteTextMuted)
            Text(description, fontSize = 14.sp, color = EpiloteTextMuted)
            Text("Module en cours de développement", fontSize = 12.sp, color = EpiloteGreen)
        }
    }
}
