package cg.epilote.shared.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class ApiClient(
    private val baseUrl: String,
    private val tokenProvider: () -> String?,
    private val onTokenExpired: suspend () -> Unit
) {
    val http = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(Logging) {
            level = LogLevel.HEADERS
        }

        install(HttpTimeout) {
            requestTimeoutMillis  = 30_000
            connectTimeoutMillis  = 15_000
            socketTimeoutMillis   = 30_000
        }

        install(DefaultRequest) {
            url(baseUrl)
            contentType(ContentType.Application.Json)
            tokenProvider()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }

        HttpResponseValidator {
            validateResponse { response ->
                if (response.status == HttpStatusCode.Unauthorized) {
                    onTokenExpired()
                }
            }
        }
    }

    suspend inline fun <reified T> get(path: String): T =
        http.get(path).body()

    suspend inline fun <reified T, reified B : Any> post(path: String, body: B): T =
        http.post(path) { setBody(body) }.body()

    suspend inline fun <reified T, reified B : Any> put(path: String, body: B): T =
        http.put(path) { setBody(body) }.body()
}
