package cg.epilote.shared.presentation.viewmodel

import cg.epilote.shared.data.local.AbsenceRepository
import cg.epilote.shared.data.local.EleveRepository
import cg.epilote.shared.data.local.MatiereRepository
import cg.epilote.shared.data.local.NoteRepository
import cg.epilote.shared.domain.model.BulletinEleve
import cg.epilote.shared.domain.model.MoyenneCalculator
import cg.epilote.shared.domain.usecase.notes.LockBulletinUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BulletinUiState {
    object Loading : BulletinUiState()
    data class Ready(val bulletins: List<BulletinEleve>) : BulletinUiState()
    data class Locked(val bulletins: List<BulletinEleve>) : BulletinUiState()
    data class Error(val message: String) : BulletinUiState()
}

class BulletinViewModel(
    private val eleveRepo: EleveRepository,
    private val noteRepo: NoteRepository,
    private val matiereRepo: MatiereRepository,
    private val absenceRepo: AbsenceRepository,
    private val lockUseCase: LockBulletinUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow<BulletinUiState>(BulletinUiState.Loading)
    val uiState: StateFlow<BulletinUiState> = _uiState.asStateFlow()

    fun loadBulletins(schoolId: String, classeId: String, periode: String) {
        _uiState.value = BulletinUiState.Loading
        scope.launch {
            runCatching {
                val eleves   = eleveRepo.getByClasse(schoolId, classeId)
                val notes    = noteRepo.getByClasse(schoolId, classeId, periode)
                val matieres = matiereRepo.getByClasse(schoolId, classeId)

                val bulletins = eleves.map { eleve ->
                    val absCount = absenceRepo.getByEleve(schoolId, eleve.id).size
                    MoyenneCalculator.calculerBulletinEleve(eleve, notes, matieres, absCount, periode)
                }
                MoyenneCalculator.calculerRangs(bulletins)
            }.onSuccess { bulletins ->
                val isLocked = bulletins.isNotEmpty() && noteRepo
                    .getByClasse(schoolId, bulletins.first().classeId, periode)
                    .all { it.locked }
                _uiState.value = if (isLocked)
                    BulletinUiState.Locked(bulletins)
                else
                    BulletinUiState.Ready(bulletins)
            }.onFailure {
                _uiState.value = BulletinUiState.Error(it.message ?: "Erreur de chargement")
            }
        }
    }

    fun lockBulletin(schoolId: String, classeId: String, periode: String) {
        scope.launch {
            val count = lockUseCase.execute(schoolId, classeId, periode)
            loadBulletins(schoolId, classeId, periode)
        }
    }
}
