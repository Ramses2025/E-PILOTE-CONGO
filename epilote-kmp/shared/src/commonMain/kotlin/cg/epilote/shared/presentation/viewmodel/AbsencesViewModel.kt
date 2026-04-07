package cg.epilote.shared.presentation.viewmodel

import cg.epilote.shared.data.local.AbsenceRepository
import cg.epilote.shared.domain.model.Absence
import cg.epilote.shared.domain.usecase.absences.AbsenceResult
import cg.epilote.shared.domain.usecase.absences.JustifyAbsenceUseCase
import cg.epilote.shared.domain.usecase.absences.SaveAbsenceUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AbsenceUiState {
    object Idle : AbsenceUiState()
    object Saved : AbsenceUiState()
    object Justified : AbsenceUiState()
    data class Error(val message: String) : AbsenceUiState()
}

class AbsencesViewModel(
    private val absenceRepo: AbsenceRepository,
    private val saveAbsenceUseCase: SaveAbsenceUseCase,
    private val justifyUseCase: JustifyAbsenceUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _absences = MutableStateFlow<List<Absence>>(emptyList())
    val absences: StateFlow<List<Absence>> = _absences.asStateFlow()

    private val _uiState = MutableStateFlow<AbsenceUiState>(AbsenceUiState.Idle)
    val uiState: StateFlow<AbsenceUiState> = _uiState.asStateFlow()

    fun loadByDate(ecoleId: String, date: String) {
        scope.launch {
            absenceRepo.observeByDate(ecoleId, date).collect { _absences.value = it }
        }
    }

    fun loadByEleve(ecoleId: String, eleveId: String) {
        scope.launch {
            _absences.value = absenceRepo.getByEleve(ecoleId, eleveId)
        }
    }

    fun markAbsent(ecoleId: String, eleveId: String, date: String, saisieParId: String) {
        val result = saveAbsenceUseCase.execute(ecoleId, eleveId, date, saisieParId)
        _uiState.value = when (result) {
            is AbsenceResult.Success -> AbsenceUiState.Saved
            is AbsenceResult.Error   -> AbsenceUiState.Error(result.message)
        }
    }

    fun justifyAbsence(ecoleId: String, eleveId: String, date: String, motif: String) {
        val result = justifyUseCase.execute(ecoleId, eleveId, date, motif)
        _uiState.value = when (result) {
            is AbsenceResult.Success -> AbsenceUiState.Justified
            is AbsenceResult.Error   -> AbsenceUiState.Error(result.message)
        }
    }

    fun resetState() { _uiState.value = AbsenceUiState.Idle }
}
