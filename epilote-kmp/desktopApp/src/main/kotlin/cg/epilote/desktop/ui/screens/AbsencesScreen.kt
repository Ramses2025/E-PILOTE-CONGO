package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import cg.epilote.shared.domain.model.Absence
import cg.epilote.shared.domain.model.UserSession
import cg.epilote.shared.presentation.viewmodel.AbsenceUiState
import cg.epilote.shared.presentation.viewmodel.AbsencesViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AbsencesScreen(
    session: UserSession,
    viewModel: AbsencesViewModel
) {
    val absences by viewModel.absences.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val today = remember { LocalDate.now().format(DateTimeFormatter.ISO_DATE) }
    var selectedDate by remember { mutableStateOf(today) }
    var justifyDialog by remember { mutableStateOf<Absence?>(null) }

    LaunchedEffect(selectedDate) {
        session.ecoleId?.let { viewModel.loadByDate(it, selectedDate) }
    }

    LaunchedEffect(uiState) {
        if (uiState is AbsenceUiState.Saved || uiState is AbsenceUiState.Justified) {
            session.ecoleId?.let { viewModel.loadByDate(it, selectedDate) }
            viewModel.resetState()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Présences & Absences", fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CalendarToday, null, tint = EpiloteTextMuted, modifier = Modifier.size(16.dp))
                Text(selectedDate, fontSize = 13.sp, color = EpiloteTextMuted)
            }
        }
        Divider()

        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (absences.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null,
                                    tint = EpiloteGreen, modifier = Modifier.size(48.dp))
                                Text("Aucune absence enregistrée pour cette date",
                                    color = EpiloteTextMuted, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    items(absences) { absence ->
                        AbsenceCard(
                            absence = absence,
                            onJustify = { justifyDialog = absence }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .background(EpiloteSurface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Résumé du jour", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                val justified = absences.count { it.justifiee }
                val notJustified = absences.count { !it.justifiee }

                StatRow("Total absences", absences.size.toString(), Color(0xFF172B4D))
                StatRow("Justifiées", justified.toString(), EpiloteGreen)
                StatRow("Non justifiées", notJustified.toString(), EpiloteRed)

                if (uiState is AbsenceUiState.Error) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEDEB),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            (uiState as AbsenceUiState.Error).message,
                            modifier = Modifier.padding(12.dp),
                            color = EpiloteRed,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    justifyDialog?.let { absence ->
        JustificationDialog(
            absence = absence,
            onConfirm = { motif ->
                session.ecoleId?.let {
                    viewModel.justifyAbsence(it, absence.eleveId, absence.date, motif)
                }
                justifyDialog = null
            },
            onDismiss = { justifyDialog = null }
        )
    }
}

@Composable
private fun AbsenceCard(absence: Absence, onJustify: () -> Unit) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (absence.justifiee) EpiloteGreenLight else Color(0xFFFFEDEB),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (absence.justifiee) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        null,
                        tint = if (absence.justifiee) EpiloteGreen else EpiloteRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(absence.eleveName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(
                        if (absence.justifiee) "Justifiée${absence.motif?.let { " : $it" } ?: ""}"
                        else "Non justifiée",
                        fontSize = 12.sp,
                        color = if (absence.justifiee) EpiloteGreen else EpiloteTextMuted
                    )
                }
            }

            if (!absence.justifiee) {
                OutlinedButton(
                    onClick = onJustify,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Justifier", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = EpiloteTextMuted)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun JustificationDialog(
    absence: Absence,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var motif by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).width(360.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Justifier l'absence", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Élève : ${absence.eleveName}", fontSize = 14.sp, color = EpiloteTextMuted)

                OutlinedTextField(
                    value = motif,
                    onValueChange = { motif = it },
                    label = { Text("Motif de l'absence") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = { if (motif.isNotBlank()) onConfirm(motif) },
                        enabled = motif.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EpiloteGreen)
                    ) {
                        Text("Valider")
                    }
                }
            }
        }
    }
}
