package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.*
import cg.epilote.shared.domain.model.Classe
import cg.epilote.shared.domain.model.Matiere
import cg.epilote.shared.domain.model.Note
import cg.epilote.shared.domain.model.UserSession
import cg.epilote.shared.presentation.viewmodel.ClassesViewModel
import cg.epilote.shared.presentation.viewmodel.NotesViewModel

@Composable
fun NotesScreen(
    session: UserSession,
    classesViewModel: ClassesViewModel,
    notesViewModel: NotesViewModel
) {
    val classes by classesViewModel.classes.collectAsState()
    val selectedClasse by classesViewModel.selectedClasse.collectAsState()
    val matieres by classesViewModel.matieres.collectAsState()
    val notes by notesViewModel.notes.collectAsState()

    var selectedMatiere by remember { mutableStateOf<Matiere?>(null) }
    var selectedPeriode by remember { mutableStateOf("T1") }
    val periodes = listOf("T1", "T2", "T3")

    LaunchedEffect(session.schoolId) {
        session.schoolId?.let { classesViewModel.loadClasses(it) }
    }

    LaunchedEffect(selectedClasse, selectedMatiere, selectedPeriode) {
        val ecole = session.schoolId ?: return@LaunchedEffect
        val classe = selectedClasse ?: return@LaunchedEffect
        val matiere = selectedMatiere ?: return@LaunchedEffect
        notesViewModel.loadNotes(ecole, classe.id, selectedPeriode)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Notes", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))

            ClasseDropdown(
                classes = classes,
                selected = selectedClasse,
                onSelected = { classe ->
                    session.schoolId?.let { classesViewModel.selectClasse(classe, it) }
                    selectedMatiere = null
                }
            )

            MatiereDropdown(
                matieres = matieres,
                selected = selectedMatiere,
                onSelected = { selectedMatiere = it }
            )

            periodes.forEach { p ->
                FilterChip(
                    selected = selectedPeriode == p,
                    onClick = { selectedPeriode = p },
                    label = { Text(p, fontSize = 12.sp) }
                )
            }
        }

        Divider()

        if (selectedClasse == null) {
            EmptyState("Sélectionnez une classe pour afficher les notes", Icons.Default.Class)
        } else if (selectedMatiere == null) {
            EmptyState("Sélectionnez une matière", Icons.Default.Book)
        } else {
            NotesGrid(
                notes = notes,
                matiere = selectedMatiere!!,
                periode = selectedPeriode,
                schoolId = session.schoolId ?: "",
                classeId = selectedClasse!!.id,
                enseignantId = session.userId,
                onSave = { eleveId, valeur ->
                    notesViewModel.saveNote(
                        schoolId = session.schoolId ?: "",
                        anneeId      = selectedClasse!!.anneeId,
                        classeId     = selectedClasse!!.id,
                        matiereId    = selectedMatiere!!.id,
                        eleveId      = eleveId,
                        periode      = selectedPeriode,
                        valeur       = valeur,
                        enseignantId = session.userId
                    )
                }
            )
        }
    }
}

@Composable
private fun NotesGrid(
    notes: List<Note>,
    matiere: Matiere,
    periode: String,
    schoolId: String,
    classeId: String,
    enseignantId: String,
    onSave: (eleveId: String, valeur: Double) -> Unit
) {
    val noteMap = notes.associateBy { it.eleveId }
    val editValues = remember(notes) {
        mutableStateMapOf<String, String>().apply {
            notes.forEach { put(it.eleveId, it.valeur.toString()) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EpiloteSurface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Élève", modifier = Modifier.weight(2f),
                fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = EpiloteTextMuted)
            Text("Note /${matiere.coefficient * 20}",
                modifier = Modifier.width(100.dp),
                fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = EpiloteTextMuted,
                textAlign = TextAlign.Center)
            Text("Statut", modifier = Modifier.width(80.dp),
                fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = EpiloteTextMuted)
        }
        Divider()

        LazyColumn {
            itemsIndexed(notes) { index, note ->
                val currentValue = editValues[note.eleveId] ?: note.valeur.toString()
                val isEdited = currentValue != note.valeur.toString()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (index % 2 == 0) Color.White else EpiloteSurface)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(2f)) {
                        Text(note.eleveName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    NoteInputCell(
                        value = currentValue,
                        locked = note.locked,
                        onValueChange = { editValues[note.eleveId] = it },
                        onCommit = {
                            val v = it.toDoubleOrNull()
                            if (v != null && v in 0.0..20.0) onSave(note.eleveId, v)
                        }
                    )

                    Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) {
                        if (note.locked) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = EpiloteGreenLight
                            ) {
                                Text("Validé", fontSize = 11.sp, color = EpiloteGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        } else if (note.requiresReview) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFFEDEB)
                            ) {
                                Text("Conflit", fontSize = 11.sp, color = EpiloteRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        } else if (isEdited) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFFF0D1)
                            ) {
                                Text("Modifié", fontSize = 11.sp, color = EpiloteOrange,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
                Divider(color = Color(0xFFF0F0F0))
            }
        }
    }
}

@Composable
private fun NoteInputCell(
    value: String,
    locked: Boolean,
    onValueChange: (String) -> Unit,
    onCommit: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(32.dp)
            .border(
                1.dp,
                if (locked) Color.Transparent else Color(0xFFDFE1E6),
                RoundedCornerShape(4.dp)
            )
            .background(
                if (locked) EpiloteSurface else Color.White,
                RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (locked) {
            Text(value, fontSize = 13.sp, textAlign = TextAlign.Center, color = EpiloteTextMuted)
        } else {
            BasicTextField(
                value = value,
                onValueChange = { v ->
                    val filtered = v.filter { it.isDigit() || it == '.' }
                    onValueChange(filtered)
                    val num = filtered.toDoubleOrNull()
                    if (num != null && num in 0.0..20.0) onCommit(filtered)
                },
                textStyle = TextStyle(fontSize = 13.sp, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                singleLine = true
            )
        }
    }
}

@Composable
private fun ClasseDropdown(
    classes: List<Classe>,
    selected: Classe?,
    onSelected: (Classe) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(selected?.nom ?: "Classe", fontSize = 13.sp)
            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            classes.forEach { c ->
                DropdownMenuItem(
                    text = { Text("${c.nom} — ${c.niveauNom}", fontSize = 13.sp) },
                    onClick = { onSelected(c); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun MatiereDropdown(
    matieres: List<Matiere>,
    selected: Matiere?,
    onSelected: (Matiere) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(selected?.nom ?: "Matière", fontSize = 13.sp)
            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            matieres.forEach { m ->
                DropdownMenuItem(
                    text = { Text("${m.nom} (coeff. ${m.coefficient})", fontSize = 13.sp) },
                    onClick = { onSelected(m); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = Color(0xFFDFE1E6), modifier = Modifier.size(48.dp))
            Text(message, color = EpiloteTextMuted, fontSize = 14.sp)
        }
    }
}
