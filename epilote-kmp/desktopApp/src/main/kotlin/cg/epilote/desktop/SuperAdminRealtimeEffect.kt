package cg.epilote.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cg.epilote.desktop.data.AdminRealtimeClient
import cg.epilote.desktop.data.AdminRealtimeEventDto
import kotlinx.coroutines.delay

@Composable
internal fun SuperAdminRealtimeEffect(
    enabled: Boolean,
    client: AdminRealtimeClient,
    currentUserId: String,
    onEvent: suspend (AdminRealtimeEventDto) -> Unit
) {
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect

        var lastRefreshAt = 0L
        client.collect { event ->
            if (event.actorId == currentUserId) return@collect
            val now = System.currentTimeMillis()
            if (now - lastRefreshAt < 750L) {
                delay(750L - (now - lastRefreshAt))
            }
            lastRefreshAt = System.currentTimeMillis()
            onEvent(event)
        }
    }
}
