package cg.epilote.shared.domain.usecase.notes

import cg.epilote.shared.data.local.NoteRepository
import cg.epilote.shared.domain.model.Note

class ResolveConflictUseCase(private val noteRepo: NoteRepository) {

    fun resolveKeepLocal(note: Note) {
        noteRepo.save(note.copy(requiresReview = false))
    }

    fun resolveWithValue(note: Note, resolvedValeur: Double): SaveNoteResult {
        if (resolvedValeur < 0.0 || resolvedValeur > 20.0) {
            return SaveNoteResult.Error("Valeur invalide : $resolvedValeur")
        }
        noteRepo.save(note.copy(valeur = resolvedValeur, requiresReview = false))
        return SaveNoteResult.Success
    }
}
