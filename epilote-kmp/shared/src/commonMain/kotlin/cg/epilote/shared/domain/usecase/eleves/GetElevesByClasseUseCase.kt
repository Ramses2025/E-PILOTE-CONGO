package cg.epilote.shared.domain.usecase.eleves

import cg.epilote.shared.data.local.EleveRepository
import cg.epilote.shared.domain.model.Eleve
import kotlinx.coroutines.flow.Flow

class GetElevesByClasseUseCase(private val eleveRepo: EleveRepository) {

    fun asFlow(schoolId: String, classeId: String): Flow<List<Eleve>> =
        eleveRepo.observeByClasse(schoolId, classeId)

    fun now(schoolId: String, classeId: String): List<Eleve> =
        eleveRepo.getByClasse(schoolId, classeId)
}

class SearchEleveUseCase(private val eleveRepo: EleveRepository) {

    fun execute(schoolId: String, query: String): List<Eleve> {
        if (query.length < 2) return emptyList()
        return eleveRepo.search(schoolId, query)
    }
}
