package cg.epilote.shared.domain.model

data class MoyenneMatiere(
    val matiereId: String,
    val matiereNom: String,
    val coefficient: Int,
    val moyenne: Double,
    val noteCount: Int
)

data class BulletinEleve(
    val eleveId: String,
    val eleveNom: String,
    val elevePrenom: String,
    val classeId: String,
    val periode: String,
    val moyennes: List<MoyenneMatiere>,
    val moyenneGenerale: Double,
    val rang: Int = 0,
    val totalEleves: Int = 0,
    val absencesCount: Int = 0,
    val locked: Boolean = false
)

object MoyenneCalculator {

    fun calculerMoyenneMatiere(notes: List<Note>, matiereId: String): Double {
        val notesMatiere = notes.filter { it.matiereId == matiereId && !it.requiresReview }
        if (notesMatiere.isEmpty()) return 0.0
        return notesMatiere.sumOf { it.valeur } / notesMatiere.size
    }

    fun calculerMoyenneGenerale(moyennes: List<MoyenneMatiere>): Double {
        val totalCoeff = moyennes.sumOf { it.coefficient }
        if (totalCoeff == 0) return 0.0
        return moyennes.sumOf { it.moyenne * it.coefficient } / totalCoeff
    }

    fun calculerBulletinEleve(
        eleve: Eleve,
        notes: List<Note>,
        matieres: List<Matiere>,
        absencesCount: Int,
        periode: String
    ): BulletinEleve {
        val notesEleve = notes.filter { it.eleveId == eleve.id }

        val moyennesMatieres = matieres.map { matiere ->
            val moy = calculerMoyenneMatiere(notesEleve, matiere.id)
            MoyenneMatiere(
                matiereId   = matiere.id,
                matiereNom  = matiere.nom,
                coefficient = matiere.coefficient,
                moyenne     = moy,
                noteCount   = notesEleve.count { it.matiereId == matiere.id }
            )
        }

        return BulletinEleve(
            eleveId         = eleve.id,
            eleveNom        = eleve.nom,
            elevePrenom     = eleve.prenom,
            classeId        = eleve.classeId,
            periode         = periode,
            moyennes        = moyennesMatieres,
            moyenneGenerale = calculerMoyenneGenerale(moyennesMatieres),
            absencesCount   = absencesCount
        )
    }

    fun calculerRangs(bulletins: List<BulletinEleve>): List<BulletinEleve> {
        val sorted = bulletins.sortedByDescending { it.moyenneGenerale }
        return sorted.mapIndexed { index, bulletin ->
            bulletin.copy(rang = index + 1, totalEleves = bulletins.size)
        }
    }
}
