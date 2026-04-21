package cg.epilote.backend.admin

import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Component
class AdminRealtimeBroker {
    private val emitters = ConcurrentHashMap<String, SseEmitter>()
    private val recentEvents = ArrayDeque<AdminRealtimeEvent>()
    private val eventBufferLock = Any()
    private val heartbeatScheduler = Executors.newSingleThreadScheduledExecutor()
    private val heartbeatIntervalSeconds = 15L
    private val maxBufferedEvents = 512

    init {
        heartbeatScheduler.scheduleAtFixedRate(
            { emitHeartbeat() },
            heartbeatIntervalSeconds,
            heartbeatIntervalSeconds,
            TimeUnit.SECONDS
        )
    }

    fun subscribe(clientId: String, lastEventId: String? = null): SseEmitter {
        val emitter = SseEmitter(0L)
        emitters[clientId] = emitter

        emitter.onCompletion { emitters.remove(clientId) }
        emitter.onTimeout {
            emitters.remove(clientId)
            emitter.complete()
        }
        emitter.onError {
            emitters.remove(clientId)
            emitter.complete()
        }

        runCatching {
            emitter.send(
                SseEmitter.event()
                    .name("connected")
                    .id(clientId)
                    .data(mapOf("connectedAt" to System.currentTimeMillis()))
            )
        }.onFailure {
            emitters.remove(clientId)
            emitter.completeWithError(it)
        }

        if (lastEventId != null) {
            val replayed = replayMissedEvents(emitter, lastEventId)
            if (!replayed) {
                runCatching {
                    emitter.send(
                        SseEmitter.event()
                            .name("reconnect")
                            .id("reconnect-${System.currentTimeMillis()}")
                            .data(mapOf("lastEventId" to lastEventId, "hint" to "refresh"))
                    )
                }
            }
        }

        return emitter
    }

    fun publish(event: AdminRealtimeEvent) {
        bufferEvent(event)
        emitters.entries.forEach { entry ->
            try {
                sendEvent(entry.value, event)
            } catch (_: IOException) {
                emitters.remove(entry.key)
                entry.value.complete()
            } catch (_: IllegalStateException) {
                emitters.remove(entry.key)
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        heartbeatScheduler.shutdownNow()
    }

    private fun replayMissedEvents(emitter: SseEmitter, lastEventId: String): Boolean {
        val eventsToReplay = synchronized(eventBufferLock) {
            val snapshot = recentEvents.toList()
            val lastSeenIndex = snapshot.indexOfFirst { it.eventId == lastEventId }
            if (lastSeenIndex == -1) null else snapshot.drop(lastSeenIndex + 1)
        } ?: return false

        return runCatching {
            eventsToReplay.forEach { event ->
                sendEvent(emitter, event)
            }
        }.isSuccess
    }

    private fun bufferEvent(event: AdminRealtimeEvent) {
        synchronized(eventBufferLock) {
            if (recentEvents.size >= maxBufferedEvents) {
                recentEvents.removeFirst()
            }
            recentEvents.addLast(event)
        }
    }

    private fun sendEvent(emitter: SseEmitter, event: AdminRealtimeEvent) {
        emitter.send(
            SseEmitter.event()
                .name(event.eventType)
                .id(event.eventId)
                .data(event)
        )
    }

    private fun emitHeartbeat() {
        emitters.entries.forEach { entry ->
            try {
                entry.value.send(
                    SseEmitter.event()
                        .name("heartbeat")
                        .data(mapOf("sentAt" to System.currentTimeMillis()))
                )
            } catch (_: IOException) {
                emitters.remove(entry.key)
                entry.value.complete()
            } catch (_: IllegalStateException) {
                emitters.remove(entry.key)
            }
        }
    }
}
