package cg.epilote.shared.presentation.viewmodel

import cg.epilote.shared.data.local.ClasseRepository
import cg.epilote.shared.data.local.MatiereRepository
import cg.epilote.shared.domain.model.Classe
import cg.epilote.shared.domain.model.Matiere
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ClassesViewModel(
    private val classeRepo: ClasseRepository,
    private val matiereRepo: MatiereRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _classes = MutableStateFlow<List<Classe>>(emptyList())
    val classes: StateFlow<List<Classe>> = _classes.asStateFlow()

    private val _matieres = MutableStateFlow<List<Matiere>>(emptyList())
    val matieres: StateFlow<List<Matiere>> = _matieres.asStateFlow()

    private val _selectedClasse = MutableStateFlow<Classe?>(null)
    val selectedClasse: StateFlow<Classe?> = _selectedClasse.asStateFlow()

    fun loadClasses(schoolId: String) {
        scope.launch {
            classeRepo.observeByEcole(schoolId).collect { _classes.value = it }
        }
    }

    fun selectClasse(classe: Classe, schoolId: String) {
        _selectedClasse.value = classe
        scope.launch {
            matiereRepo.observeByClasse(schoolId, classe.id).collect { _matieres.value = it }
        }
    }

    fun getMatieresByEnseignant(schoolId: String, enseignantId: String): List<Matiere> =
        matiereRepo.getByEnseignant(schoolId, enseignantId)
}
