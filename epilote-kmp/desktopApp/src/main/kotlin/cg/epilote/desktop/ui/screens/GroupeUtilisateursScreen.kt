package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.*
import cg.epilote.desktop.ui.theme.cursorHand
import kotlinx.coroutines.launch

@Composable
fun GroupeUtilisateursScreen(
    groupeRepo: GroupeAdminDataRepository
) {
    val ecoles by groupeRepo.ecoles.collectAsState()
    val profils by groupeRepo.profils.collectAsState()
    val usersByEcole by groupeRepo.usersByEcole.collectAsState()
    val isOffline by groupeRepo.isOffline.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedEcoleId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }

    LaunchedEffect(selectedEcoleId) {
        selectedEcoleId?.let { id ->
            if (usersByEcole[id] == null) groupeRepo.refreshUsersForEcole(id)
        }
    }
    LaunchedEffect(ecoles) {
        if (selectedEcoleId == null && ecoles.isNotEmpty()) selectedEcoleId = ecoles.first().id
    }

    val currentUsers = usersByEcole[selectedEcoleId] ?: emptyList()

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4F8))
            .verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Utilisateurs", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("Gestion des utilisateurs par école", fontSize = 13.sp, color = Color(0xFF64748B))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isOffline) OfflineBadge()
                Button(
                    onClick = { showCreateDialog = true },
                    enabled = !isOffline && selectedEcoleId != null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                ) {
                    Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nouvel utilisateur", fontSize = 13.sp)
                }
            }
        }

        feedback?.let { fb -> AdminFeedbackBanner(feedback = fb, onDismiss = { feedback = null }) }

        if (ecoles.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sélectionner une école", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ecoles.forEach { ecole ->
                            val isSelected = ecole.id == selectedEcoleId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedEcoleId = ecole.id; scope.launch { groupeRepo.refreshUsersForEcole(ecole.id) } },
                                label = { Text(ecole.nom, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingIcon = if (isSelected) ({ Icon(Icons.Default.School, null, modifier = Modifier.size(14.dp)) }) else null,
                                modifier = Modifier.cursorHand()
                            )
                        }
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                    Text("Vous devez d'abord créer une école avant de pouvoir gérer des utilisateurs.",
                        fontSize = 13.sp, color = Color(0xFF78350F))
                }
            }
        }

        val selectedEcole = ecoles.find { it.id == selectedEcoleId }
        if (selectedEcole != null) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${selectedEcole.nom} — ${currentUsers.size} utilisateur${if (currentUsers.size > 1) "s" else ""}",
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                        if (usersByEcole[selectedEcoleId] == null)
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    if (currentUsers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.People, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(36.dp))
                                Text("Aucun utilisateur dans cette école.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Spacer(Modifier.width(40.dp))
                            Text("Nom", modifier = Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                            Text("Email", modifier = Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                            Text("Profil", modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                            Text("Statut", modifier = Modifier.width(70.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        currentUsers.forEachIndexed { idx, user ->
                            UserRow(
                                user    = user,
                                profils = profils,
                                rowBg   = if (idx % 2 == 0) Color.White else Color(0xFFFAFAFA),
                                onAssignProfil = { profilId ->
                                    scope.launch {
                                        val ok = groupeRepo.assignProfil(user.id, AssignProfilDto(profilId))
                                        feedback = if (ok) {
                                            groupeRepo.refreshUsersForEcole(selectedEcoleId!!)
                                            AdminFeedbackMessage("Profil mis à jour pour ${user.firstName}.")
                                        } else AdminFeedbackMessage("Erreur lors de l'affectation du profil.", isError = true)
                                    }
                                }
                            )
                            if (idx < currentUsers.lastIndex) HorizontalDivider(color = Color(0xFFE2E8F0))
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog && selectedEcoleId != null) {
        CreateUserGroupeDialog(
            ecole   = ecoles.find { it.id == selectedEcoleId }!!,
            profils = profils,
            onDismiss = { showCreateDialog = false },
            onConfirm = { dto ->
                scope.launch {
                    val result = groupeRepo.createUser(dto)
                    showCreateDialog = false
                    feedback = if (result != null) {
                        groupeRepo.refreshUsersForEcole(selectedEcoleId!!)
                        AdminFeedbackMessage("Utilisateur « ${result.firstName} ${result.lastName} » créé avec succès.")
                    } else AdminFeedbackMessage("Erreur lors de la création. Vérifiez les informations saisies.", isError = true)
                }
            }
        )
    }
}
