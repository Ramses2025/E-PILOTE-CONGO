package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import cg.epilote.shared.domain.model.Classe
import cg.epilote.shared.domain.model.Matiere
import cg.epilote.shared.domain.model.UserSession
import cg.epilote.shared.presentation.viewmodel.ClassesViewModel

@Composable
fun ClassesScreen(
    session: UserSession,
    viewModel: ClassesViewModel
) {
    val classes by viewModel.classes.collectAsState()
    val selectedClasse by viewModel.selectedClasse.collectAsState()
    val matieres by viewModel.matieres.collectAsState()

    LaunchedEffect(session.ecoleId) {
        session.ecoleId?.let { viewModel.loadClasses(it) }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight()
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Classes", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EpiloteGreenLight
                ) {
                    Text(
                        "${classes.size} classe(s)",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 12.sp,
                        color = EpiloteGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Divider()

            if (classes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Class, null,
                            tint = Color(0xFFDFE1E6), modifier = Modifier.size(40.dp))
                        Text("Aucune classe", color = EpiloteTextMuted, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn {
                    items(classes) { classe ->
                        ClasseListItem(
                            classe = classe,
                            isSelected = selectedClasse?.id == classe.id,
                            onClick = {
                                session.ecoleId?.let { viewModel.selectClasse(classe, it) }
                            }
                        )
                    }
                }
            }
        }

        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

        if (selectedClasse == null) {
            Box(
                modifier = Modifier.fillMaxSize().background(EpiloteSurface),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, null,
                        tint = Color(0xFFDFE1E6), modifier = Modifier.size(40.dp))
                    Text("Sélectionnez une classe pour voir ses matières",
                        color = EpiloteTextMuted, fontSize = 14.sp)
                }
            }
        } else {
            MatieresPanel(
                classe = selectedClasse!!,
                matieres = matieres
            )
        }
    }
}

@Composable
private fun ClasseListItem(
    classe: Classe,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) EpiloteGreenLight else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isSelected) EpiloteGreen else Color(0xFFF0F0F0),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                classe.nom.take(2).uppercase(),
                color = if (isSelected) Color.White else EpiloteTextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                classe.nom,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (isSelected) EpiloteGreenDark else MaterialTheme.colorScheme.onSurface
            )
            Text(
                classe.niveauNom + (classe.section?.let { " — $it" } ?: ""),
                fontSize = 12.sp,
                color = EpiloteTextMuted
            )
        }
        if (isSelected) {
            Icon(Icons.Default.ChevronRight, null, tint = EpiloteGreen, modifier = Modifier.size(16.dp))
        }
    }
    Divider(color = Color(0xFFF5F5F5))
}

@Composable
private fun MatieresPanel(classe: Classe, matieres: List<Matiere>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EpiloteSurface)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(classe.nom, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${classe.niveauNom} · Capacité : ${classe.capacite} élèves",
                    fontSize = 13.sp,
                    color = EpiloteTextMuted
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = EpiloteGreenLight
            ) {
                Text(
                    "${matieres.size} matière(s)",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = EpiloteGreen,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (matieres.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Book, null,
                            tint = Color(0xFFDFE1E6), modifier = Modifier.size(36.dp))
                        Text("Aucune matière assignée à cette classe",
                            color = EpiloteTextMuted, fontSize = 13.sp)
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(matieres) { matiere ->
                    MatiereCard(matiere = matiere)
                }
            }
        }
    }
}

@Composable
private fun MatiereCard(matiere: Matiere) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFE6F0FF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    matiere.code.take(3).uppercase(),
                    color = Color(0xFF0052CC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(matiere.nom, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "Enseignant : ${matiere.enseignantNom.ifBlank { "Non assigné" }}",
                    fontSize = 12.sp,
                    color = EpiloteTextMuted
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Coeff. ${matiere.coefficient}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EpiloteGreen
                )
                Text(
                    "${matiere.heuresParSemaine}h/sem",
                    fontSize = 11.sp,
                    color = EpiloteTextMuted
                )
            }
        }
    }
}
