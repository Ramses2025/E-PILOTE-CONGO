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
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
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
internal fun SubscriptionDetailDialog(
    subscription: SubscriptionDto,
    onDismiss: () -> Unit,
    onChangePlan: () -> Unit,
    onChangeStatus: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val statusColor = subscriptionStatusColor(subscription.statut)
    val accentColor = planColorById(subscription.planId)
    val nextStatus = when (subscription.statut.lowercase()) {
        "active" -> "suspended"
        "suspended" -> "active"
        else -> "active"
    }

    AdminDialogWindow(
        title = subscription.groupeNom,
        subtitle = "${subscription.planNom} • ${formatMoneyXaf(subscription.prixXAF)}",
        onDismiss = onDismiss,
        size = DpSize(660.dp, 540.dp),
        content = {
            Column(modifier = Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.10f)) {
                        Text(subscriptionStatusLabel(subscription.statut), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                    }
                    Surface(shape = RoundedCornerShape(999.dp), color = accentColor.copy(alpha = 0.12f)) {
                        Text(subscription.planNom, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(color = Color(0xFFE9EEF5))
                SubscriptionDetailRow("Groupe", subscription.groupeNom)
                SubscriptionDetailRow("Identifiant groupe", subscription.groupeId)
                SubscriptionDetailRow("Slug", subscription.groupeSlug.ifBlank { "—" })
                SubscriptionDetailRow("Plan", subscription.planNom)
                SubscriptionDetailRow("Montant", formatMoneyXaf(subscription.prixXAF))
                SubscriptionDetailRow("Début", formatDate(subscription.dateDebut))
                SubscriptionDetailRow("Fin", formatDate(subscription.dateFin))
                SubscriptionDetailRow("Renouvellement", if (subscription.renouvellementAuto) "Automatique" else "Manuel")
                HorizontalDivider(color = Color(0xFFE9EEF5))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SubscriptionBadge(Icons.Default.Groups, "${subscription.maxStudents} élèves", Color(0xFF2563EB))
                    SubscriptionBadge(Icons.Default.Subscriptions, "${subscription.moduleCount} modules", accentColor)
                    SubscriptionBadge(Icons.Default.Autorenew, "${subscription.maxPersonnel} personnel", Color(0xFF6D28D9))
                }
            }
        },
        actions = {
            TextButton(onClick = { onChangeStatus(nextStatus) }, modifier = Modifier.cursorHand()) {
                Text(
                    when (nextStatus) {
                        "suspended" -> "Suspendre"
                        "active" -> "Réactiver"
                        else -> "Activer"
                    },
                    color = if (nextStatus == "suspended") Color(0xFFD97706) else Color(0xFF059669)
                )
            }
            OutlinedButton(onClick = onChangePlan, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Paid, null, modifier = Modifier.size(16.dp))
                Text("Changer de plan", modifier = Modifier.padding(start = 6.dp))
            }
            Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp), modifier = Modifier.cursorHand()) {
                Text("Fermer")
            }
        }
    )
}

