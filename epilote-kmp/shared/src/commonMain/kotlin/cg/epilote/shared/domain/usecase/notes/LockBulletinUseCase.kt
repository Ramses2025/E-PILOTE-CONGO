package cg.epilote.shared.domain.usecase.notes

import cg.epilote.shared.data.local.NoteRepository

class LockBulletinUseCase(private val noteRepo: NoteRepository) {

    fun execute(schoolId: String, classeId: String, periode: String): Int {
        val notes = noteRepo.getByClasse(schoolId, classeId, periode)
        var locked = 0
        notes.filter { !it.locked }.forEach { note ->
            noteRepo.save(note.copy(locked = true))
            locked++
        }
        return locked
    }
}
