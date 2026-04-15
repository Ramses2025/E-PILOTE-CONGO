package cg.epilote.shared.domain.usecase.absences

import cg.epilote.shared.data.local.AbsenceRepository
import cg.epilote.shared.domain.model.Absence

sealed class AbsenceResult {
    object Success : AbsenceResult()
    data class Error(val message: String) : AbsenceResult()
}

class SaveAbsenceUseCase(private val absenceRepo: AbsenceRepository) {

    fun execute(
        schoolId: String,
        eleveId: String,
        date: String,
        saisieParId: String
    ): AbsenceResult {
        if (date.isBlank()) return AbsenceResult.Error("Date invalide")

        val absence = Absence(
            id          = Absence.buildId(schoolId, eleveId, date),
            schoolId = schoolId,
            eleveId     = eleveId,
            date        = date,
            justifiee   = false,
            motif       = null,
            saisieParId = saisieParId
        )
        absenceRepo.save(absence)
        return AbsenceResult.Success
    }
}
