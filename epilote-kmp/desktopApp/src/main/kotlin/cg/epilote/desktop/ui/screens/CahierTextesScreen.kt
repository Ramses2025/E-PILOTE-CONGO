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
import cg.epilote.shared.domain.model.UserSession

data class CahierEntry(
    val id: String,
    val titre: String,
    val matiere: String,
    val type: String,
    val date: String,
    val contenu: String,
    val genereParIA: Boolean = false
)

@Composable
fun CahierTextesScreen(
    session: UserSession,
    onRequestGenerateContent: (titre: String, niveau: String, matiere: String, type: String) -> Unit
) {
    var entries by remember { mutableStateOf(listOf<CahierEntry>()) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<CahierEntry?>(null) }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(320.dp)
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
                Text("Cahier de textes", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showGenerateDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EpiloteGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Générer avec IA", fontSize = 12.sp)
                }
            }
            Divider()

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Book, null,
                            tint = Color(0xFFDFE1E6), modifier = Modifier.size(40.dp))
                        Text("Aucune entrée", color = EpiloteTextMuted, fontSize = 13.sp)
                        Text("Utilisez \"Générer avec IA\" pour créer\nvotre premier contenu",
                            color = EpiloteTextMuted, fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            } else {
                LazyColumn {
                    items(entries) { entry ->
                        CahierEntryItem(
                            entry = entry,
                            isSelected = selectedEntry?.id == entry.id,
                            onClick = { selectedEntry = entry }
                        )
                    }
                }
            }
        }

        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EpiloteSurface)
        ) {
            if (selectedEntry != null) {
                CahierEntryDetail(entry = selectedEntry!!)
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Article, null,
                            tint = Color(0xFFDFE1E6), modifier = Modifier.size(48.dp))
                        Text("Sélectionnez une entrée pour la lire",
                            color = EpiloteTextMuted, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showGenerateDialog) {
        GenerateContentDialog(
            onGenerate = { titre, niveau, matiere, type ->
                onRequestGenerateContent(titre, niveau, matiere, type)
                showGenerateDialog = false
            },
            onDismiss = { showGenerateDialog = false }
        )
    }
}

@Composable
private fun CahierEntryItem(
    entry: CahierEntry,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val typeColors = mapOf(
        "COURS"          to Pair(Color(0xFFE6F0FF), Color(0xFF0052CC)),
        "EXAMEN"         to Pair(Color(0xFFFFEDEB), Color(0xFFDE350B)),
        "DEVOIR"         to Pair(Color(0xFFFFF0D1), Color(0xFFFF8B00)),
        "FICHE_REVISION" to Pair(Color(0xFFF3F0FF), Color(0xFF6554C0)),
        "PROGRESSION"    to Pair(Color(0xFFE3F7EF), Color(0xFF00875A))
    )
    val (bg, fg) = typeColors[entry.type] ?: Pair(EpiloteSurface, EpiloteTextMuted)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) EpiloteGreenLight else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(shape = RoundedCornerShape(4.dp), color = bg) {
                Text(
                    entry.type.replace("_", " "),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    color = fg,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (entry.genereParIA) {
                Surface(shape = RoundedCornerShape(4.dp), color = EpiloteGreenLight) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = EpiloteGreen, modifier = Modifier.size(9.dp))
                        Text("IA", fontSize = 10.sp, color = EpiloteGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(entry.date, fontSize = 10.sp, color = EpiloteTextMuted)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            entry.titre,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = if (isSelected) EpiloteGreenDark else MaterialTheme.colorScheme.onSurface
        )
        Text(entry.matiere, fontSize = 11.sp, color = EpiloteTextMuted)
    }
    Divider(color = Color(0xFFF5F5F5))
}

@Composable
private fun CahierEntryDetail(entry: CahierEntry) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.titre, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(entry.matiere, fontSize = 13.sp, color = EpiloteTextMuted)
                    Text("·", color = EpiloteTextMuted)
                    Text(entry.date, fontSize = 13.sp, color = EpiloteTextMuted)
                    if (entry.genereParIA) {
                        Surface(shape = RoundedCornerShape(4.dp), color = EpiloteGreenLight) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, tint = EpiloteGreen, modifier = Modifier.size(10.dp))
                                Text("Généré par IA", fontSize = 11.sp, color = EpiloteGreen)
                            }
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(20.dp)
            ) {
                item {
                    Text(
                        entry.contenu,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun GenerateContentDialog(
    onGenerate: (titre: String, niveau: String, matiere: String, type: String) -> Unit,
    onDismiss: () -> Unit
) {
    var titre   by remember { mutableStateOf("") }
    var niveau  by remember { mutableStateOf("") }
    var matiere by remember { mutableStateOf("") }
    var type    by remember { mutableStateOf("COURS") }
    val types   = listOf("COURS", "EXAMEN", "DEVOIR", "FICHE_REVISION", "PROGRESSION")

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
                        modifier = Modifier
                            .size(36.dp)
                            .background(EpiloteGreenLight, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = EpiloteGreen, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Générer avec l'IA", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Programmes officiels MEPSA · Congo Brazzaville",
                            fontSize = 12.sp, color = EpiloteTextMuted)
                    }
                }

                OutlinedTextField(
                    value = titre,
                    onValueChange = { titre = it },
                    label = { Text("Titre / Sujet de la leçon") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = matiere,
                        onValueChange = { matiere = it },
                        label = { Text("Matière") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = niveau,
                        onValueChange = { niveau = it },
                        label = { Text("Niveau (ex: Terminale D)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Type de contenu", fontSize = 13.sp, color = EpiloteTextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t.replace("_", " "), fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EpiloteGreenLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = EpiloteGreen, modifier = Modifier.size(14.dp))
                        Text(
                            "Le contenu sera généré en respectant les programmes officiels du MEPSA",
                            fontSize = 12.sp,
                            color = EpiloteGreenDark
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = {
                            if (titre.isNotBlank() && niveau.isNotBlank()) {
                                onGenerate(titre, niveau, matiere, type)
                            }
                        },
                        enabled = titre.isNotBlank() && niveau.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EpiloteGreen)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Générer")
                    }
                }
            }
        }
    }
}
