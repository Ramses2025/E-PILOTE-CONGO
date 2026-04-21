package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.AdminMessageApiDto
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand

@Composable
internal fun AdminMessageComposeDialog(
    groupes: List<GroupeDto>,
    admins: List<AdminUserDto>,
    initialTargetType: String = "all_groups",
    initialGroupId: String? = null,
    initialAdminId: String? = null,
    initialSubject: String = "",
    initialContent: String = "",
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (subject: String, content: String, targetType: String, groupId: String?, adminId: String?) -> Unit
) {
    val scrollState = rememberScrollState()
    val targetOptions = listOf(
        "all_groups" to "Tous les groupes scolaires",
        "group" to "Un groupe scolaire",
        "all_admins" to "Tous les administrateurs",
        "admin" to "Un administrateur"
    )
    var subject by remember(initialSubject) { mutableStateOf(initialSubject) }
    var content by remember(initialContent) { mutableStateOf(initialContent) }
    var targetType by remember { mutableStateOf(initialTargetType) }
    var groupId by remember { mutableStateOf(initialGroupId.orEmpty()) }
    var adminId by remember { mutableStateOf(initialAdminId.orEmpty()) }
    var targetMenuExpanded by remember { mutableStateOf(false) }
    var groupMenuExpanded by remember { mutableStateOf(false) }
    var adminMenuExpanded by remember { mutableStateOf(false) }
    val canSubmit = subject.isNotBlank() && content.isNotBlank() && when (targetType) {
        "group" -> groupId.isNotBlank()
        "admin" -> adminId.isNotBlank()
        else -> true
    }

    AdminDialogWindow(
        title = "Nouveau message",
        subtitle = "Communication ciblée depuis l'espace super-admin",
        onDismiss = onDismiss,
        size = DpSize(720.dp, 600.dp),
        content = {
            Column(modifier = Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Sujet") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Cible", fontSize = 12.sp, color = EpiloteTextMuted)
                    OutlinedButton(onClick = { targetMenuExpanded = true }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(12.dp)) {
                        Text(targetOptions.firstOrNull { it.first == targetType }?.second ?: "Sélectionner une cible", modifier = Modifier.fillMaxWidth())
                    }
                    DropdownMenu(expanded = targetMenuExpanded, onDismissRequest = { targetMenuExpanded = false }) {
                        targetOptions.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                targetType = key
                                if (key != "group") groupId = ""
                                if (key != "admin") adminId = ""
                                targetMenuExpanded = false
                            })
                        }
                    }
                }
                if (targetType == "group") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Groupe scolaire", fontSize = 12.sp, color = EpiloteTextMuted)
                        OutlinedButton(onClick = { groupMenuExpanded = true }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(12.dp)) {
                            Text(groupes.firstOrNull { it.id == groupId }?.nom ?: "Sélectionner un groupe", modifier = Modifier.fillMaxWidth())
                        }
                        DropdownMenu(expanded = groupMenuExpanded, onDismissRequest = { groupMenuExpanded = false }) {
                            groupes.sortedBy { it.nom.lowercase() }.forEach { group ->
                                DropdownMenuItem(text = { Text(group.nom) }, onClick = {
                                    groupId = group.id
                                    groupMenuExpanded = false
                                })
                            }
                        }
                    }
                }
                if (targetType == "admin") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Administrateur", fontSize = 12.sp, color = EpiloteTextMuted)
                        OutlinedButton(onClick = { adminMenuExpanded = true }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(12.dp)) {
                            Text(admins.firstOrNull { it.id == adminId }?.let(::adminDisplayName) ?: "Sélectionner un administrateur", modifier = Modifier.fillMaxWidth())
                        }
                        DropdownMenu(expanded = adminMenuExpanded, onDismissRequest = { adminMenuExpanded = false }) {
                            admins.sortedBy { adminDisplayName(it).lowercase() }.forEach { admin ->
                                DropdownMenuItem(text = { Text(adminDisplayName(admin)) }, onClick = {
                                    adminId = admin.id
                                    adminMenuExpanded = false
                                })
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    maxLines = 10,
                    label = { Text("Message") },
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
                onClick = {
                    onSubmit(
                        subject.trim(),
                        content.trim(),
                        targetType,
                        groupId.takeIf { targetType == "group" && it.isNotBlank() },
                        adminId.takeIf { targetType == "admin" && it.isNotBlank() }
                    )
                },
                enabled = !isSubmitting && canSubmit,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D3557)),
                modifier = Modifier.cursorHand()
            ) {
                Text("Envoyer")
            }
        }
    )
}

@Composable
internal fun AdminMessageThreadDialog(
    thread: AdminMessageThread,
    onDismiss: () -> Unit,
    onReply: () -> Unit
) {
    val scrollState = rememberScrollState()
    AdminDialogWindow(
        title = thread.targetLabel,
        subtitle = "${thread.messageCount} message(s)",
        onDismiss = onDismiss,
        size = DpSize(760.dp, 620.dp),
        content = {
            Column(modifier = Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                thread.messages.forEach { message ->
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF8FAFC)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(message.sujet, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text(message.contenu, fontSize = 13.sp, color = Color(0xFF334155))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(message.createdBy.ifBlank { "Super-admin" }, fontSize = 11.sp, color = Color(0xFF1D3557), fontWeight = FontWeight.Medium)
                                Text(formatDate(message.createdAt), fontSize = 11.sp, color = EpiloteTextMuted)
                            }
                        }
                    }
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss, modifier = Modifier.cursorHand()) {
                Text("Fermer")
            }
            Button(onClick = onReply, shape = RoundedCornerShape(10.dp), modifier = Modifier.cursorHand()) {
                Text("Répondre")
            }
        }
    )
}
