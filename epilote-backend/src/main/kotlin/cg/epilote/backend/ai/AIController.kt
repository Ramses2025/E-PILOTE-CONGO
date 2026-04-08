package cg.epilote.backend.ai

import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
class AIController(private val aiService: AIService) {

    // ── Génération de contenu pédagogique (cours, examen, devoir…) ────────────

    @PostMapping("/api/ai/generate-content")
    @PreAuthorize("isAuthenticated()")
    fun generateContent(
        @Valid @RequestBody req: AIContentRequest
    ): ResponseEntity<AIContentResponse> = runBlocking {
        ResponseEntity.ok(aiService.generateContent(req))
    }

    // ── Génération d'appréciation bulletin ───────────────────────────────────

    @PostMapping("/api/ai/generate-appreciation")
    @PreAuthorize("isAuthenticated()")
    fun generateAppreciation(
        @Valid @RequestBody req: AIAppreciationRequest
    ): ResponseEntity<AIAppreciationResponse> = runBlocking {
        ResponseEntity.ok(aiService.generateAppreciation(req))
    }
}
