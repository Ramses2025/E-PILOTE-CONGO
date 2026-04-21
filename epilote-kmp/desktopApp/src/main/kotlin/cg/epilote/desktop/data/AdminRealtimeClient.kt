package cg.epilote.desktop.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Serializable
data class AdminRealtimeEventDto(
    val eventId: String = "",
    val eventType: String = "",
    val channel: String = "",
    val entityType: String = "",
    val entityId: String? = null,
    val action: String = "",
    val path: String = "",
    val emittedAt: Long = 0L,
    val actorId: String? = null,
    val payload: JsonObject? = null
)

class AdminRealtimeClient(
    private val baseUrl: String,
    private val tokenProvider: () -> String?,
    private val onUnauthorized: suspend () -> Boolean = { false },
    private val lastEventIdProvider: () -> String? = { null },
    private val onReconnectNeeded: (suspend () -> Unit)? = null
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val httpClient = HttpClient {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = Long.MAX_VALUE  // SSE is long-lived
            socketTimeoutMillis = Long.MAX_VALUE   // no socket timeout for streaming
        }
    }

    suspend fun collect(onEvent: suspend (AdminRealtimeEventDto) -> Unit) {
        while (true) {
            val connected = connectOnce(onEvent)
            if (!connected) {
                delay(3_000)
            }
        }
    }

    private suspend fun connectOnce(onEvent: suspend (AdminRealtimeEventDto) -> Unit): Boolean {
        println("[SSE] Connecting to $baseUrl/api/super-admin/events/stream ...")
        val response = runCatching {
            httpClient.get("$baseUrl/api/super-admin/events/stream") {
                header(HttpHeaders.Accept, "text/event-stream")
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                lastEventIdProvider()?.let { header("Last-Event-ID", it) }
            }
        }.getOrElse { error ->
            println("[SSE] Connection failed: ${error.message}")
            return false
        }

        if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
            println("[SSE] Got ${response.status} — attempting token refresh")
            val refreshed = runCatching { onUnauthorized() }.getOrDefault(false)
            return refreshed
        }

        if (response.status.value !in 200..299) {
            println("[SSE] Unexpected status ${response.status}")
            return false
        }

        println("[SSE] Connected successfully — reading stream ...")
        val channel = response.bodyAsChannel()
        var eventName = "message"
        var dataBuffer = StringBuilder()
        while (!channel.isClosedForRead) {
            val line = runCatching { channel.readUTF8Line() }.getOrNull() ?: break
            when {
                line.startsWith("event:") -> {
                    eventName = line.substringAfter(':').trim()
                }
                line.startsWith("id:") -> {
                    // SSE id field — tracked via event DTO
                }
                line.startsWith("data:") -> {
                    dataBuffer.append(line.substringAfter(':').trim())
                }
                line.isBlank() -> {
                    // End of event — process buffered data
                    val payload = dataBuffer.toString().trim()
                    dataBuffer = StringBuilder()
                    if (payload.isBlank()) { eventName = "message"; continue }

                    when (eventName) {
                        "connected" -> println("[SSE] Server connected event received")
                        "heartbeat" -> { /* keep-alive, ignore */ }
                        "reconnect" -> {
                            println("[SSE] Reconnect hint — triggering full refresh")
                            onReconnectNeeded?.invoke()
                        }
                        else -> {
                            val event = runCatching { json.decodeFromString(AdminRealtimeEventDto.serializer(), payload) }
                                .getOrElse { error ->
                                    println("[SSE] Decode failed: ${error.message} — raw: ${payload.take(200)}")
                                    null
                                }
                            if (event != null) {
                                println("[SSE] Event: ${event.entityType}/${event.action} id=${event.entityId}")
                                onEvent(event)
                            }
                        }
                    }
                    eventName = "message"
                }
            }
        }

        println("[SSE] Stream closed — will reconnect")
        onReconnectNeeded?.invoke()
        return false
    }
}
