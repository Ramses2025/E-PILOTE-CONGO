package cg.epilote.shared.domain.model

// ─── Auth / Session ───────────────────────────────────────────────────────────

data class ModulePermission(
    val moduleSlug: String,
    val canRead: Boolean = true,
    val canWrite: Boolean = false,
    val canDelete: Boolean = false,
    val canExport: Boolean = false
)

data class UserSession(
    val userId: String,
    val email: String,
    val schoolId: String?,
    val groupId: String?,
    val role: String,
    val firstName: String = "",
    val lastName: String = "",
    val permissions: List<ModulePermission> = emptyList(),
    val accessToken: String,
    val refreshToken: String,
    val offlineToken: String,
    val offlineTokenExpiresAt: Long = 0L,
    val rememberMe: Boolean = false,
    /**
     * Vrai lorsque le compte a été créé avec un mot de passe initial à usage
     * unique et n'a pas encore été changé. Le client desktop ouvre alors le
     * dialogue forcé de changement de mot de passe avant tout accès aux écrans
     * applicatifs ; après succès, ce flag est repassé à `false`.
     */
    val mustChangePassword: Boolean = false
) {
    fun hasModule(slug: String): Boolean =
        permissions.any { it.moduleSlug == slug && it.canRead }
}

// ─── Structure scolaire ───────────────────────────────────────────────────────

data class Niveau(
    val id: String,
    val nom: String,
    val shortName: String,
    val orderIndex: Int
)

data class Filiere(
    val id: String,
    val nom: String,
    val code: String
)

data class AcademicConfig(
    val id: String,
    val schoolId: String,
    val anneeId: String,
    val anneeNom: String,
    val isCurrent: Boolean,
    val niveaux: List<Niveau>,
    val filieres: List<Filiere>,
    val periodes: List<String>
) {
    companion object {
        fun buildId(schoolId: String, annee: String) = "acfg::$schoolId::$annee"
    }
}

// ─── Élève ────────────────────────────────────────────────────────────────────

data class MedicalSummary(
    val bloodType: String? = null,
    val allergies: List<String> = emptyList(),
    val chronicConditions: List<String> = emptyList()
)

data class Parent(
    val relation: String,
    val nom: String,
    val phone: String?,
    val email: String?,
    val profession: String? = null
)

data class Eleve(
    val id: String,
    val schoolId: String,
    val matricule: String,
    val nom: String,
    val prenom: String,
    val classeId: String,
    val anneeId: String,
    val dateNaissance: String? = null,
    val genre: String? = null,
    val photo: String? = null,
    val parents: List<Parent> = emptyList(),
    val medicalSummary: MedicalSummary = MedicalSummary(),
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun buildId(schoolId: String, matricule: String) = "stu::${schoolId}_$matricule"
    }
}

// ─── Note (Grade) ─────────────────────────────────────────────────────────────

data class Note(
    val id: String,
    val schoolId: String,
    val anneeId: String,
    val classeId: String,
    val eleveId: String,
    val eleveName: String = "",
    val matiereId: String,
    val matiereNom: String = "",
    val enseignantId: String,
    val evaluationId: String? = null,
    val typeEval: String = "devoir",
    val periode: String,
    val valeur: Double,
    val valeurMax: Double = 20.0,
    val coefficient: Int = 1,
    val commentaire: String? = null,
    val locked: Boolean = false,
    val requiresReview: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun buildId(schoolId: String, classeId: String, matiereId: String, eleveId: String, periode: String, typeEval: String = "DS") =
            "grd::$schoolId::$classeId::$matiereId::$eleveId::$periode::$typeEval"
    }
}

// ─── Absence ──────────────────────────────────────────────────────────────────

data class Absence(
    val id: String,
    val schoolId: String,
    val anneeId: String = "",
    val classeId: String = "",
    val eleveId: String,
    val eleveName: String = "",
    val date: String,
    val justifiee: Boolean = false,
    val motif: String? = null,
    val saisieParId: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun buildId(schoolId: String, eleveId: String, date: String) =
            "att::$schoolId::$eleveId::$date"
    }
}

// ─── Classe ───────────────────────────────────────────────────────────────────

data class Classe(
    val id: String,
    val schoolId: String,
    val anneeId: String,
    val nom: String,
    val section: String? = null,
    val niveauId: String,
    val niveauNom: String = "",
    val filiereId: String? = null,
    val enseignantPrincipalId: String? = null,
    val capacite: Int = 50,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun buildId(schoolId: String, anneeId: String, nom: String) =
            "cls::$schoolId::$anneeId::${nom.replace(" ", "_").lowercase()}"
    }
}

// ─── Matière (Subject) ────────────────────────────────────────────────────────

data class Matiere(
    val id: String,
    val schoolId: String,
    val classeId: String,
    val nom: String,
    val code: String = "",
    val coefficient: Int = 1,
    val heuresParSemaine: Double = 0.0,
    val enseignantId: String,
    val enseignantNom: String = "",
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun buildId(schoolId: String, classeId: String, code: String) =
            "sub::$schoolId::$classeId::${code.lowercase()}"
    }
}

// ─── Module applicatif ────────────────────────────────────────────────────────

data class ModuleApp(
    val id: String,
    val slug: String,
    val nom: String,
    val isCore: Boolean = true,
    val requiredPlan: String = "gratuit",
    val isActive: Boolean = true
)

// ─── Sync ─────────────────────────────────────────────────────────────────────

enum class SyncStatus {
    SYNCED,
    PENDING,
    OFFLINE,
    ERROR,
    CONFLICT
}

data class SyncMutation(
    val id: String,
    val schoolId: String,
    val clientMutationId: String,
    val deviceId: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: String,
    val status: String = "pending",
    val userId: String,
    val idempotencyKey: String,
    val clientCreatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun buildId(deviceId: String, timestamp: Long, entityType: String) =
            "mut::${deviceId}::${timestamp}::$entityType"
    }
}
