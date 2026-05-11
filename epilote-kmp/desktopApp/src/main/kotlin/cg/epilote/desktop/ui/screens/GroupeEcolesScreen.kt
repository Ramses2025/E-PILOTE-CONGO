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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.*
import cg.epilote.desktop.ui.theme.cursorHand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GroupeEcolesScreen(
    groupeRepo: GroupeAdminDataRepository
) {
    val ecoles by groupeRepo.ecoles.collectAsState()
    val isOffline by groupeRepo.isOffline.collectAsState()
    val scope = rememberCoroutineScope()
    var reloadTick by remember { mutableStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }

    LaunchedEffect(reloadTick) {
        if (reloadTick > 0) groupeRepo.refreshAll(showLoading = false)
        while (true) {
            delay(120_000)
            groupeRepo.refreshAll(showLoading = false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Écoles", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("${ecoles.size} école${if (ecoles.size > 1) "s" else ""} dans votre groupe", fontSize = 13.sp, color = Color(0xFF64748B))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isOffline) {
                    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFEF3C7)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Default.WifiOff, null, tint = Color(0xFFD97706), modifier = Modifier.size(13.dp))
                            Text("Hors ligne", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Medium)
                        }
                    }
                }
                IconButton(onClick = { reloadTick++ }, modifier = Modifier.cursorHand()) {
                    Icon(Icons.Default.Refresh, "Actualiser", tint = Color(0xFF64748B))
                }
                Button(
                    onClick = { showCreateDialog = true },
                    enabled = !isOffline,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nouvelle école", fontSize = 13.sp)
                }
            }
        }

        // Feedback
        feedback?.let { fb ->
            AdminFeedbackBanner(feedback = fb, onDismiss = { feedback = null })
        }

        // Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            if (ecoles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.School, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(40.dp))
                        Text("Aucune école enregistrée", fontSize = 14.sp, color = Color(0xFF94A3B8))
                        Text("Cliquez sur « Nouvelle école » pour commencer.", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                    }
                }
            } else {
                Column {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("#", modifier = Modifier.width(30.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
                        Text("Nom", modifier = Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                        Text("Province", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                        Text("Territoire", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                        Text("Niveaux", modifier = Modifier.weight(1.5f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                        Text("Créée le", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    ecoles.forEachIndexed { idx, ecole ->
                        val rowBg = if (idx % 2 == 0) Color.White else Color(0xFFFAFAFA)
                        Row(
                            modifier = Modifier.fillMaxWidth().background(rowBg).padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${idx + 1}", modifier = Modifier.width(30.dp), fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text(ecole.nom, modifier = Modifier.weight(2f), fontSize = 13.sp, color = Color(0xFF1E293B), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                            Text(ecole.province, modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(ecole.territoire, modifier = Modifier.weight(1f), fontSize = 12.sp, color = Color(0xFF475569), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                ecole.niveaux.joinToString(", ").ifBlank { "—" },
                                modifier = Modifier.weight(1.5f),
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                cg.epilote.desktop.ui.screens.superadmin.formatDate(ecole.createdAt),
                                modifier = Modifier.weight(1f),
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        if (idx < ecoles.lastIndex) HorizontalDivider(color = Color(0xFFE2E8F0))
                    }
                }
            }
        }
    }

    // Création d'une école
    if (showCreateDialog) {
        CreateEcoleDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { dto ->
                scope.launch {
                    val result = groupeRepo.createEcole(dto)
                    showCreateDialog = false
                    feedback = if (result != null) {
                        AdminFeedbackMessage("École « ${result.nom} » créée avec succès.")
                    } else {
                        AdminFeedbackMessage("Erreur lors de la création. Vérifiez votre connexion.", isError = true)
                    }
                }
            }
        )
    }
}

// ── Dialogue création école ───────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateEcoleDialog(
    onDismiss: () -> Unit,
    onConfirm: (CreateEcoleDto) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var territoire by remember { mutableStateOf("") }
    val niveauxOptions = listOf("maternelle", "primaire", "secondaire", "lycée", "technique")
    val selectedNiveaux = remember { mutableStateListOf<String>("primaire") }
    var nomError by remember { mutableStateOf(false) }

    AdminDialogWindow(
        title = "Nouvelle école",
        subtitle = "Enregistrer une nouvelle école dans votre groupe",
        onDismiss = onDismiss,
        size = DpSize(540.dp, 420.dp),
        content = {
            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it; nomError = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nom de l'école *") },
                isError = nomError,
                supportingText = if (nomError) ({ Text("Le nom est requis") }) else null,
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = province,
                    onValueChange = { province = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Province") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = territoire,
                    onValueChange = { territoire = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Territoire / Commune") },
                    singleLine = true
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Niveaux enseignés", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    niveauxOptions.forEach { niveau ->
                        val selected = niveau in selectedNiveaux
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) selectedNiveaux.remove(niveau) else selectedNiveaux.add(niveau)
                            },
                            label = { Text(niveau.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                            modifier = Modifier.cursorHand()
                        )
                    }
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss, modifier = Modifier.cursorHand()) { Text("Annuler") }
            Button(
                onClick = {
                    if (nom.isBlank()) { nomError = true; return@Button }
                    onConfirm(
                        CreateEcoleDto(
                            nom = nom.trim(),
                            province = province.trim(),
                            territoire = territoire.trim(),
                            niveaux = selectedNiveaux.toList().ifEmpty { listOf("primaire") }
                        )
                    )
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.cursorHand(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Créer l'école", fontSize = 13.sp)
            }
        }
    )
}
