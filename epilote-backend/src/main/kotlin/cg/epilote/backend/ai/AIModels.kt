package cg.epilote.backend.ai

import jakarta.validation.constraints.NotBlank

// ── Requêtes entrantes ────────────────────────────────────────────────────────

data class AIContentRequest(
    @field:NotBlank val titre: String,
    @field:NotBlank val niveau: String,
    val filiere: String = "",
    val type: ContentType = ContentType.COURS,
    val matiere: String = "",
    val dureeMinutes: Int = 60,
    val context: String = "Congo/MEPSA"
)

data class AIAppreciationRequest(
    @field:NotBlank val eleveNom: String,
    val genre: String = "M",
    val moyenneGenerale: Double,
    val moyenneMin: Double = 0.0,
    val moyenneMax: Double = 20.0,
    val rang: Int = 0,
    val effectif: Int = 0,
    val absences: Int = 0,
    val comportement: String = "correct",
    val notesMatieres: List<MatiereNote> = emptyList()
)

data class MatiereNote(
    val matiere: String,
    val note: Double,
    val noteMax: Double = 20.0,
    val coefficient: Int = 1
)

enum class ContentType {
    COURS, EXAMEN, DEVOIR, FICHE_REVISION, PROGRESSION
}

// ── Réponses sortantes ────────────────────────────────────────────────────────

data class AIContentResponse(
    val contenu: String,
    val titre: String,
    val type: ContentType,
    val niveau: String,
    val tokensUtilises: Int = 0,
    val modele: String = "",
    val fallback: Boolean = false
)

data class AIAppreciationResponse(
    val appreciation: String,
    val mention: String,
    val conseil: String = "",
    val fallback: Boolean = false
)

// ── Payload LLM interne ───────────────────────────────────────────────────────

data class LLMMessage(val role: String, val content: String)

data class LLMRequest(
    val model: String,
    val messages: List<LLMMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 2048
)

data class LLMChoice(val message: LLMMessage, val finish_reason: String = "")
data class LLMUsage(val prompt_tokens: Int = 0, val completion_tokens: Int = 0, val total_tokens: Int = 0)
data class LLMResponse(val choices: List<LLMChoice>, val usage: LLMUsage = LLMUsage())
