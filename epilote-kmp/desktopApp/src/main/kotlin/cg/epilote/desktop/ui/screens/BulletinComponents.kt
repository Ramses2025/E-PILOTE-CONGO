package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cg.epilote.desktop.ui.theme.*

data class AppreciationResult(
    val eleveNom: String,
    val appreciation: String,
    val mention: String,
    val conseil: String = "",
    val fallback: Boolean = false
)

fun mentionLabel(moy: Double) = when {
    moy >= 16 -> "Excellent"
    moy >= 14 -> "Très Bien"
    moy >= 12 -> "Bien"
    moy >= 10 -> "Assez Bien"
    moy >= 8  -> "Passable"
    else      -> "Insuffisant"
}

fun mentionColor(moy: Double) = when {
    moy >= 16 -> Color(0xFF00875A)
    moy >= 14 -> Color(0xFF0052CC)
    moy >= 12 -> Color(0xFF6554C0)
    moy >= 10 -> Color(0xFFFF8B00)
    moy >= 8  -> Color(0xFFFF8B00)
    else      -> Color(0xFFDE350B)
}

// ── Dialog résultat appréciation IA ──────────────────────────────────────────

@Composable
fun AppreciationDialog(result: AppreciationResult, onDismiss: () -> Unit) {
    val mentionCol = mentionColor(
        when (result.mention) {
            "Excellent"  -> 16.0; "Très Bien" -> 14.0; "Bien"       -> 12.0
            "Assez Bien" -> 10.0; "Passable"  -> 8.0  ; else         -> 5.0
        }
    )
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp).width(460.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).background(EpiloteGreenLight, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = EpiloteGreen, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Appréciation générée par IA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(result.eleveNom, fontSize = 13.sp, color = EpiloteTextMuted)
                    }
                    if (result.fallback) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFFF0D1)) {
                            Text("Template", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp, color = EpiloteOrange)
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = mentionCol.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(6.dp), color = mentionCol.copy(alpha = 0.15f)) {
                            Text(
                                result.mention,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = mentionCol
                            )
                        }
                        Text(result.appreciation, fontSize = 14.sp, lineHeight = 20.sp,
                            modifier = Modifier.weight(1f))
                    }
                }

                if (result.conseil.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF0F0F0),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Lightbulb, null,
                                tint = EpiloteOrange, modifier = Modifier.size(14.dp))
                            Text(result.conseil, fontSize = 12.sp, color = Color(0xFF6B6B6B))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EpiloteGreen)
                    ) { Text("Fermer") }
                }
            }
        }
    }
}

// ── Dropdown sélection classe ─────────────────────────────────────────────────

@Composable
fun BulletinClasseDropdown(
    classes: List<cg.epilote.shared.domain.model.Classe>,
    selected: cg.epilote.shared.domain.model.Classe?,
    onSelected: (cg.epilote.shared.domain.model.Classe) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(selected?.nom ?: "Sélectionner une classe", fontSize = 13.sp)
            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            classes.forEach { classe ->
                DropdownMenuItem(
                    text = { Text(classe.nom, fontSize = 13.sp) },
                    onClick = { onSelected(classe); expanded = false }
                )
            }
        }
    }
}
