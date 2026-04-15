package cg.epilote.shared.domain.usecase.notes

import cg.epilote.shared.data.local.NoteRepository
import cg.epilote.shared.domain.model.Note

sealed class SaveNoteResult {
    object Success : SaveNoteResult()
    data class Error(val message: String) : SaveNoteResult()
}

class SaveNoteUseCase(private val noteRepo: NoteRepository) {

    fun execute(
        schoolId: String,
        anneeId: String,
        classeId: String,
        matiereId: String,
        eleveId: String,
        periode: String,
        valeur: Double,
        enseignantId: String
    ): SaveNoteResult {
        if (valeur < 0.0 || valeur > 20.0) {
            return SaveNoteResult.Error("La note doit être comprise entre 0 et 20")
        }

        val existingNote = noteRepo.getByClasse(schoolId, classeId, periode)
            .find { it.eleveId == eleveId && it.matiereId == matiereId }

        if (existingNote?.locked == true) {
            return SaveNoteResult.Error("Le bulletin est verrouillé, aucune modification possible")
        }

        val note = Note(
            id           = Note.buildId(schoolId, classeId, matiereId, eleveId, periode),
            schoolId = schoolId,
            anneeId      = anneeId,
            eleveId      = eleveId,
            classeId     = classeId,
            matiereId    = matiereId,
            periode      = periode,
            valeur       = valeur,
            enseignantId = enseignantId
        )
        noteRepo.save(note)
        return SaveNoteResult.Success
    }
}
