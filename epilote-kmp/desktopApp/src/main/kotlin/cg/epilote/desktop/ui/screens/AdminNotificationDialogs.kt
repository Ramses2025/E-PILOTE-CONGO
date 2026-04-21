package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand

@Composable
internal fun AnnouncementComposeDialog(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (title: String, content: String, target: String) -> Unit
) {
    val scrollState = rememberScrollState()
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("all") }
    var targetMenuExpanded by remember { mutableStateOf(false) }
    val targetOptions = listOf(
        "all" to "Toute la plateforme",
        "groupes" to "Tous les groupes scolaires",
        "billing" to "Facturation / abonnements",
        "admins" to "Administrateurs plateforme"
    )
    val canSubmit = title.isNotBlank() && content.isNotBlank()

    AdminDialogWindow(
        title = "Nouvelle diffusion",
        subtitle = "Publier une annonce globale depuis l'espace super-admin",
        onDismiss = onDismiss,
        size = DpSize(700.dp, 560.dp),
        content = {
            Column(modifier = Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Titre") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Campaign, null) }
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Cible", fontSize = 12.sp, color = EpiloteTextMuted)
                    OutlinedButton(onClick = { targetMenuExpanded = true }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(12.dp)) {
                        Text(targetOptions.firstOrNull { it.first == target }?.second ?: "Toute la plateforme", modifier = Modifier.fillMaxWidth())
                    }
                    DropdownMenu(expanded = targetMenuExpanded, onDismissRequest = { targetMenuExpanded = false }) {
                        targetOptions.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                target = key
                                targetMenuExpanded = false
                            })
                        }
                    }
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    maxLines = 10,
                    label = { Text("Contenu") },
                    shape = RoundedCornerShape(12.dp)
                )
                errorMessage?.let {
                    AdminFeedbackBanner(feedback = AdminFeedbackMessage(it, isError = true), onDismiss = {})
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting, modifier = Modifier.cursorHand()) {
                Text("Annuler")
            }
            Button(
                onClick = { onSubmit(title.trim(), content.trim(), target) },
                enabled = !isSubmitting && canSubmit,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D3557)),
                modifier = Modifier.cursorHand()
            ) {
                Text("Publier")
            }
        }
    )
}

@Composable
internal fun NotificationDetailDialog(
    item: AdminNotificationItem,
    onDismiss: () -> Unit,
    onOpenPdf: (() -> Unit)?,
    onExportPdf: (() -> Unit)?,
    onSharePdf: (() -> Unit)?,
    documentFeedback: AdminFeedbackMessage?,
    onDismissDocumentFeedback: () -> Unit,
    isDocumentProcessing: Boolean
) {
    val scrollState = rememberScrollState()
    val severityColor = notificationSeverityColor(item.severity)
    val sourceColor = notificationSourceColor(item.sourceType)

    AdminDialogWindow(
        title = item.title,
        subtitle = item.relatedReference.ifBlank { item.targetLabel },
        onDismiss = onDismiss,
        size = DpSize(700.dp, 560.dp),
        content = {
            Column(modifier = Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(999.dp), color = severityColor.copy(alpha = 0.10f)) {
                        Text(notificationSeverityLabel(item.severity), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 11.sp, color = severityColor, fontWeight = FontWeight.SemiBold)
                    }
                    Surface(shape = RoundedCornerShape(999.dp), color = sourceColor.copy(alpha = 0.10f)) {
                        Text(notificationSourceLabel(item.sourceType), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 11.sp, color = sourceColor, fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(color = Color(0xFFE9EEF5))
                NotificationDetailRow("Cible", item.targetLabel)
                NotificationDetailRow("Date", formatDate(item.createdAt))
                item.relatedReference.takeIf { it.isNotBlank() }?.let {
                    NotificationDetailRow("Référence liée", it)
                }
                item.relatedStatus.takeIf { it.isNotBlank() }?.let {
                    NotificationDetailRow("Statut lié", it)
                }
                documentFeedback?.let { feedback ->
                    AdminFeedbackBanner(feedback = feedback, onDismiss = onDismissDocumentFeedback)
                }
                Text("Description", fontSize = 12.sp, color = EpiloteTextMuted)
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF8FAFC)) {
                    Text(item.message, modifier = Modifier.fillMaxWidth().padding(14.dp), fontSize = 13.sp, color = Color(0xFF334155))
                }
                if (onOpenPdf != null && onExportPdf != null && onSharePdf != null) {
                    HorizontalDivider(color = Color(0xFFE9EEF5))
                    Text("Document lié", fontSize = 12.sp, color = EpiloteTextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpenPdf, enabled = !isDocumentProcessing, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                            Text("Prévisualiser PDF", modifier = Modifier.padding(start = 6.dp))
                        }
                        OutlinedButton(onClick = onExportPdf, enabled = !isDocumentProcessing, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Text("Exporter", modifier = Modifier.padding(start = 6.dp))
                        }
                        OutlinedButton(onClick = onSharePdf, enabled = !isDocumentProcessing, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                            Text("Partager", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }
        },
        actions = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp), modifier = Modifier.cursorHand()) {
                Text("Fermer")
            }
        }
    )
}

@Composable
internal fun AnnouncementDetailDialog(
    item: cg.epilote.desktop.data.AnnouncementApiDto,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    AdminDialogWindow(
        title = item.titre.ifBlank { "Diffusion globale" },
        subtitle = announcementTargetLabel(item.cible),
        onDismiss = onDismiss,
        size = DpSize(700.dp, 520.dp),
        content = {
            Column(modifier = Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NotificationDetailRow("Cible", announcementTargetLabel(item.cible))
                NotificationDetailRow("Publication", formatDate(item.createdAt))
                item.createdBy.takeIf { it.isNotBlank() }?.let {
                    NotificationDetailRow("Publié par", it)
                }
                Text("Contenu", fontSize = 12.sp, color = EpiloteTextMuted)
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF8FAFC)) {
                    Text(item.contenu, modifier = Modifier.fillMaxWidth().padding(14.dp), fontSize = 13.sp, color = Color(0xFF334155))
                }
            }
        },
        actions = {
            Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp), modifier = Modifier.cursorHand()) {
                Text("Fermer")
            }
        }
    )
}

@Composable
private fun NotificationDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = EpiloteTextMuted)
        Text(value, fontSize = 13.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
    }
}
