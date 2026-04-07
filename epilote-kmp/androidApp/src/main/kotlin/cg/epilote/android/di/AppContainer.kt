package cg.epilote.android.di

import android.content.Context
import cg.epilote.android.connectivity.ConnectivityObserver
import cg.epilote.shared.data.local.*
import cg.epilote.shared.data.remote.ApiClient
import cg.epilote.shared.data.remote.AuthApiService
import cg.epilote.shared.data.sync.SyncManager
import cg.epilote.shared.domain.usecase.absences.JustifyAbsenceUseCase
import cg.epilote.shared.domain.usecase.absences.SaveAbsenceUseCase
import cg.epilote.shared.domain.usecase.auth.LoginUseCase
import cg.epilote.shared.domain.usecase.auth.LogoutUseCase
import cg.epilote.shared.domain.usecase.auth.RefreshTokenUseCase
import cg.epilote.shared.domain.usecase.eleves.GetElevesByClasseUseCase
import cg.epilote.shared.domain.usecase.eleves.SearchEleveUseCase
import cg.epilote.shared.domain.usecase.notes.LockBulletinUseCase
import cg.epilote.shared.domain.usecase.notes.ResolveConflictUseCase
import cg.epilote.shared.domain.usecase.notes.SaveNoteUseCase
import cg.epilote.shared.presentation.viewmodel.*

class AppContainer(private val context: Context) {

    private val backendUrl = "https://api.epilote.cg"
    private val syncGatewayUrl = "wss://x0by7zekx39pidsy.apps.cloud.couchbase.com:4984/epilote"

    private val sessionRepo: UserSessionRepository by lazy {
        UserSessionRepository(EpiloteDatabase.instance)
    }

    val syncManager: SyncManager by lazy {
        SyncManager(EpiloteDatabase.instance, syncGatewayUrl)
    }

    private val connectivity = ConnectivityObserver(context)

    private val apiClient: ApiClient by lazy {
        ApiClient(
            baseUrl = backendUrl,
            tokenProvider = { sessionRepo.getSession()?.accessToken },
            onTokenExpired = { refreshTokenUseCase.execute() }
        )
    }

    private val authApi    by lazy { AuthApiService(apiClient) }
    private val noteRepo   by lazy { NoteRepository(EpiloteDatabase.instance) }
    private val absenceRepo by lazy { AbsenceRepository(EpiloteDatabase.instance) }
    private val eleveRepo  by lazy { EleveRepository(EpiloteDatabase.instance) }
    private val classeRepo by lazy { ClasseRepository(EpiloteDatabase.instance) }
    private val matiereRepo by lazy { MatiereRepository(EpiloteDatabase.instance) }

    private val refreshTokenUseCase by lazy { RefreshTokenUseCase(authApi, sessionRepo) }

    val loginViewModel: LoginViewModel by lazy {
        LoginViewModel(LoginUseCase(authApi, sessionRepo, syncManager, context))
    }

    val syncIndicatorViewModel: SyncIndicatorViewModel by lazy {
        SyncIndicatorViewModel(syncManager)
    }

    val notesViewModel: NotesViewModel by lazy {
        NotesViewModel(noteRepo, syncManager)
    }

    val absencesViewModel: AbsencesViewModel by lazy {
        AbsencesViewModel(absenceRepo, SaveAbsenceUseCase(absenceRepo), JustifyAbsenceUseCase(absenceRepo))
    }

    val elevesViewModel: ElevesViewModel by lazy {
        ElevesViewModel(GetElevesByClasseUseCase(eleveRepo), SearchEleveUseCase(eleveRepo))
    }

    val classesViewModel: ClassesViewModel by lazy {
        ClassesViewModel(classeRepo, matiereRepo)
    }

    val bulletinViewModel: BulletinViewModel by lazy {
        BulletinViewModel(eleveRepo, noteRepo, matiereRepo, absenceRepo, LockBulletinUseCase(noteRepo))
    }

    init {
        observeConnectivity()
    }

    private fun observeConnectivity() {
        kotlinx.coroutines.GlobalScope.run {
            kotlinx.coroutines.launch(kotlinx.coroutines.Dispatchers.IO) {
                connectivity.observe().collect { connected ->
                    if (connected) syncManager.onNetworkAvailable()
                    else syncManager.onNetworkLost()
                }
            }
        }
    }
}
