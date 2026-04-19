package cg.epilote.desktop.ui.theme

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.WindowPosition

/**
 * Returns a Modifier that makes an undecorated DialogWindow draggable.
 * Pass the DialogState from rememberDialogState().
 * Apply the returned modifier to the root composable inside DialogWindow content.
 */
@Composable
fun draggableDialog(state: DialogState): Modifier {
    var initialPosition by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current

    return Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = {
                val pos = state.position
                val x = when (pos) {
                    is WindowPosition.Absolute -> pos.x
                    else -> 0.dp
                }
                val y = when (pos) {
                    is WindowPosition.Absolute -> pos.y
                    else -> 0.dp
                }
                initialPosition = DpOffset(x, y)
            },
            onDrag = { change, dragAmount ->
                change.consume()
                val dx = with(density) { dragAmount.x.toDp() }
                val dy = with(density) { dragAmount.y.toDp() }
                val newPos = DpOffset(
                    x = initialPosition.x + dx,
                    y = initialPosition.y + dy
                )
                initialPosition = newPos
                state.position = WindowPosition.Absolute(newPos.x, newPos.y)
            }
        )
    }
}
