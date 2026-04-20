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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
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
import cg.epilote.desktop.data.CreateCategorieDto
import cg.epilote.desktop.data.UpdateCategorieDto
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import java.text.Normalizer

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryDetailDialog(
    category: CategorieDto,
    modules: List<ModuleDto>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val color = categoryColor(category.code)
    val scrollState = rememberScrollState()

    AdminDialogWindow(
        title = category.nom,
        subtitle = "${category.code} — ordre ${category.ordre}",
        onDismiss = onDismiss,
        size = DpSize(760.dp, 560.dp),
        content = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f)) {
                        Text(category.code, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = color, fontWeight = FontWeight.SemiBold)
                    }
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp), color = if (category.isCore) Color(0xFFEDE9FE) else Color(0xFFDBEAFE)) {
                        Text(if (category.isCore) "Catégorie core" else "Catégorie métier", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = if (category.isCore) Color(0xFF6D28D9) else Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                    }
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp), color = if (category.isActive) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)) {
                        Text(if (category.isActive) "Active" else "Inactive", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = if (category.isActive) Color(0xFF166534) else Color(0xFFB91C1C), fontWeight = FontWeight.SemiBold)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Modules rattachés", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("${modules.size} module(s) utilisent actuellement cette catégorie dans la plateforme.", color = EpiloteTextMuted, fontSize = 12.sp)
                }

                if (modules.isEmpty()) {
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC)) {
                        Text("Aucun module n'est encore rattaché à cette catégorie.", modifier = Modifier.fillMaxWidth().padding(16.dp), color = EpiloteTextMuted)
                    }
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        modules.forEach { module ->
                            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp), color = color.copy(alpha = 0.1f)) {
                                Text(module.nom, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = color, fontSize = 12.sp)
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
            OutlinedButton(onClick = onEdit, modifier = Modifier.padding(start = 8.dp).cursorHand()) {
                Text("Modifier")
            }
            FilledTonalButton(onClick = onToggleStatus, modifier = Modifier.padding(start = 8.dp).cursorHand()) {
                Text(if (category.isActive) "Suspendre" else "Réactiver")
            }
        }
    )
}

@Composable
internal fun CategoryFormDialog(
    category: CategorieDto?,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (CreateCategorieDto) -> Unit,
    onUpdate: (String, UpdateCategorieDto) -> Unit
) {
    val isEdit = category != null
    val currentCategory = category
    val scrollState = rememberScrollState()
    var nom by remember(category) { mutableStateOf(category?.nom ?: "") }
    var code by remember(category) { mutableStateOf(category?.code ?: "") }
    var ordreValue by remember(category) { mutableStateOf(category?.ordre?.toString() ?: "0") }
    var isCore by remember(category) { mutableStateOf(category?.isCore ?: false) }
    var codeTouched by remember(category) { mutableStateOf(isEdit) }
    val normalizedCode = remember(code) { normalizeCategoryCode(code) }
    val ordre = ordreValue.toIntOrNull()
    val canSubmit = nom.isNotBlank() && ordre != null && (isEdit || normalizedCode.isNotBlank())

    AdminDialogWindow(
        title = if (isEdit) "Modifier la catégorie" else "Nouvelle catégorie",
        subtitle = if (isEdit) "Mettez à jour les métadonnées de la catégorie" else "Ajoutez une nouvelle catégorie à la plateforme",
        onDismiss = onDismiss,
        size = DpSize(680.dp, 520.dp),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = {
                        nom = it
                        if (!isEdit && !codeTouched) {
                            code = normalizeCategoryCode(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nom") },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = if (isEdit) currentCategory?.code.orEmpty() else code,
                    onValueChange = {
                        if (!isEdit) {
                            codeTouched = true
                            code = normalizeCategoryCode(it)
                        }
                    },
                    enabled = !isEdit,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Code technique") },
                    supportingText = { Text("Format conseillé : lettres minuscules et tirets") },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = ordreValue,
                    onValueChange = { value -> ordreValue = value.filter { it.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ordre d'affichage") },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = isCore, onCheckedChange = { isCore = it })
                    Column {
                        Text("Catégorie core", fontWeight = FontWeight.SemiBold)
                        Text("Les catégories core représentent les espaces système prioritaires.", fontSize = 12.sp, color = EpiloteTextMuted)
                    }
                }
                errorMessage?.let {
                    AdminFeedbackBanner(feedback = AdminFeedbackMessage(it, isError = true), onDismiss = {})
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting, modifier = Modifier.cursorHand()) {
                Text("Annuler")
            }
            FilledTonalButton(
                onClick = {
                    if (!canSubmit || ordre == null) return@FilledTonalButton
                    if (isEdit) {
                        onUpdate(currentCategory?.code.orEmpty(), UpdateCategorieDto(nom = nom.trim(), isCore = isCore, ordre = ordre))
                    } else {
                        onCreate(CreateCategorieDto(code = normalizedCode, nom = nom.trim(), isCore = isCore, ordre = ordre))
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

private fun normalizeCategoryCode(value: String): String {
    val normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
    return normalized
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
}