@Composable
internal fun SubscriptionFormDialog(
    groupes: List<GroupeDto>,
    plans: List<PlanDto>,
    existing: SubscriptionDto?,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (groupeId: String, planId: String, renouvellementAuto: Boolean) -> Unit
) {
    var groupMenuExpanded by remember { mutableStateOf(false) }
    var planMenuExpanded by remember { mutableStateOf(false) }
    var selectedGroupId by remember { mutableStateOf(existing?.groupeId ?: groupes.firstOrNull()?.id.orEmpty()) }
    var selectedPlanId by remember { mutableStateOf(existing?.planId ?: plans.firstOrNull()?.id.orEmpty()) }
    var renouvellementAuto by remember { mutableStateOf(existing?.renouvellementAuto ?: false) }

    val selectedGroup = groupes.firstOrNull { it.id == selectedGroupId }
    val selectedPlan = plans.firstOrNull { it.id == selectedPlanId }
    val availableGroups = if (existing != null) groupes.filter { it.id == existing.groupeId } else groupes.sortedBy { it.nom.lowercase() }
    val availablePlans = plans.filter { it.isActive }.sortedBy { it.prixXAF }

    AdminDialogWindow(
        title = if (existing == null) "Nouvel abonnement" else "Modifier l'abonnement",
        subtitle = if (existing == null) "Attribuez un plan à un groupe scolaire" else "Changez le plan ou le renouvellement",
        onDismiss = onDismiss,
        size = DpSize(620.dp, 460.dp),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Groupe scolaire", fontSize = 12.sp, color = EpiloteTextMuted)
                    OutlinedButton(onClick = { if (existing == null) groupMenuExpanded = true }, enabled = existing == null, modifier = Modifier.fillMaxWidth().cursorHand(), shape = RoundedCornerShape(12.dp)) {
                        Text(selectedGroup?.nom ?: "Sélectionner un groupe", modifier = Modifier.fillMaxWidth(), maxLines = 1)
                    }
                    DropdownMenu(expanded = groupMenuExpanded, onDismissRequest = { groupMenuExpanded = false }) {
                        availableGroups.forEach { group ->
                            DropdownMenuItem(text = { Text(group.nom) }, onClick = {
                                selectedGroupId = group.id
                                groupMenuExpanded = false
                            })
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Plan d'abonnement", fontSize = 12.sp, color = EpiloteTextMuted)
                    OutlinedButton(onClick = { planMenuExpanded = true }, modifier = Modifier.fillMaxWidth().cursorHand(), shape = RoundedCornerShape(12.dp)) {
                        Text(selectedPlan?.nom ?: "Sélectionner un plan", modifier = Modifier.fillMaxWidth(), maxLines = 1)
                    }
                    DropdownMenu(expanded = planMenuExpanded, onDismissRequest = { planMenuExpanded = false }) {
                        availablePlans.forEach { plan ->
                            DropdownMenuItem(text = { Text("${plan.nom} • ${formatMoneyXaf(plan.prixXAF)}") }, onClick = {
                                selectedPlanId = plan.id
                                planMenuExpanded = false
                            })
                        }
                    }
                }

                selectedPlan?.let { plan ->
                    Surface(shape = RoundedCornerShape(14.dp), color = planColorById(plan.id).copy(alpha = 0.08f)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(plan.nom, fontWeight = FontWeight.Bold, color = Color(0xFF1D3557))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SubscriptionBadge(Icons.Default.Paid, formatMoneyXaf(plan.prixXAF), planColorById(plan.id))
                                SubscriptionBadge(Icons.Default.Groups, "${plan.maxStudents} élèves", Color(0xFF2563EB))
                                SubscriptionBadge(Icons.Default.Autorenew, if (renouvellementAuto) "Auto-renouvelé" else "Renouvellement manuel", Color(0xFF6D28D9))
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { renouvellementAuto = !renouvellementAuto }, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(999.dp)) {
                        Icon(Icons.Default.Autorenew, null, modifier = Modifier.size(16.dp))
                        Text(if (renouvellementAuto) "Renouvellement auto activé" else "Renouvellement manuel", modifier = Modifier.padding(start = 6.dp))
                    }
                }

                errorMessage?.let {
                    AdminFeedbackBanner(feedback = AdminFeedbackMessage(it, isError = true), onDismiss = {})
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting, modifier = Modifier.cursorHand()) { Text("Annuler") }
            Button(
                onClick = { onSubmit(selectedGroupId, selectedPlanId, renouvellementAuto) },
                enabled = !isSubmitting && selectedGroupId.isNotBlank() && selectedPlanId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D3557)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.cursorHand()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Text("Traitement…", modifier = Modifier.padding(start = 8.dp))
                } else {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Text(if (existing == null) "Créer l'abonnement" else "Enregistrer", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    )
}

@Composable
private fun SubscriptionDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = EpiloteTextMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D3557))
    }
}

@Composable
private fun SubscriptionBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}
