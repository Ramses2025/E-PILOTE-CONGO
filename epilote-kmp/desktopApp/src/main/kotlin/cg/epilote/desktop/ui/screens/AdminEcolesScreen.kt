package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import kotlinx.serialization.Serializable

@Serializable
data class EcoleDto(
    val id: String = "",
    val groupId: String = "",
    val nom: String = "",
    val province: String = "",
    val territoire: String = "",
    val niveaux: List<String> = emptyList(),
    val planId: String = "",
    val createdAt: Long = 0
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminEcolesScreen(
    ecoles: List<EcoleDto>,
    groupes: List<GroupeDto>,
    isLoading: Boolean,
    onCreateEcole: (groupId: String, nom: String, province: String, territoire: String) -> Unit,
    onRefresh: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Écoles", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Gestion des établissements scolaires", fontSize = 13.sp, color = EpiloteTextMuted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onRefresh, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Actualiser")
                }
                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D3557)),
                    enabled = groupes.isNotEmpty()
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nouvelle École")
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        }

        if (ecoles.isEmpty() && !isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.School, null, tint = EpiloteTextMuted, modifier = Modifier.size(48.dp))
                        Text("Aucune école", fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
                        Text("Créez un groupe d'abord, puis ajoutez des écoles", fontSize = 12.sp, color = EpiloteTextMuted)
                    }
                }
            }
        }

        ecoles.forEach { ecole ->
            val groupeNom = groupes.find { it.id == ecole.groupId }?.nom ?: ecole.groupId
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).background(Color(0xFF1D3557).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.School, null, tint = Color(0xFF1D3557), modifier = Modifier.size(24.dp))
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(ecole.nom, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("${ecole.province} — ${ecole.territoire}", fontSize = 12.sp, color = EpiloteTextMuted)
                            Text("Groupe : $groupeNom", fontSize = 11.sp, color = EpiloteTextMuted)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ecole.niveaux.forEach { niveau ->
                                Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1D3557).copy(alpha = 0.08f)) {
                                    Text(niveau, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp, color = Color(0xFF1D3557), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog && groupes.isNotEmpty()) {
        CreateEcoleDialog(
            groupes = groupes,
            onDismiss = { showCreateDialog = false },
            onCreate = { groupId, nom, province, territoire ->
                onCreateEcole(groupId, nom, province, territoire)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun CreateEcoleDialog(
    groupes: List<GroupeDto>,
    onDismiss: () -> Unit,
    onCreate: (groupId: String, nom: String, province: String, territoire: String) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var territoire by remember { mutableStateOf("") }
    var selectedGroupeId by remember { mutableStateOf(groupes.firstOrNull()?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle École", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Groupe scolaire", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    groupes.forEach { g ->
                        FilterChip(
                            selected = selectedGroupeId == g.id,
                            onClick = { selectedGroupeId = g.id },
                            label = { Text(g.nom, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = nom, onValueChange = { nom = it },
                    label = { Text("Nom de l'école") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = province, onValueChange = { province = it },
                    label = { Text("Province") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = territoire, onValueChange = { territoire = it },
                    label = { Text("Territoire / Ville") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (nom.isNotBlank() && province.isNotBlank()) onCreate(selectedGroupeId, nom, province, territoire) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D3557)),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Créer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
