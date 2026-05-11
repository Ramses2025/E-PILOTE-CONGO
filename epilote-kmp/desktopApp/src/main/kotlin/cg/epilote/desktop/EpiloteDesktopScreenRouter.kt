package cg.epilote.desktop

import androidx.compose.runtime.*
import cg.epilote.desktop.data.*
import cg.epilote.desktop.ui.components.DesktopScreen
import cg.epilote.desktop.ui.screens.*
import cg.epilote.shared.data.local.NoteRepository
import cg.epilote.shared.domain.model.BulletinEleve
import cg.epilote.shared.domain.model.UserSession
import cg.epilote.shared.domain.usecase.notes.ResolveConflictUseCase
import cg.epilote.shared.presentation.viewmodel.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun NonAdminScreenContent(
    currentScreen: DesktopScreen,
    session: UserSession,
    groupeRepo: GroupeAdminDataRepository?,
    classesVm: ClassesViewModel,
    notesVm: NotesViewModel,
    absencesVm: AbsencesViewModel,
    bulletinVm: BulletinViewModel,
    appScope: CoroutineScope,
    aiClient: DesktopAIClient,
    noteRepo: NoteRepository,
    resolveConflictUseCase: ResolveConflictUseCase,
    onScreenChange: (DesktopScreen) -> Unit,
    appreciationResult: AppreciationResult?,
    onAppreciationResult: (AppreciationResult?) -> Unit,
    onAILoadingChange: (Boolean) -> Unit
) {
    when (currentScreen) {
        DesktopScreen.GROUPE_DASHBOARD ->
            if (groupeRepo != null) GroupeDashboardScreen(
                session                = session,
                groupeRepo             = groupeRepo,
                onNavigateEcoles       = { onScreenChange(DesktopScreen.GROUPE_ECOLES) },
                onNavigateUtilisateurs = { onScreenChange(DesktopScreen.GROUPE_UTILISATEURS) },
                onNavigateProfils      = { onScreenChange(DesktopScreen.GROUPE_PROFILS) }
            ) else PlaceholderScreen("Dashboard Groupe", "Groupe non associé à ce compte")

        DesktopScreen.GROUPE_ECOLES ->
            if (groupeRepo != null) GroupeEcolesScreen(groupeRepo = groupeRepo)
            else PlaceholderScreen("Écoles", "Groupe non associé à ce compte")

        DesktopScreen.GROUPE_UTILISATEURS ->
            if (groupeRepo != null) GroupeUtilisateursScreen(groupeRepo = groupeRepo)
            else PlaceholderScreen("Utilisateurs", "Groupe non associé à ce compte")

        DesktopScreen.GROUPE_PROFILS ->
            if (groupeRepo != null) GroupeProfilsScreen(groupeRepo = groupeRepo)
            else PlaceholderScreen("Profils d'accès", "Groupe non associé à ce compte")

        DesktopScreen.DASHBOARD -> DashboardScreen(session)
        DesktopScreen.CLASSES   -> ClassesScreen(session, classesVm)
        DesktopScreen.NOTES     -> NotesScreen(session, classesVm, notesVm)
        DesktopScreen.ABSENCES  -> AbsencesScreen(session, absencesVm)

        DesktopScreen.BULLETINS -> BulletinScreen(
            session               = session,
            classesViewModel      = classesVm,
            bulletinViewModel     = bulletinVm,
            appreciationResult    = appreciationResult,
            onDismissAppreciation = { onAppreciationResult(null) },
            onRequestAppreciation = { bulletin: BulletinEleve ->
                onAILoadingChange(true)
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
                    onAppreciationResult(if (resp != null) {
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
                    })
                    onAILoadingChange(false)
                }
            }
        )

        DesktopScreen.CAHIER -> CahierTextesScreen(
            session = session,
            onRequestGenerateContent = { titre, niveau, matiere, type ->
                appScope.launch {
                    aiClient.generateContent(AIContentRequestDto(
                        titre = titre, niveau = niveau, matiere = matiere, type = type
                    ))
                }
            }
        )

        DesktopScreen.CONFLICTS -> ConflictsScreen(
            session                = session,
            noteRepo               = noteRepo,
            resolveConflictUseCase = resolveConflictUseCase
        )

        DesktopScreen.ELEVES        -> PlaceholderScreen("Élèves", "Dossiers et inscriptions des élèves")
        DesktopScreen.INSCRIPTIONS  -> PlaceholderScreen("Inscriptions", "Gestion des inscriptions scolaires")
        DesktopScreen.FINANCES      -> PlaceholderScreen("Finances", "Gestion financière et facturation")
        DesktopScreen.PERSONNEL     -> PlaceholderScreen("Personnel", "Gestion des employés")
        DesktopScreen.DISCIPLINE    -> PlaceholderScreen("Discipline", "Suivi disciplinaire des élèves")
        DesktopScreen.ANNONCES      -> PlaceholderScreen("Annonces", "Annonces et communications")
        else -> Unit
    }
}
