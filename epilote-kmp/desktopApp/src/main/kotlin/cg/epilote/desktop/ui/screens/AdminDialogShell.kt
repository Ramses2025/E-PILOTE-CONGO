package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand

internal data class AdminFeedbackMessage(
    val message: String,
    val isError: Boolean = false
)

@Composable
internal fun AdminDialogWindow(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    size: DpSize,
    content: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    val dialogState = rememberDialogState(size = size)

    DialogWindow(
        onCloseRequest = onDismiss,
        title = title,
        state = dialogState,
        undecorated = true,
        resizable = false
    ) {
        WindowDraggableArea(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(14.dp),
                color = Color.White
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D3557))
                            Text(subtitle, fontSize = 12.sp, color = EpiloteTextMuted)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.cursorHand()) {
                            Icon(Icons.Default.Close, "Fermer")
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE9EEF5))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        content = content
                    )
                    HorizontalDivider(color = Color(0xFFE9EEF5))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions
                    )
                }
            }
        }
    }
}

@Composable
internal fun AdminFeedbackBanner(
    feedback: AdminFeedbackMessage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (feedback.isError) Color(0xFFFEE2E2) else Color(0xFFD1FAE5)
    val contentColor = if (feedback.isError) Color(0xFFB3261E) else Color(0xFF166534)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(feedback.message, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            IconButton(onClick = onDismiss, modifier = Modifier.cursorHand()) {
                Icon(Icons.Default.Close, "Fermer", tint = contentColor)
            }
        }
    }
}

@Composable
internal fun AdminConfirmationDialog(
    title: String,
    subtitle: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmContainerColor: Color,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    size: DpSize = DpSize(560.dp, 300.dp)
) {
    AdminDialogWindow(
        title = title,
        subtitle = subtitle,
        onDismiss = onDismiss,
        size = size,
        content = {
            Text(message, fontSize = 13.sp, color = Color(0xFF334155))
            errorMessage?.let {
                AdminFeedbackBanner(
                    feedback = AdminFeedbackMessage(it, isError = true),
                    onDismiss = {}
                )
            }
        },
        actions = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting, modifier = Modifier.cursorHand()) {
                Text("Annuler")
            }
            Row(modifier = Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onConfirm,
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = confirmContainerColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Row(modifier = Modifier.padding(start = 8.dp)) {
                            Text("Traitement…")
                        }
                    } else {
                        Text(confirmLabel)
                    }
                }
            }
        }
    )
}
