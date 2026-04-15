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
import androidx.compose.ui.window.Dialog
import cg.epilote.desktop.ui.theme.*
import cg.epilote.shared.domain.model.BulletinEleve
import cg.epilote.shared.domain.model.UserSession
import cg.epilote.shared.presentation.viewmodel.BulletinUiState
import cg.epilote.shared.presentation.viewmodel.BulletinViewModel
import cg.epilote.shared.presentation.viewmodel.ClassesViewModel

data class AppreciationResult(
    val eleveNom: String,
    val appreciation: String,
    val mention: String,
    val conseil: String,
    val fallback: Boolean
)

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

            ClasseDropdown(
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

private fun mentionLabel(moy: Double) = when {
    moy >= 16 -> "Excellent"
    moy >= 14 -> "Très Bien"
    moy >= 12 -> "Bien"
    moy >= 10 -> "Assez Bien"
    moy >= 8  -> "Passable"
    else      -> "Insuffisant"
}

private fun mentionColor(moy: Double) = when {
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
private fun ClasseDropdown(
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
