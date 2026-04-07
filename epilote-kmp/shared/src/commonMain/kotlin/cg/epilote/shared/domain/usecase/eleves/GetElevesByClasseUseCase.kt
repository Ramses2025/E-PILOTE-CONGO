package cg.epilote.shared.domain.usecase.eleves

import cg.epilote.shared.data.local.EleveRepository
import cg.epilote.shared.domain.model.Eleve
import kotlinx.coroutines.flow.Flow

class GetElevesByClasseUseCase(private val eleveRepo: EleveRepository) {

    fun asFlow(ecoleId: String, classeId: String): Flow<List<Eleve>> =
        eleveRepo.observeByClasse(ecoleId, classeId)

    fun now(ecoleId: String, classeId: String): List<Eleve> =
        eleveRepo.getByClasse(ecoleId, classeId)
}

class SearchEleveUseCase(private val eleveRepo: EleveRepository) {

    fun execute(ecoleId: String, query: String): List<Eleve> {
        if (query.length < 2) return emptyList()
        return eleveRepo.search(ecoleId, query)
    }
}
