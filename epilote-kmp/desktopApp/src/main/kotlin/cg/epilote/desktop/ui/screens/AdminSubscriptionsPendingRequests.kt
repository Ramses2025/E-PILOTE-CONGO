package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.*
import cg.epilote.desktop.ui.screens.superadmin.formatDate
import cg.epilote.desktop.ui.theme.cursorHand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ── Demandes d'abonnement en attente (Super Admin → Abonnements) ─────────────

@Composable
internal fun PendingSubscriptionRequestsSection(
    requests: List<SubscriptionRequestApiDto>,
    scope: CoroutineScope,
    client: DesktopAdminClient,
    onApproved: (SubscriptionRequestApiDto) -> Unit,
    onResolved: () -> Unit
) {
    if (requests.isEmpty()) return

    var resolveTarget by remember { mutableStateOf<Pair<SubscriptionRequestApiDto, String>?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Inbox, null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                    Text("Demandes en attente", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                }
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFD97706)) {
                    Text(
                        "${requests.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFFDE68A))

            requests.forEachIndexed { idx, req ->
                PendingRequestRow(
                    request   = req,
                    onApprove = { resolveTarget = req to "approved" },
                    onReject  = { resolveTarget = req to "rejected" }
                )
                if (idx < requests.lastIndex) HorizontalDivider(color = Color(0xFFFEF3C7))
            }
        }
    }

    resolveTarget?.let { (req, action) ->
        ResolveRequestDialog(
            request   = req,
            action    = action,
            scope     = scope,
            client    = client,
            onDismiss = { resolveTarget = null },
            onSuccess = {
                resolveTarget = null
                onResolved()
                if (action == "approved") onApproved(req)
            }
        )
    }
}

// ── Ligne d'une demande ──────────────────────────────────────────────────────

@Composable
private fun PendingRequestRow(
    request: SubscriptionRequestApiDto,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val isRenewal = request.requestType == "RENEWAL_REQUEST"
    val typeColor = if (isRenewal) Color(0xFF0EA5E9) else Color(0xFF7C3AED)
    val typeIcon  = if (isRenewal) Icons.Default.Autorenew else Icons.Default.SwapHoriz

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(shape = RoundedCornerShape(8.dp), color = typeColor.copy(alpha = 0.10f)) {
                Icon(typeIcon, null, tint = typeColor, modifier = Modifier.padding(6.dp).size(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    request.groupeNom.ifBlank { request.groupeId },
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(4.dp), color = typeColor.copy(alpha = 0.10f)) {
                        Text(
                            request.typeLabel.ifBlank { request.requestType },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = typeColor
                        )
                    }
                    Text(formatDate(request.createdAt), fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
                if (!request.message.isNullOrBlank()) {
                    val preview = request.message
                        .lines()
                        .dropWhile { it.startsWith("Groupe :") || it.startsWith("Type de demande :") || it.isBlank() }
                        .joinToString(" ")
                        .trim()
                        .take(120)
                    if (preview.isNotBlank()) {
                        Text(
                            preview, fontSize = 11.sp, color = Color(0xFF64748B),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onReject,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.cursorHand(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB91C1C)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Rejeter", fontSize = 12.sp)
            }
            Button(
                onClick = onApprove,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.cursorHand(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Approuver", fontSize = 12.sp)
            }
        }
    }
}

// ── Dialogue confirmation approve/reject ─────────────────────────────────────

@Composable
private fun ResolveRequestDialog(
    request: SubscriptionRequestApiDto,
    action: String,
    scope: CoroutineScope,
    client: DesktopAdminClient,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val isApprove = action == "approved"
    var notes by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AdminDialogWindow(
        title     = if (isApprove) "Approuver la demande" else "Rejeter la demande",
        subtitle  = "${request.groupeNom} — ${request.typeLabel}",
        onDismiss = { if (!isSubmitting) onDismiss() },
        size      = DpSize(500.dp, 310.dp),
        content   = {
            errorMsg?.let { Text(it, fontSize = 12.sp, color = Color(0xFFB91C1C)) }
            Text(
                if (isApprove)
                    "La demande sera marquée approuvée, puis l'action adaptée s'ouvrira automatiquement."
                else
                    "La demande sera marquée rejetée. Ajoutez un motif si souhaité.",
                fontSize = 13.sp, color = Color(0xFF475569)
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 300) notes = it },
                modifier = Modifier.fillMaxWidth().height(90.dp),
                label = {
                    Text(
                        if (isApprove) "Note interne (optionnel)" else "Motif du rejet (optionnel)",
                        fontSize = 12.sp
                    )
                },
                maxLines = 4,
                supportingText = { Text("${notes.length}/300", fontSize = 11.sp, color = Color(0xFF94A3B8)) }
            )
        },
        actions = {
            TextButton(onClick = { if (!isSubmitting) onDismiss() }, modifier = Modifier.cursorHand()) {
                Text("Annuler")
            }
            Button(
                onClick = {
                    scope.launch {
                        isSubmitting = true
                        errorMsg = null
                        val ok = runCatching {
                            client.resolveSubscriptionRequest(
                                id     = request.id,
                                action = action,
                                notes  = notes.takeIf { it.isNotBlank() }
                            )
                        }.getOrDefault(false)
                        isSubmitting = false
                        if (ok) onSuccess()
                        else errorMsg = "Impossible de traiter la demande. Vérifiez votre connexion."
                    }
                },
                enabled  = !isSubmitting,
                shape    = RoundedCornerShape(10.dp),
                modifier = Modifier.cursorHand(),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (isApprove) Color(0xFF059669) else Color(0xFFB91C1C)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Text(if (isApprove) "Confirmer l'approbation" else "Confirmer le rejet", fontSize = 13.sp)
            }
        }
    )
}
