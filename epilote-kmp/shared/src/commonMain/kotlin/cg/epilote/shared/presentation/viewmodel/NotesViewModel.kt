package cg.epilote.shared.presentation.viewmodel

import cg.epilote.shared.data.local.NoteRepository
import cg.epilote.shared.data.sync.SyncManager
import cg.epilote.shared.domain.model.Note
import cg.epilote.shared.domain.model.SyncStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesViewModel(
    private val noteRepo: NoteRepository,
    private val syncManager: SyncManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Idle)
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus
    val pendingCount: StateFlow<Long>     = syncManager.pendingCount

    fun loadNotes(ecoleId: String, classeId: String, periode: String) {
        scope.launch {
            noteRepo.observeByClasse(ecoleId, classeId, periode)
                .collect { _notes.value = it }
        }
    }

    fun saveNote(ecoleId: String, anneeId: String, classeId: String, matiereId: String,
                 eleveId: String, periode: String, valeur: Double, enseignantId: String) {
        if (valeur < 0 || valeur > 20) {
            _uiState.value = NotesUiState.Error("La note doit être entre 0 et 20")
            return
        }
        scope.launch {
            val note = Note(
                id           = Note.buildId(ecoleId, classeId, matiereId, eleveId, periode),
                ecoleId      = ecoleId,
                anneeId      = anneeId,
                eleveId      = eleveId,
                classeId     = classeId,
                matiereId    = matiereId,
                periode      = periode,
                valeur       = valeur,
                enseignantId = enseignantId
            )
            noteRepo.save(note)
            _uiState.value = NotesUiState.Saved
        }
    }

    fun loadPendingReview(ecoleId: String) {
        scope.launch {
            val pending = noteRepo.getPendingReview(ecoleId)
            if (pending.isNotEmpty()) {
                _uiState.value = NotesUiState.HasConflicts(pending)
            }
        }
    }
}

sealed class NotesUiState {
    object Idle : NotesUiState()
    object Saved : NotesUiState()
    data class Error(val message: String) : NotesUiState()
    data class HasConflicts(val notes: List<Note>) : NotesUiState()
}
