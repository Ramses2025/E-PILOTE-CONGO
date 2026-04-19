package cg.epilote.backend.ai

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class AIService(
    @Value("\${ai.openai.api-key:}") private val openaiKey: String,
    @Value("\${ai.mistral.api-key:}") private val mistralKey: String,
    @Value("\${ai.provider:mistral}") private val provider: String
) {
    private val log = LoggerFactory.getLogger(AIService::class.java)

    private val json = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private val mediaType = "application/json".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val activeKey: String get() = if (provider == "openai") openaiKey else mistralKey
    private val apiUrl: String get() = if (provider == "openai")
        "https://api.openai.com/v1/chat/completions"
    else
        "https://api.mistral.ai/v1/chat/completions"

    private val model: String get() = if (provider == "openai") "gpt-4o-mini" else "mistral-small-latest"

    // ── Génération contenu pédagogique ────────────────────────────────────────

    suspend fun generateContent(req: AIContentRequest): AIContentResponse {
        if (activeKey.isBlank()) {
            log.warn("Clé API IA non configurée — retour template vide")
            return fallbackContent(req)
        }

        val typeLabel = when (req.type) {
            ContentType.COURS         -> "un cours structuré"
            ContentType.EXAMEN        -> "un sujet d'examen"
            ContentType.DEVOIR        -> "un devoir maison"
            ContentType.FICHE_REVISION -> "une fiche de révision"
            ContentType.PROGRESSION   -> "une progression annuelle"
        }

        val systemPrompt = """
            Tu es un enseignant expérimenté du système éducatif congolais (Congo-Brazzaville), 
            respectant les programmes officiels du MEPSA (Ministère de l'Enseignement Primaire, 
            Secondaire et de l'Alphabétisation). Tu produis des contenus pédagogiques complets, 
            structurés et adaptés au niveau scolaire indiqué. 
            Tu utilises toujours le français académique congolais. 
            Tu structures tes réponses en Markdown.
        """.trimIndent()

        val userPrompt = """
            Génère $typeLabel pour :
            - Titre / Sujet : ${req.titre}
            - Matière : ${req.matiere.ifBlank { "non précisée" }}
            - Niveau : ${req.niveau}
            - Filière : ${req.filiere.ifBlank { "générale" }}
            - Durée prévue : ${req.dureeMinutes} minutes
            - Contexte : ${req.context}
            
            Structure attendue selon le type :
            ${structureAttendueParType(req.type)}
        """.trimIndent()

        return try {
            val response = callLLM(systemPrompt, userPrompt)
            val contenu = response.choices.firstOrNull()?.message?.content ?: ""
            AIContentResponse(
                contenu        = contenu,
                titre          = req.titre,
                type           = req.type,
                niveau         = req.niveau,
                tokensUtilises = response.usage.total_tokens,
                modele         = model,
                fallback       = false
            )
        } catch (e: Exception) {
            log.error("Erreur appel LLM generateContent : ${e.message}")
            fallbackContent(req)
        }
    }

    // ── Génération appréciations bulletins ────────────────────────────────────

    suspend fun generateAppreciation(req: AIAppreciationRequest): AIAppreciationResponse {
        if (activeKey.isBlank()) {
            log.warn("Clé API IA non configurée — retour appréciation par défaut")
            return fallbackAppreciation(req)
        }

        val pronoun = if (req.genre == "F") "Elle" else "Il"

        val notesTexte = if (req.notesMatieres.isNotEmpty()) {
            req.notesMatieres.joinToString(", ") {
                "${it.matiere}: ${it.note}/${it.noteMax}"
            }
        } else "non détaillées"

        val systemPrompt = """
            Tu es un directeur d'école congolais rédigeant des appréciations de bulletins scolaires.
            Tes appréciations sont en français formel, bienveillantes mais objectives, courtes (2-3 phrases max),
            et tiennent compte du rang, de la moyenne et du comportement de l'élève.
            Ne jamais utiliser le nom de l'élève dans l'appréciation (remplace par "l'élève", "$pronoun", etc.).
            Format de réponse JSON strict : {"appreciation": "...", "mention": "...", "conseil": "..."}
        """.trimIndent()

        val userPrompt = """
            Élève : ${req.eleveNom} (${if (req.genre == "F") "fille" else "garçon"})
            Moyenne générale : ${req.moyenneGenerale}/20
            Rang : ${if (req.rang > 0) "${req.rang}e/${req.effectif}" else "non classé"}
            Absences : ${req.absences} demi-journée(s)
            Comportement : ${req.comportement}
            Notes par matière : $notesTexte
            
            Génère une appréciation de bulletin scolaire.
            La mention doit être : Excellent (≥16), Très Bien (≥14), Bien (≥12), Assez Bien (≥10), Passable (≥8), Insuffisant (<8)
        """.trimIndent()

        return try {
            val response = callLLM(systemPrompt, userPrompt, temperature = 0.5, maxTokens = 512)
            val content = response.choices.firstOrNull()?.message?.content ?: ""
            parseAppreciationJson(content, req)
        } catch (e: Exception) {
            log.error("Erreur appel LLM generateAppreciation : ${e.message}")
            fallbackAppreciation(req)
        }
    }

    // ── Appel LLM générique ───────────────────────────────────────────────────

    private fun callLLM(
        systemPrompt: String,
        userPrompt: String,
        temperature: Double = 0.7,
        maxTokens: Int = 2048
    ): LLMResponse {
        val payload = LLMRequest(
            model = model,
            messages = listOf(
                LLMMessage("system", systemPrompt),
                LLMMessage("user", userPrompt)
            ),
            temperature = temperature,
            max_tokens  = maxTokens
        )

        val body = json.writeValueAsString(payload).toRequestBody(mediaType)
        val request = Request.Builder()
            .url(apiUrl)
            .header("Authorization", "Bearer $activeKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("LLM HTTP ${response.code} : ${response.body?.string()?.take(200)}")
            }
            val responseBody = response.body?.string()
                ?: throw RuntimeException("Réponse LLM vide")
            return json.readValue(responseBody)
        }
    }

    // ── Parsers et fallbacks ──────────────────────────────────────────────────

    private fun parseAppreciationJson(content: String, req: AIAppreciationRequest): AIAppreciationResponse {
        return try {
            val clean = content.trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val map = json.readValue<Map<String, String>>(clean)
            AIAppreciationResponse(
                appreciation = map["appreciation"] ?: fallbackAppreciation(req).appreciation,
                mention      = map["mention"] ?: mentionFromMoyenne(req.moyenneGenerale),
                conseil      = map["conseil"] ?: "",
                fallback     = false
            )
        } catch (e: Exception) {
            fallbackAppreciation(req)
        }
    }

    private fun fallbackContent(req: AIContentRequest): AIContentResponse {
        val template = """
            # ${req.titre}
            **Niveau :** ${req.niveau} | **Matière :** ${req.matiere} | **Durée :** ${req.dureeMinutes} min
            
            *[Contenu à compléter — service IA non disponible]*
            
            ## Objectifs
            - Objectif 1
            - Objectif 2
            
            ## Développement
            *[Développer ici le contenu de la leçon]*
            
            ## Conclusion
            *[Synthèse et évaluation formative]*
        """.trimIndent()
        return AIContentResponse(
            contenu  = template,
            titre    = req.titre,
            type     = req.type,
            niveau   = req.niveau,
            fallback = true
        )
    }

    private fun fallbackAppreciation(req: AIAppreciationRequest): AIAppreciationResponse {
        val mention = mentionFromMoyenne(req.moyenneGenerale)
        val appreciation = when {
            req.moyenneGenerale >= 16 -> "Résultats excellents. Continuez dans cette voie remarquable."
            req.moyenneGenerale >= 14 -> "Très bons résultats. L'élève fait preuve de sérieux et de rigueur."
            req.moyenneGenerale >= 12 -> "Bons résultats dans l'ensemble. Des efforts supplémentaires consolideront ces acquis."
            req.moyenneGenerale >= 10 -> "Résultats satisfaisants. Un travail régulier permettra de progresser davantage."
            req.moyenneGenerale >= 8  -> "Résultats passables. L'élève doit fournir plus d'efforts pour atteindre le niveau requis."
            else                      -> "Résultats insuffisants. Un soutien scolaire renforcé est vivement recommandé."
        }
        return AIAppreciationResponse(
            appreciation = appreciation,
            mention      = mention,
            conseil      = if (req.absences > 5) "La régularité de présence doit être améliorée." else "",
            fallback     = true
        )
    }

    private fun mentionFromMoyenne(moy: Double) = when {
        moy >= 16 -> "Excellent"
        moy >= 14 -> "Très Bien"
        moy >= 12 -> "Bien"
        moy >= 10 -> "Assez Bien"
        moy >= 8  -> "Passable"
        else      -> "Insuffisant"
    }

    private fun structureAttendueParType(type: ContentType) = when (type) {
        ContentType.COURS         -> "Introduction, Prérequis, Développement (parties numérotées), Exemples, Exercices d'application, Conclusion/Résumé"
        ContentType.EXAMEN        -> "En-tête (durée, barème), Instructions, Partie I (QCM ou Vrai/Faux), Partie II (Questions ouvertes), Partie III (Problème ou Dissertation)"
        ContentType.DEVOIR        -> "Consignes, Exercices numérotés avec barème indicatif, Date de remise"
        ContentType.FICHE_REVISION -> "Points clés, Définitions essentielles, Formules/règles, Méthodes, Pièges à éviter"
        ContentType.PROGRESSION   -> "Tableau des séquences (semaine, titre, durée, objectifs, évaluation)"
    }
}
