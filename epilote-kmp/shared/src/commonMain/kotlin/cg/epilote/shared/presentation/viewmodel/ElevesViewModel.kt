package cg.epilote.shared.presentation.viewmodel

import cg.epilote.shared.domain.model.Eleve
import cg.epilote.shared.domain.usecase.eleves.GetElevesByClasseUseCase
import cg.epilote.shared.domain.usecase.eleves.SearchEleveUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ElevesViewModel(
    private val getElevesUseCase: GetElevesByClasseUseCase,
    private val searchUseCase: SearchEleveUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _eleves = MutableStateFlow<List<Eleve>>(emptyList())
    val eleves: StateFlow<List<Eleve>> = _eleves.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Eleve>>(emptyList())
    val searchResults: StateFlow<List<Eleve>> = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var currentEcoleId: String = ""

    fun loadByClasse(ecoleId: String, classeId: String) {
        currentEcoleId = ecoleId
        scope.launch {
            getElevesUseCase.asFlow(ecoleId, classeId).collect { _eleves.value = it }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            _searchResults.value = searchUseCase.execute(currentEcoleId, query)
        } else {
            _searchResults.value = emptyList()
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }
}
