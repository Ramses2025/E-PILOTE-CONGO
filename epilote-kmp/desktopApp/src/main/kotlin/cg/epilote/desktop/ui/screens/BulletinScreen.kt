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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.*
import cg.epilote.shared.domain.model.BulletinEleve
import cg.epilote.shared.domain.model.UserSession
import cg.epilote.shared.presentation.viewmodel.BulletinUiState
import cg.epilote.shared.presentation.viewmodel.BulletinViewModel
import cg.epilote.shared.presentation.viewmodel.ClassesViewModel

@Composable
fun BulletinScreen(
    session: UserSession,
    classesViewModel: ClassesViewModel,
    bulletinViewModel: BulletinViewModel,
    onRequestAppreciation: (BulletinEleve) -> Unit,
    appreciationResult: AppreciationResult? = null,
    onDismissAppreciation: () -> Unit = {}
) {
    val classes by classesViewModel.classes.collectAsState()
    val selectedClasse by classesViewModel.selectedClasse.collectAsState()
    val uiState by bulletinViewModel.uiState.collectAsState()
    var selectedPeriode by remember { mutableStateOf("T1") }
    val periodes = listOf("T1", "T2", "T3")

    LaunchedEffect(session.schoolId) {
        session.schoolId?.let { classesViewModel.loadClasses(it) }
    }

    LaunchedEffect(selectedClasse, selectedPeriode) {
        val ecole = session.schoolId ?: return@LaunchedEffect
        val classe = selectedClasse ?: return@LaunchedEffect
        bulletinViewModel.loadBulletins(ecole, classe.id, selectedPeriode)
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
            Text("Bulletins scolaires", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f))

            BulletinClasseDropdown(
                classes = classes,
                selected = selectedClasse,
                onSelected = { classe ->
                    session.schoolId?.let { classesViewModel.selectClasse(classe, it) }
                }
            )

            periodes.forEach { p ->
                FilterChip(
                    selected = selectedPeriode == p,
                    onClick = { selectedPeriode = p },
                    label = { Text(p, fontSize = 12.sp) }
                )
            }

            if (uiState is BulletinUiState.Ready) {
                Button(
                    onClick = {
                        session.schoolId?.let { ecole ->
                            selectedClasse?.let { classe ->
                                bulletinViewModel.lockBulletin(ecole, classe.id, selectedPeriode)
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EpiloteOrange)
                ) {
                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Valider les bulletins", fontSize = 13.sp)
                }
            }
        }
        Divider()

        when (val state = uiState) {
            is BulletinUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EpiloteGreen)
                }
            }

            is BulletinUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Error, null, tint = EpiloteRed, modifier = Modifier.size(36.dp))
                        Text(state.message, color = EpiloteRed, fontSize = 13.sp)
                    }
                }
            }

            is BulletinUiState.Ready,
            is BulletinUiState.Locked -> {
                val bulletins = if (state is BulletinUiState.Ready) state.bulletins
                                else (state as BulletinUiState.Locked).bulletins
                val isLocked = state is BulletinUiState.Locked

                if (selectedClasse == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sélectionnez une classe", color = EpiloteTextMuted, fontSize = 14.sp)
                    }
                } else {
                    BulletinTable(
                        bulletins = bulletins,
                        isLocked = isLocked,
                        onRequestAppreciation = onRequestAppreciation
                    )
                }
            }
        }

        if (appreciationResult != null) {
            AppreciationDialog(result = appreciationResult, onDismiss = onDismissAppreciation)
        }
    }
}

@Composable
private fun BulletinTable(
    bulletins: List<BulletinEleve>,
    isLocked: Boolean,
    onRequestAppreciation: (BulletinEleve) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (isLocked) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = EpiloteGreenLight
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, null, tint = EpiloteGreen, modifier = Modifier.size(16.dp))
                    Text("Bulletins validés — plus aucune modification n'est possible",
                        fontSize = 13.sp, color = EpiloteGreenDark, fontWeight = FontWeight.Medium)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EpiloteSurface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Rang",  modifier = Modifier.width(48.dp),  fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted, textAlign = TextAlign.Center)
            Text("Élève", modifier = Modifier.weight(2f),    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
            Text("Moy.",  modifier = Modifier.width(64.dp),  fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted, textAlign = TextAlign.Center)
            Text("Abs.",  modifier = Modifier.width(48.dp),  fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted, textAlign = TextAlign.Center)
            Text("Mention",modifier = Modifier.width(100.dp),fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
            Text("IA",    modifier = Modifier.width(80.dp),  fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted, textAlign = TextAlign.Center)
        }
        Divider()

        LazyColumn {
            items(bulletins) { bulletin ->
                BulletinRow(
                    bulletin = bulletin,
                    onRequestAppreciation = { onRequestAppreciation(bulletin) }
                )
                Divider(color = Color(0xFFF5F5F5))
            }
        }
    }
}

@Composable
private fun BulletinRow(
    bulletin: BulletinEleve,
    onRequestAppreciation: () -> Unit
) {
    val mention = mentionLabel(bulletin.moyenneGenerale)
    val mentionColor = mentionColor(bulletin.moyenneGenerale)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${bulletin.rang}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = when (bulletin.rang) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> EpiloteTextMuted
                }
            )
        }

        Column(modifier = Modifier.weight(2f)) {
            Text(
                "${bulletin.eleveNom} ${bulletin.elevePrenom}",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Text(
                "${bulletin.totalEleves} élève(s) · ${bulletin.moyennes.size} matière(s)",
                fontSize = 11.sp,
                color = EpiloteTextMuted
            )
        }

        Box(modifier = Modifier.width(64.dp), contentAlignment = Alignment.Center) {
            Text(
                String.format("%.2f", bulletin.moyenneGenerale),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = mentionColor
            )
        }

        Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            Text(
                "${bulletin.absencesCount}",
                fontSize = 13.sp,
                color = if (bulletin.absencesCount > 5) EpiloteRed else EpiloteTextMuted
            )
        }

        Surface(
            modifier = Modifier.width(100.dp),
            shape = RoundedCornerShape(6.dp),
            color = mentionColor.copy(alpha = 0.12f)
        ) {
            Text(
                mention,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = mentionColor,
                textAlign = TextAlign.Center
            )
        }

        Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) {
            IconButton(
                onClick = onRequestAppreciation,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "Générer appréciation IA",
                    tint = EpiloteGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// mentionLabel, mentionColor, AppreciationDialog, ClasseDropdown → BulletinComponents.kt
