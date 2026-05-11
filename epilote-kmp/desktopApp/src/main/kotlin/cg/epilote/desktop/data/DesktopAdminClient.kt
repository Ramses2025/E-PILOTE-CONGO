package cg.epilote.desktop.data
import java.util.logging.Logger

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DesktopAdminClient(
    internal val baseUrl: String,
    internal val tokenProvider: () -> String?,
    private val onUnauthorized: suspend () -> Boolean = { false }
) {
    private val log = Logger.getLogger(DesktopAdminClient::class.java.name)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    data class ApiProblemDto(
        val detail: String? = null,
        val title: String? = null,
        val message: String? = null,
        val error: String? = null
    )

    sealed interface ApiCallResult<out T> {
        data class Success<T>(val value: T) : ApiCallResult<T>
        data class Failure(val statusCode: Int, val message: String?) : ApiCallResult<Nothing>
    }

    internal val httpClient = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
    }

    /**
     * Sémantique HTTP officielle (RFC 7235 / RFC 7231) :
     *  - 401 Unauthorized : credentials manquants ou expirés → tenter un refresh.
     *  - 403 Forbidden    : authentifié mais non autorisé → un refresh ne changera
     *                       pas le rôle, donc inutile (et masque l'erreur métier
     *                       réelle). On laisse la 403 remonter.
     */
    internal fun shouldAttemptRefresh(status: HttpStatusCode): Boolean =
        status == HttpStatusCode.Unauthorized

    internal fun extractErrorMessage(status: HttpStatusCode, body: String): String? {
        val trimmed = body.trim()
        if (trimmed.isBlank()) {
            return status.description.takeIf { it.isNotBlank() }
        }
        val parsed = runCatching { json.decodeFromString(ApiProblemDto.serializer(), trimmed) }.getOrNull()
        return parsed?.detail?.takeIf { it.isNotBlank() }
            ?: parsed?.message?.takeIf { it.isNotBlank() }
            ?: parsed?.error?.takeIf { it.isNotBlank() }
            ?: parsed?.title?.takeIf { it.isNotBlank() }
            ?: trimmed.take(300)
    }

    internal suspend inline fun <reified T> execute(
        method: String,
        path: String,
        crossinline request: suspend () -> HttpResponse
    ): T? {
        val firstResponse = runCatching { request() }.getOrElse { error ->
            log.warning("DesktopAdminClient $method $path request failed: ${error.message}")
            return null
        }

        if (shouldAttemptRefresh(firstResponse.status)) {
            log.fine("DesktopAdminClient $method $path returned ${firstResponse.status}; attempting token refresh")
            val refreshed = runCatching { onUnauthorized() }.getOrDefault(false)
            if (refreshed) {
                val retryResponse = runCatching { request() }.getOrElse { error ->
                    log.warning("DesktopAdminClient $method $path retry failed: ${error.message}")
                    return null
                }
                if (retryResponse.status.value in 200..299) {
                    return runCatching {
                        retryResponse.body<T>()
                    }.getOrElse { error ->
                        log.warning("DesktopAdminClient $method $path decode failed: ${error.message}")
                        null
                    }
                }
                log.fine("DesktopAdminClient $method $path retry returned ${retryResponse.status}")
                return null
            }
            return null
        }

        if (firstResponse.status.value !in 200..299) {
            val errBody = runCatching { firstResponse.bodyAsText() }.getOrNull().orEmpty().take(300)
            log.warning("DesktopAdminClient $method $path returned ${firstResponse.status} — $errBody")
            return null
        }

        return runCatching {
            firstResponse.body<T>()
        }.getOrElse { error ->
            log.warning("DesktopAdminClient $method $path decode failed: ${error.message}")
            null
        }
    }

    internal suspend inline fun <reified T> executeResult(
        method: String,
        path: String,
        crossinline request: suspend () -> HttpResponse
    ): ApiCallResult<T> {
        val firstResponse = runCatching { request() }.getOrElse { error ->
            log.warning("DesktopAdminClient $method $path request failed: ${error.message}")
            return ApiCallResult.Failure(0, error.message)
        }

        if (shouldAttemptRefresh(firstResponse.status)) {
            log.fine("DesktopAdminClient $method $path returned ${firstResponse.status}; attempting token refresh")
            val refreshed = runCatching { onUnauthorized() }.getOrDefault(false)
            if (refreshed) {
                val retryResponse = runCatching { request() }.getOrElse { error ->
                    log.warning("DesktopAdminClient $method $path retry failed: ${error.message}")
                    return ApiCallResult.Failure(0, error.message)
                }
                if (retryResponse.status.value in 200..299) {
                    return runCatching {
                        ApiCallResult.Success(retryResponse.body<T>())
                    }.getOrElse { error ->
                        log.warning("DesktopAdminClient $method $path decode failed: ${error.message}")
                        ApiCallResult.Failure(retryResponse.status.value, "Réponse invalide du serveur.")
                    }
                }
                val retryBody = runCatching { retryResponse.bodyAsText() }.getOrNull().orEmpty()
                log.warning("DesktopAdminClient $method $path retry returned ${retryResponse.status} — ${retryBody.take(300)}")
                return ApiCallResult.Failure(retryResponse.status.value, extractErrorMessage(retryResponse.status, retryBody))
            }
            return ApiCallResult.Failure(firstResponse.status.value, "Session expirée.")
        }

        if (firstResponse.status.value !in 200..299) {
            val errBody = runCatching { firstResponse.bodyAsText() }.getOrNull().orEmpty()
            log.warning("DesktopAdminClient $method $path returned ${firstResponse.status} — ${errBody.take(300)}")
            return ApiCallResult.Failure(firstResponse.status.value, extractErrorMessage(firstResponse.status, errBody))
        }

        return runCatching {
            ApiCallResult.Success(firstResponse.body<T>())
        }.getOrElse { error ->
            log.warning("DesktopAdminClient $method $path decode failed: ${error.message}")
            ApiCallResult.Failure(firstResponse.status.value, "Réponse invalide du serveur.")
        }
    }

    internal suspend fun executeStatus(
        method: String,
        path: String,
        request: suspend () -> HttpResponse
    ): Boolean {
        val firstResponse = runCatching { request() }.getOrElse { error ->
            log.warning("DesktopAdminClient $method $path request failed: ${error.message}")
            return false
        }

        if (shouldAttemptRefresh(firstResponse.status)) {
            log.fine("DesktopAdminClient $method $path returned ${firstResponse.status}; attempting token refresh")
            val refreshed = runCatching { onUnauthorized() }.getOrDefault(false)
            if (refreshed) {
                val retryResponse = runCatching { request() }.getOrElse { error ->
                    log.warning("DesktopAdminClient $method $path retry failed: ${error.message}")
                    return false
                }
                return retryResponse.status.value in 200..299
            }
            return false
        }

        return firstResponse.status.value in 200..299
    }

    internal suspend fun executeStatusResult(
        method: String,
        path: String,
        request: suspend () -> HttpResponse
    ): ApiCallResult<Unit> {
        val firstResponse = runCatching { request() }.getOrElse { error ->
            log.warning("DesktopAdminClient $method $path request failed: ${error.message}")
            return ApiCallResult.Failure(0, error.message)
        }

        if (shouldAttemptRefresh(firstResponse.status)) {
            log.fine("DesktopAdminClient $method $path returned ${firstResponse.status}; attempting token refresh")
            val refreshed = runCatching { onUnauthorized() }.getOrDefault(false)
            if (refreshed) {
                val retryResponse = runCatching { request() }.getOrElse { error ->
                    log.warning("DesktopAdminClient $method $path retry failed: ${error.message}")
                    return ApiCallResult.Failure(0, error.message)
                }
                if (retryResponse.status.value in 200..299) {
                    return ApiCallResult.Success(Unit)
                }
                val retryBody = runCatching { retryResponse.bodyAsText() }.getOrNull().orEmpty()
                log.warning("DesktopAdminClient $method $path retry returned ${retryResponse.status} — ${retryBody.take(300)}")
                return ApiCallResult.Failure(retryResponse.status.value, extractErrorMessage(retryResponse.status, retryBody))
            }
            return ApiCallResult.Failure(firstResponse.status.value, "Session expirée.")
        }

        if (firstResponse.status.value !in 200..299) {
            val errBody = runCatching { firstResponse.bodyAsText() }.getOrNull().orEmpty()
            log.warning("DesktopAdminClient $method $path returned ${firstResponse.status} — ${errBody.take(300)}")
            return ApiCallResult.Failure(firstResponse.status.value, extractErrorMessage(firstResponse.status, errBody))
        }

        return ApiCallResult.Success(Unit)
    }

    internal suspend fun executeBytes(
        method: String,
        path: String,
        request: suspend () -> HttpResponse
    ): ByteArray? {
        val firstResponse = runCatching { request() }.getOrElse { error ->
            log.warning("DesktopAdminClient $method $path request failed: ${error.message}")
            return null
        }

        if (shouldAttemptRefresh(firstResponse.status)) {
            log.fine("DesktopAdminClient $method $path returned ${firstResponse.status}; attempting token refresh")
            val refreshed = runCatching { onUnauthorized() }.getOrDefault(false)
            if (refreshed) {
                val retryResponse = runCatching { request() }.getOrElse { error ->
                    log.warning("DesktopAdminClient $method $path retry failed: ${error.message}")
                    return null
                }
                if (retryResponse.status.value in 200..299) {
                    return runCatching { retryResponse.body<ByteArray>() }.getOrElse { error ->
                        log.warning("DesktopAdminClient $method $path byte decode failed: ${error.message}")
                        null
                    }
                }
                log.fine("DesktopAdminClient $method $path retry returned ${retryResponse.status}")
                return null
            }
            return null
        }

        if (firstResponse.status.value !in 200..299) {
            log.fine("DesktopAdminClient $method $path returned ${firstResponse.status}")
            return null
        }

        return runCatching { firstResponse.body<ByteArray>() }.getOrElse { error ->
            log.warning("DesktopAdminClient $method $path byte decode failed: ${error.message}")
            null
        }
    }

    internal suspend inline fun <reified T> get(path: String): T? =
        execute("GET", path) {
            httpClient.get("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
        }

    internal suspend inline fun <reified T, reified B : Any> post(path: String, body: B): T? =
        execute("POST", path) {
            httpClient.post("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(body)
            }
        }

    internal suspend inline fun <reified T, reified B : Any> postResult(path: String, body: B): ApiCallResult<T> =
        executeResult("POST", path) {
            httpClient.post("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(body)
            }
        }

    internal suspend inline fun <reified T> delete(path: String): T? =
        execute("DELETE", path) {
            httpClient.delete("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
        }

    internal suspend fun deleteResult(path: String): ApiCallResult<Unit> =
        executeStatusResult("DELETE", path) {
            httpClient.delete("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
        }

    internal suspend inline fun <reified T, reified B : Any> put(path: String, body: B): T? =
        execute("PUT", path) {
            httpClient.put("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setBody(body)
            }
        }

}
