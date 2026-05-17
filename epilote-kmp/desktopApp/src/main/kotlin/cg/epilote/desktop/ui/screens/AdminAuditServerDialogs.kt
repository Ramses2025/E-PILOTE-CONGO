package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.AuditEventApiDto
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand

@Composable
internal fun AuditServerEntryDetailDialog(
    entry: AuditEventApiDto,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    AdminDialogWindow(
        title    = entry.actionLabel.ifBlank { entry.action },
        subtitle = "${entry.category}  ·  ${entry.outcome}",
        onDismiss = onDismiss,
        size = DpSize(760.dp, 580.dp),
        content = {
            Column(modifier = Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LineKv("Date",       formatDate(entry.timestamp))
                LineKv("Catégorie",  entry.category)
                LineKv("Action",     entry.action)
                LineKv("Résultat",   entry.outcome)
                LineKv("Acteur",     entry.actorEmail ?: entry.actorId ?: "—")
                entry.actorRole?.let   { LineKv("Rôle",        it) }
                entry.targetType?.let  { LineKv("Cible",       "${entry.targetType} ${entry.targetId ?: ""}") }
                entry.targetLabel?.let { LineKv("Label cible",  it) }
                entry.ipAddress?.let   { LineKv("IP",           it) }
                entry.userAgent?.let   { LineKv("User-Agent",   it) }
                entry.message?.let     { LineKv("Message",      it) }
                if (entry.metadata.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Métadonnées", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    entry.metadata.forEach { (k, v) ->
                        val display = if (v is kotlinx.serialization.json.JsonPrimitive && v.isString) v.content else v.toString()
                        LineKv(k, display)
                    }
                }
            }
        },
        actions = {
            OutlinedButton(
                onClick = {
                    val actor   = entry.actorEmail ?: entry.actorId ?: ""
                    val ip      = entry.ipAddress ?: ""
                    val message = entry.message ?: ""
                    val json = "{\"id\":\"${entry.id}\",\"timestamp\":${entry.timestamp}" +
                        ",\"action\":\"${entry.action}\",\"category\":\"${entry.category}\"" +
                        ",\"outcome\":\"${entry.outcome}\",\"actor\":\"$actor\"" +
                        ",\"ip\":\"$ip\",\"message\":\"$message\"}"
                    clipboardManager.setText(AnnotatedString(json))
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.cursorHand()
            ) { Text("Copier JSON") }
            TextButton(onClick = onDismiss, modifier = Modifier.cursorHand()) { Text("Fermer") }
        }
    )
}

@Composable
private fun LineKv(key: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$key :", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.width(120.dp))
        Text(value, fontSize = 12.sp, color = EpiloteTextMuted, modifier = Modifier.weight(1f))
    }
}
