package cg.epilote.shared.domain.model

data class UserSession(
    val userId: String,
    val username: String,
    val ecoleId: String?,
    val groupeId: String?,
    val role: String,
    val modulesAccess: List<String>,
    val accessToken: String,
    val refreshToken: String,
    val offlineToken: String
)

data class Eleve(
    val id: String,
    val ecoleId: String,
    val matricule: String,
    val nom: String,
    val prenom: String,
    val classeId: String,
    val dateNaissance: String,
    val updatedAt: Long = System.currentTimeMillis()
)

data class Note(
    val id: String,
    val ecoleId: String,
    val eleveId: String,
    val classeId: String,
    val matiereId: String,
    val periode: String,
    val valeur: Double,
    val auteurId: String,
    val locked: Boolean = false,
    val requiresReview: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun buildId(ecoleId: String, classeId: String, matiereId: String, eleveId: String, periode: String) =
            "note::$ecoleId::$classeId::$matiereId::$eleveId::$periode"
    }
}

data class Absence(
    val id: String,
    val ecoleId: String,
    val eleveId: String,
    val date: String,
    val justifiee: Boolean = false,
    val motif: String? = null,
    val saisieParId: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun buildId(ecoleId: String, eleveId: String, date: String) =
            "absence::$ecoleId::$eleveId::$date"
    }
}

data class Classe(
    val id: String,
    val ecoleId: String,
    val nom: String,
    val niveau: String,
    val annee: String,
    val updatedAt: Long = System.currentTimeMillis()
)

data class Matiere(
    val id: String,
    val ecoleId: String,
    val classeId: String,
    val nom: String,
    val coefficient: Int = 1,
    val enseignantId: String,
    val updatedAt: Long = System.currentTimeMillis()
)

data class Module(
    val id: String,
    val code: String,
    val nom: String,
    val categorieCode: String,
    val isActive: Boolean = true
)

enum class SyncStatus {
    SYNCED,
    PENDING,
    OFFLINE
}
