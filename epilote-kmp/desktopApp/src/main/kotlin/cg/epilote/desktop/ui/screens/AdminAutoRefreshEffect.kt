package cg.epilote.desktop.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

@Composable
internal fun AdminAutoRefreshEffect(
    refreshKey: Any?,
    intervalMs: Long = 30_000L,
    onRefresh: suspend () -> Unit
) {
    LaunchedEffect(refreshKey, intervalMs) {
        while (true) {
            delay(intervalMs)
            onRefresh()
        }
    }
}
