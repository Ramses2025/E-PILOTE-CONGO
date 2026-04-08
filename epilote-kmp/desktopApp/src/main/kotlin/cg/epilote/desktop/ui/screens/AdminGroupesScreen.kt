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
import kotlinx.serialization.Serializable

@Serializable
data class GroupeDto(
    val id: String = "",
    val nom: String = "",
    val province: String = "",
    val planId: String = "",
    val ecolesCount: Int = 0,
    val createdAt: Long = 0
)

@Composable
fun AdminGroupesScreen(
    groupes: List<GroupeDto>,
    adminGroupesByGroup: Map<String, List<UserDto>>,
    isLoading: Boolean,
    onCreateGroupe: (nom: String, province: String, planId: String) -> Unit,
    onCreateAdminGroupe: (groupeId: String, username: String, password: String, nom: String, prenom: String, email: String) -> Unit,
    onRefresh: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedGroupeForAdmin by remember { mutableStateOf<GroupeDto?>(null) }
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
                Text("Groupes Scolaires", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text("Gérer les groupes d'établissements scolaires", fontSize = 13.sp, color = EpiloteTextMuted)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A9D8F))
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Nouveau Groupe")
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EpiloteGreen)
            }
        }

        if (groupes.isEmpty() && !isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Business, null, tint = EpiloteTextMuted, modifier = Modifier.size(48.dp))
                        Text("Aucun groupe scolaire", fontWeight = FontWeight.SemiBold, color = EpiloteTextMuted)
                        Text("Créez votre premier groupe pour commencer", fontSize = 12.sp, color = EpiloteTextMuted)
                    }
                }
            }
        }

        groupes.forEach { groupe ->
            val admins = adminGroupesByGroup[groupe.id].orEmpty()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(48.dp).background(Color(0xFF2A9D8F).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Business, null, tint = Color(0xFF2A9D8F), modifier = Modifier.size(24.dp))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(groupe.nom, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Province : ${groupe.province}", fontSize = 12.sp, color = EpiloteTextMuted)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${groupe.ecolesCount}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1D3557))
                                Text("écoles", fontSize = 11.sp, color = EpiloteTextMuted)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    groupe.planId.contains("pro") -> Color(0xFFE9C46A).copy(alpha = 0.15f)
                                    groupe.planId.contains("premium") -> Color(0xFF1D3557).copy(alpha = 0.1f)
                                    else -> Color(0xFF2A9D8F).copy(alpha = 0.1f)
                                }
                            ) {
                                Text(
                                    when {
                                        groupe.planId.contains("pro") -> "PRO"
                                        groupe.planId.contains("premium") -> "PREMIUM"
                                        else -> "GRATUIT"
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        groupe.planId.contains("pro") -> Color(0xFFE9C46A)
                                        groupe.planId.contains("premium") -> Color(0xFF1D3557)
                                        else -> Color(0xFF2A9D8F)
                                    }
                                )
                            }
                            FilledTonalButton(
                                onClick = { selectedGroupeForAdmin = groupe },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Admin Groupe")
                            }
                        }
                    }

                    Divider(color = Color(0xFFE9EEF5))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Admins du groupe", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF1D3557))

                        if (admins.isEmpty()) {
                            Text(
                                "Aucun admin groupe défini pour ce tenant.",
                                fontSize = 12.sp,
                                color = EpiloteTextMuted
                            )
                        } else {
                            admins.forEach { admin ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF7FAFC)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("${admin.firstName} ${admin.lastName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(admin.email.ifBlank { admin.username }, fontSize = 11.sp, color = EpiloteTextMuted)
                                        }
                                        Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFF6C5CE7).copy(alpha = 0.12f)) {
                                            Text(
                                                "ADMIN GROUPE",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF6C5CE7)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateGroupeDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { nom, province, planId ->
                onCreateGroupe(nom, province, planId)
                showCreateDialog = false
            }
        )
    }

    selectedGroupeForAdmin?.let { groupe ->
        CreateAdminGroupeDialog(
            groupe = groupe,
            onDismiss = { selectedGroupeForAdmin = null },
            onCreate = { username, password, nom, prenom, email ->
                onCreateAdminGroupe(groupe.id, username, password, nom, prenom, email)
                selectedGroupeForAdmin = null
            }
        )
    }
}

@Composable
private fun CreateGroupeDialog(
    onDismiss: () -> Unit,
    onCreate: (nom: String, province: String, planId: String) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var selectedPlan by remember { mutableStateOf("plan::gratuit") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau Groupe Scolaire", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom du groupe") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = province,
                    onValueChange = { province = it },
                    label = { Text("Province") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                Text("Plan d'abonnement", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("plan::gratuit", "Gratuit", Color(0xFF2A9D8F)),
                        Triple("plan::premium", "Premium", Color(0xFF1D3557)),
                        Triple("plan::pro", "Pro", Color(0xFFE9C46A))
                    ).forEach { (planId, label, color) ->
                        FilterChip(
                            selected = selectedPlan == planId,
                            onClick = { selectedPlan = planId },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.15f),
                                selectedLabelColor = color
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (nom.isNotBlank() && province.isNotBlank()) onCreate(nom, province, selectedPlan) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A9D8F)),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Créer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun CreateAdminGroupeDialog(
    groupe: GroupeDto,
    onDismiss: () -> Unit,
    onCreate: (username: String, password: String, nom: String, prenom: String, email: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvel Admin Groupe", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Groupe : ${groupe.nom}", fontSize = 12.sp, color = EpiloteTextMuted)
                OutlinedTextField(value = prenom, onValueChange = { prenom = it }, label = { Text("Prénom") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Identifiant") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mot de passe") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank() && password.length >= 8 && nom.isNotBlank() && prenom.isNotBlank() && email.isNotBlank()) {
                        onCreate(username, password, nom, prenom, email)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C5CE7)),
                shape = RoundedCornerShape(10.dp)
            ) { Text("Créer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
