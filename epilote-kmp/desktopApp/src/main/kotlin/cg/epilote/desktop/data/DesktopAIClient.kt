package cg.epilote.desktop.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class DesktopAIClient(
    private val baseUrl: String,
    private val tokenProvider: () -> String?
) {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }

    suspend fun generateContent(req: AIContentRequestDto): AIContentResponseDto? =
        runCatching {
            httpClient.post("$baseUrl/api/ai/generate-content") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(req)
            }.body<AIContentResponseDto>()
        }.getOrNull()

    suspend fun generateAppreciation(req: AIAppreciationRequestDto): AIAppreciationResponseDto? =
        runCatching {
            httpClient.post("$baseUrl/api/ai/generate-appreciation") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(req)
            }.body<AIAppreciationResponseDto>()
        }.getOrNull()
}
