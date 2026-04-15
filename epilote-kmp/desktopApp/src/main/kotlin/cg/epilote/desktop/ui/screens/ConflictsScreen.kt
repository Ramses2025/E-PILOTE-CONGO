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
import cg.epilote.desktop.ui.theme.*
import cg.epilote.shared.data.local.NoteRepository
import cg.epilote.shared.domain.model.Note
import cg.epilote.shared.domain.model.UserSession
import cg.epilote.shared.domain.usecase.notes.ResolveConflictUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ConflictsScreen(
    session: UserSession,
    noteRepo: NoteRepository,
    resolveConflictUseCase: ResolveConflictUseCase
) {
    var conflicts by remember { mutableStateOf(listOf<Note>()) }
    var isLoading by remember { mutableStateOf(true) }
    var resolvedCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(session.schoolId) {
        val ecole = session.schoolId ?: return@LaunchedEffect
        isLoading = true
        conflicts = noteRepo.getPendingReview(ecole)
        isLoading = false
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
            Column {
                Text("Conflits de synchronisation", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Notes modifiées simultanément sur plusieurs appareils",
                    fontSize = 13.sp,
                    color = EpiloteTextMuted
                )
            }
            if (conflicts.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFEDEB)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Warning, null, tint = EpiloteRed, modifier = Modifier.size(14.dp))
                        Text(
                            "${conflicts.size} conflit(s) en attente",
                            fontSize = 13.sp,
                            color = EpiloteRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        Divider()

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        } else if (conflicts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null,
                        tint = EpiloteGreen, modifier = Modifier.size(52.dp))
                    Text(
                        "Aucun conflit détecté",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EpiloteGreenDark
                    )
                    Text(
                        "Toutes vos données sont cohérentes entre les appareils",
                        fontSize = 13.sp,
                        color = EpiloteTextMuted
                    )
                    if (resolvedCount > 0) {
                        Surface(shape = RoundedCornerShape(8.dp), color = EpiloteGreenLight) {
                            Text(
                                "$resolvedCount conflit(s) résolu(s) dans cette session",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                color = EpiloteGreen
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF0D1),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Info, null, tint = EpiloteOrange, modifier = Modifier.size(18.dp))
                            Text(
                                "Ces notes ont été modifiées simultanément sur plusieurs appareils. " +
                                "Choisissez quelle valeur conserver pour chaque conflit.",
                                fontSize = 13.sp,
                                color = Color(0xFF856404)
                            )
                        }
                    }
                }

                items(conflicts) { note ->
                    ConflictCard(
                        note = note,
                        onKeepLocal = {
                            scope.launch(Dispatchers.IO) {
                                resolveConflictUseCase.resolveKeepLocal(note)
                                conflicts = conflicts.filter { it.id != note.id }
                                resolvedCount++
                            }
                        },
                        onKeepRemote = { newValue ->
                            scope.launch(Dispatchers.IO) {
                                resolveConflictUseCase.resolveWithValue(note, newValue)
                                conflicts = conflicts.filter { it.id != note.id }
                                resolvedCount++
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConflictCard(
    note: Note,
    onKeepLocal: () -> Unit,
    onKeepRemote: (Double) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFFEDEB), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Warning, null, tint = EpiloteRed, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(note.eleveName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "Matière : ${note.matiereId} · Période : ${note.periode}",
                        fontSize = 12.sp,
                        color = EpiloteTextMuted
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConflictValueCard(
                    label = "Valeur locale",
                    value = note.valeur,
                    subLabel = "Saisie sur cet appareil",
                    color = Color(0xFF0052CC),
                    modifier = Modifier.weight(1f),
                    onKeep = onKeepLocal
                )
                ConflictValueCard(
                    label = "Valeur distante",
                    value = note.valeur,
                    subLabel = "Saisie sur un autre appareil",
                    color = EpiloteOrange,
                    modifier = Modifier.weight(1f),
                    onKeep = { onKeepRemote(note.valeur) }
                )
            }
        }
    }
}

@Composable
private fun ConflictValueCard(
    label: String,
    value: Double,
    subLabel: String,
    color: Color,
    modifier: Modifier = Modifier,
    onKeep: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.06f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
            Text(
                String.format("%.2f", value) + "/20",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(subLabel, fontSize = 11.sp, color = EpiloteTextMuted)
            Button(
                onClick = onKeep,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Conserver cette valeur", fontSize = 12.sp)
            }
        }
    }
}
