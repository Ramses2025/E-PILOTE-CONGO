package cg.epilote.shared.domain.usecase.absences

import cg.epilote.shared.data.local.AbsenceRepository

class JustifyAbsenceUseCase(private val absenceRepo: AbsenceRepository) {

    fun execute(ecoleId: String, eleveId: String, date: String, motif: String): AbsenceResult {
        if (motif.isBlank()) return AbsenceResult.Error("Le motif ne peut pas être vide")

        val existing = absenceRepo.getByEleve(ecoleId, eleveId).find { it.date == date }
            ?: return AbsenceResult.Error("Absence introuvable")

        absenceRepo.save(existing.copy(justifiee = true, motif = motif))
        return AbsenceResult.Success
    }
}
