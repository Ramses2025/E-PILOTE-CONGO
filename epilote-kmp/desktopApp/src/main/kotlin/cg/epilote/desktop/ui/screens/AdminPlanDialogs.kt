package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PlanDetailDialog(
    plan: PlanDto,
    modules: List<ModuleDto>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val color = planColorById(plan.id)
    val scrollState = rememberScrollState()

    AdminDialogWindow(
        title = plan.nom,
        subtitle = "Plan ${plan.type.replaceFirstChar { it.uppercase() }} • ${plan.prixXAF} ${plan.currency}",
        onDismiss = onDismiss,
        size = DpSize(640.dp, 520.dp),
        content = {
            Column(modifier = Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(999.dp), color = if (plan.isActive) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)) {
                        Text(if (plan.isActive) "ACTIF" else "INACTIF", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 11.sp, color = if (plan.isActive) Color(0xFF166534) else Color(0xFFB91C1C), fontWeight = FontWeight.SemiBold)
                    }
                    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.1f)) {
                        Text("${plan.modulesIncluded.size} modules inclus", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider(color = Color(0xFFE9EEF5))
                DetailRow("Identifiant", plan.id)
                DetailRow("Type", plan.type.replaceFirstChar { it.uppercase() })
                DetailRow("Prix", "${plan.prixXAF} ${plan.currency}")
                DetailRow("Élèves max", "${plan.maxStudents}")
                DetailRow("Personnel max", "${plan.maxPersonnel}")
                HorizontalDivider(color = Color(0xFFE9EEF5))
                Text("Modules inclus", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF1D3557))
                if (plan.modulesIncluded.isEmpty()) {
                    Text("Aucun module inclus", fontSize = 12.sp, color = EpiloteTextMuted)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        plan.modulesIncluded.forEach { slug ->
                            val moduleName = modules.firstOrNull { it.code == slug }?.nom ?: slug
                            Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.08f)) {
                                Text(moduleName, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        actions = {
            TextButton(onClick = onToggleStatus, modifier = Modifier.cursorHand()) {
                Text(if (plan.isActive) "Suspendre" else "Réactiver", color = if (plan.isActive) Color(0xFFB91C1C) else Color(0xFF059669))
            }
            OutlinedButton(onClick = onEdit, modifier = Modifier.cursorHand(), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                Text("Modifier", modifier = Modifier.padding(start = 6.dp))
            }
            Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp), modifier = Modifier.cursorHand()) {
                Text("Fermer")
            }
        }
    )
}

@Composable
internal fun PlanFormDialog(
    existing: PlanDto?,
    modules: List<ModuleDto>,
    isSubmitting: Boolean,
    errorMessage: String?,
    onSubmit: (nom: String, type: String, prixXAF: Long, maxStudents: Int, maxPersonnel: Int, modulesIncluded: List<String>, isActive: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var nom by remember { mutableStateOf(existing?.nom ?: "") }
    var type by remember { mutableStateOf(existing?.type ?: "") }
    var prixXAF by remember { mutableStateOf(existing?.prixXAF?.toString() ?: "0") }
    var maxStudents by remember { mutableStateOf(existing?.maxStudents?.toString() ?: "100") }
    var maxPersonnel by remember { mutableStateOf(existing?.maxPersonnel?.toString() ?: "10") }
    var selectedModules by remember { mutableStateOf(existing?.modulesIncluded?.toSet() ?: emptySet()) }
    var isActive by remember { mutableStateOf(existing?.isActive ?: true) }
    val scrollState = rememberScrollState()
    val isEdit = existing != null

    AdminDialogWindow(
        title = if (isEdit) "Modifier le plan" else "Nouveau plan",
        subtitle = if (isEdit) "Modifiez les paramètres du plan" else "Créez un nouveau plan d'abonnement",
        onDismiss = onDismiss,
        size = DpSize(640.dp, 580.dp),
        content = {
            Column(modifier = Modifier.verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(value = nom, onValueChange = { nom = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Nom du plan *") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                OutlinedTextField(value = type, onValueChange = { type = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Type (identifiant) *") }, singleLine = true, shape = RoundedCornerShape(12.dp), enabled = !isEdit)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = prixXAF, onValueChange = { prixXAF = it.filter { c -> c.isDigit() } }, modifier = Modifier.weight(1f), label = { Text("Prix (XAF)") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = maxStudents, onValueChange = { maxStudents = it.filter { c -> c.isDigit() } }, modifier = Modifier.weight(1f), label = { Text("Élèves max") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = maxPersonnel, onValueChange = { maxPersonnel = it.filter { c -> c.isDigit() } }, modifier = Modifier.weight(1f), label = { Text("Personnel max") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                }
                HorizontalDivider(color = Color(0xFFE9EEF5))
                Text("Modules inclus", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF1D3557))
                if (modules.isEmpty()) {
                    Text("Aucun module disponible", fontSize = 12.sp, color = EpiloteTextMuted)
                } else {
                    ModuleChipGrid(modules = modules, selected = selectedModules, onToggle = { slug ->
                        selectedModules = if (slug in selectedModules) selectedModules - slug else selectedModules + slug
                    })
                }
                errorMessage?.let {
                    AdminFeedbackBanner(feedback = AdminFeedbackMessage(it, isError = true), onDismiss = {})
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting, modifier = Modifier.cursorHand()) { Text("Annuler") }
            Row(modifier = Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        val price = prixXAF.toLongOrNull() ?: 0L
                        val students = maxStudents.toIntOrNull() ?: 100
                        val personnel = maxPersonnel.toIntOrNull() ?: 10
                        onSubmit(nom.trim(), type.trim().lowercase(), price, students, personnel, selectedModules.toList(), isActive)
                    },
                    enabled = !isSubmitting && nom.isNotBlank() && type.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D3557)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Row(modifier = Modifier.padding(start = 8.dp)) { Text("Traitement…") }
                    } else {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                        Text(if (isEdit) "Enregistrer" else "Créer le plan", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModuleChipGrid(modules: List<ModuleDto>, selected: Set<String>, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        modules.sortedBy { it.ordre }.forEach { module ->
            val isSelected = module.code in selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(module.code) },
                label = { Text(module.nom, fontSize = 11.sp, maxLines = 1) },
                modifier = Modifier.cursorHand(),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF1D3557).copy(alpha = 0.12f),
                    selectedLabelColor = Color(0xFF1D3557)
                )
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = EpiloteTextMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D3557))
    }
}
