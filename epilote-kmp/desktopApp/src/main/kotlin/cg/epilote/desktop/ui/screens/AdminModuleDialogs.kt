package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
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
import cg.epilote.desktop.data.CreateModuleDto
import cg.epilote.desktop.data.UpdateModuleDto
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import java.text.Normalizer

@Composable
internal fun ModuleDetailDialog(
    module: ModuleDto,
    categoryLabel: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val accent = categoryColor(module.categorieCode)
    val scrollState = rememberScrollState()

    AdminDialogWindow(
        title = module.nom,
        subtitle = "${module.code} — ${categoryLabel}",
        onDismiss = onDismiss,
        size = DpSize(760.dp, 560.dp),
        content = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp), color = accent.copy(alpha = 0.12f)) {
                        Text(module.code, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = accent, fontWeight = FontWeight.SemiBold)
                    }
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp), color = modulePlanAccent(module.requiredPlan).copy(alpha = 0.1f)) {
                        Text(modulePlanLabel(module.requiredPlan), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = modulePlanAccent(module.requiredPlan), fontWeight = FontWeight.SemiBold)
                    }
                    if (module.isCore) {
                        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp), color = Color(0xFFEDE9FE)) {
                            Text("Module core", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color(0xFF6D28D9), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp), color = if (module.isActive) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)) {
                        Text(if (module.isActive) "Actif" else "Inactif", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = if (module.isActive) Color(0xFF166534) else Color(0xFFB91C1C), fontWeight = FontWeight.SemiBold)
                    }
                }

                DetailLine("Catégorie", categoryLabel)
                DetailLine("Ordre d'affichage", module.ordre.toString())
                DetailLine("Plan requis", modulePlanLabel(module.requiredPlan))
                DetailLine("Description", module.description.ifBlank { "Aucune description renseignée" })
            }
        },
        actions = {
            TextButton(onClick = onDismiss, modifier = Modifier.cursorHand()) { Text("Fermer") }
            OutlinedButton(onClick = onEdit, modifier = Modifier.padding(start = 8.dp).cursorHand()) { Text("Modifier") }
            FilledTonalButton(onClick = onToggleStatus, modifier = Modifier.padding(start = 8.dp).cursorHand()) {
                Text(if (module.isActive) "Suspendre" else "Réactiver")
            }
        }
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = EpiloteTextMuted, fontWeight = FontWeight.Medium)
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC)) {
            Text(value, modifier = Modifier.fillMaxWidth().padding(14.dp), fontSize = 13.sp, color = Color(0xFF334155))
        }
    }
}

@Composable
internal fun ModuleFormDialog(
    module: ModuleDto?,
    categories: List<CategorieDto>,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (CreateModuleDto) -> Unit,
    onUpdate: (String, UpdateModuleDto) -> Unit
) {
    val isEdit = module != null
    val currentModule = module
    val scrollState = rememberScrollState()
    var nom by remember(module) { mutableStateOf(module?.nom ?: "") }
    var code by remember(module) { mutableStateOf(module?.code ?: "") }
    var codeTouched by remember(module) { mutableStateOf(isEdit) }
    var categorieCode by remember(module) { mutableStateOf(module?.categorieCode ?: categories.firstOrNull()?.code.orEmpty()) }
    var requiredPlan by remember(module) { mutableStateOf(module?.requiredPlan ?: "gratuit") }
    var ordreValue by remember(module) { mutableStateOf(module?.ordre?.toString() ?: "0") }
    var description by remember(module) { mutableStateOf(module?.description ?: "") }
    var isCore by remember(module) { mutableStateOf(module?.isCore ?: false) }
    val normalizedCode = remember(code) { normalizeModuleCode(code) }
    val ordre = ordreValue.toIntOrNull()
    val canSubmit = nom.isNotBlank() && categorieCode.isNotBlank() && ordre != null && (isEdit || normalizedCode.isNotBlank())

    AdminDialogWindow(
        title = if (isEdit) "Modifier le module" else "Nouveau module",
        subtitle = if (isEdit) "Mettez à jour le module et sa catégorie" else "Ajoutez un nouveau module à une catégorie existante",
        onDismiss = onDismiss,
        size = DpSize(760.dp, 620.dp),
        content = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = {
                        nom = it
                        if (!isEdit && !codeTouched) {
                            code = normalizeModuleCode(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nom du module") },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = if (isEdit) currentModule?.code.orEmpty() else code,
                    onValueChange = {
                        if (!isEdit) {
                            codeTouched = true
                            code = normalizeModuleCode(it)
                        }
                    },
                    enabled = !isEdit,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Code technique") },
                    supportingText = { Text("Le code est stocké comme slug du module dans Couchbase") },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                SelectionField(
                    label = "Catégorie",
                    value = categories.firstOrNull { it.code == categorieCode }?.nom ?: "Sélectionner une catégorie",
                    options = categories.map { it.code to it.nom },
                    onSelect = { categorieCode = it }
                )
                SelectionField(
                    label = "Plan requis",
                    value = modulePlanOptions.firstOrNull { it.first == requiredPlan }?.second ?: "Sélectionner un plan",
                    options = modulePlanOptions.drop(1),
                    onSelect = { requiredPlan = it }
                )
                OutlinedTextField(
                    value = ordreValue,
                    onValueChange = { value -> ordreValue = value.filter { it.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ordre d'affichage") },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description") },
                    minLines = 3,
                    maxLines = 5,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = isCore, onCheckedChange = { isCore = it })
                    Column {
                        Text("Module core", fontWeight = FontWeight.SemiBold)
                        Text("Les modules core correspondent au socle fonctionnel prioritaire.", fontSize = 12.sp, color = EpiloteTextMuted)
                    }
                }
                errorMessage?.let {
                    AdminFeedbackBanner(feedback = AdminFeedbackMessage(it, isError = true), onDismiss = {})
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting, modifier = Modifier.cursorHand()) { Text("Annuler") }
            FilledTonalButton(
                onClick = {
                    if (!canSubmit || ordre == null) return@FilledTonalButton
                    if (isEdit) {
                        onUpdate(
                            currentModule?.code.orEmpty(),
                            UpdateModuleDto(
                                nom = nom.trim(),
                                categorieCode = categorieCode,
                                description = description.trim(),
                                requiredPlan = requiredPlan,
                                isCore = isCore,
                                ordre = ordre
                            )
                        )
                    } else {
                        onCreate(
                            CreateModuleDto(
                                code = normalizedCode,
                                nom = nom.trim(),
                                categorieCode = categorieCode,
                                description = description.trim(),
                                requiredPlan = requiredPlan,
                                isCore = isCore,
                                ordre = ordre
                            )
                        )
                    }
                },
                enabled = canSubmit && !isSubmitting,
                modifier = Modifier.padding(start = 8.dp).cursorHand()
            ) {
                Text(if (isSubmitting) "Traitement…" else if (isEdit) "Enregistrer" else "Créer")
            }
        }
    )
}

@Composable
private fun SelectionField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 12.sp, color = EpiloteTextMuted, fontWeight = FontWeight.Medium)
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = Color.White, tonalElevation = 0.dp, shadowElevation = 0.dp) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().cursorHand(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(value, color = Color(0xFF0F172A))
                    androidx.compose.material3.Icon(Icons.Default.ArrowDropDown, null)
                }
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = { onSelect(key); expanded = false }
                )
            }
        }
    }
}

private fun normalizeModuleCode(value: String): String {
    val normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
    return normalized
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
}
