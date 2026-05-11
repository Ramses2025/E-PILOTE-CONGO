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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.*
import cg.epilote.desktop.ui.theme.cursorHand
import kotlinx.coroutines.launch

@Composable
fun GroupeProfilsScreen(groupeRepo: GroupeAdminDataRepository) {
    val profils by groupeRepo.profils.collectAsState()
    val modules by groupeRepo.modules.collectAsState()
    val isOffline by groupeRepo.isOffline.collectAsState()
    val scope = rememberCoroutineScope()
    var reloadTick by remember { mutableStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedProfil by remember { mutableStateOf<ProfilApiDto?>(null) }
    var feedback by remember { mutableStateOf<AdminFeedbackMessage?>(null) }

    LaunchedEffect(reloadTick) {
        if (reloadTick > 0) groupeRepo.refreshAll(showLoading = false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4F8))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Profils d'accès", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("${profils.size} profil${if (profils.size > 1) "s" else ""} configuré${if (profils.size > 1) "s" else ""}", fontSize = 13.sp, color = Color(0xFF64748B))
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
                            Text("Hors ligne", fontSize = 11.sp, color = Color(0xFF92400E))
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899))
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nouveau profil", fontSize = 13.sp)
                }
            }
        }

        // ── Feedback ──────────────────────────────────────────────────────────
        feedback?.let { fb ->
            AdminFeedbackBanner(feedback = fb, onDismiss = { feedback = null })
        }

        // ── Grille de profils ─────────────────────────────────────────────────
        if (profils.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Tune, null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(40.dp))
                        Text("Aucun profil d'accès défini", fontSize = 14.sp, color = Color(0xFF94A3B8))
                        Text("Créez un profil pour définir les droits des utilisateurs.", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                    }
                }
            }
        } else {
            // Layout: 2 colonnes
            val chunked = profils.chunked(2)
            chunked.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { profil ->
                        ProfilCard(
                            profil = profil,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedProfil = profil }
                        )
                    }
                    if (row.size < 2) Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    // ── Dialogue création profil ──────────────────────────────────────────────
    if (showCreateDialog) {
        CreateProfilDialog(
            modules   = modules,
            onDismiss = { showCreateDialog = false },
            onConfirm = { dto ->
                scope.launch {
                    val result = groupeRepo.createProfil(dto)
                    showCreateDialog = false
                    feedback = if (result != null) {
                        AdminFeedbackMessage("Profil « ${result.nom} » créé avec succès.")
                    } else {
                        AdminFeedbackMessage("Erreur lors de la création. Vérifiez votre connexion.", isError = true)
                    }
                }
            }
        )
    }

    // ── Dialogue détail profil ────────────────────────────────────────────────
    selectedProfil?.let { profil ->
        ProfilDetailDialog(profil = profil, onDismiss = { selectedProfil = null })
    }
}

// ── Carte profil ──────────────────────────────────────────────────────────────

@Composable
private fun ProfilCard(profil: ProfilApiDto, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.cursorHand(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).background(Color(0xFFFCE7F3), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tune, null, tint = Color(0xFFEC4899), modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onClick, modifier = Modifier.cursorHand()) {
                    Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                }
            }
            Text(profil.nom, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
            Text(
                "${profil.permissions.size} permission${if (profil.permissions.size > 1) "s" else ""}",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
            // Chips des modules
            if (profil.permissions.isNotEmpty()) {
                val toShow = profil.permissions.take(3)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    toShow.forEach { perm ->
                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFF3E8FF)) {
                            Text(
                                perm.moduleSlug,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                color = Color(0xFF7C3AED)
                            )
                        }
                    }
                    if (profil.permissions.size > 3) {
                        Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFE2E8F0)) {
                            Text(
                                "+${profil.permissions.size - 3}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
            Text(
                "Créé le ${cg.epilote.desktop.ui.screens.superadmin.formatDate(profil.createdAt)}",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

// ── Dialogue détail profil ────────────────────────────────────────────────────

@Composable
private fun ProfilDetailDialog(profil: ProfilApiDto, onDismiss: () -> Unit) {
    AdminDialogWindow(
        title     = profil.nom,
        subtitle  = "${profil.permissions.size} permissions configurées",
        onDismiss = onDismiss,
        size      = DpSize(620.dp, 520.dp),
        content   = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (profil.permissions.isEmpty()) {
                    Text("Aucune permission assignée à ce profil.", fontSize = 13.sp, color = Color(0xFF94A3B8))
                } else {
                    // En-tête tableau
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Module", modifier = Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                        listOf("Lire", "Écrire", "Supprimer", "Exporter").forEach { label ->
                            Text(label, modifier = Modifier.width(70.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    profil.permissions.forEachIndexed { idx, perm ->
                        val rowBg = if (idx % 2 == 0) Color.White else Color(0xFFFAFAFA)
                        Row(
                            modifier = Modifier.fillMaxWidth().background(rowBg).padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(perm.moduleSlug, modifier = Modifier.weight(2f), fontSize = 13.sp, color = Color(0xFF1E293B))
                            PermCheck(perm.canRead)
                            PermCheck(perm.canWrite)
                            PermCheck(perm.canDelete)
                            PermCheck(perm.canExport)
                        }
                        if (idx < profil.permissions.lastIndex) HorizontalDivider(color = Color(0xFFE2E8F0))
                    }
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss, modifier = Modifier.cursorHand()) { Text("Fermer") }
        }
    )
}

@Composable
private fun PermCheck(value: Boolean) {
    Box(modifier = Modifier.width(70.dp), contentAlignment = Alignment.CenterStart) {
        Icon(
            if (value) Icons.Default.CheckCircle else Icons.Default.RemoveCircle,
            contentDescription = null,
            tint = if (value) Color(0xFF059669) else Color(0xFFE2E8F0),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Dialogue création profil ──────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateProfilDialog(
    modules: List<ModuleApiDto>,
    onDismiss: () -> Unit,
    onConfirm: (CreateProfilDto) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var nomError by remember { mutableStateOf(false) }
    // Map moduleSlug -> permissions flags
    val permissions = remember(modules) {
        mutableStateMapOf<String, ProfilPermissionDto>().also { map ->
            modules.forEach { m -> map[m.code] = ProfilPermissionDto(moduleSlug = m.code) }
        }
    }

    AdminDialogWindow(
        title     = "Nouveau profil d'accès",
        subtitle  = "Définissez les permissions du profil",
        onDismiss = onDismiss,
        size      = DpSize(680.dp, 560.dp),
        content   = {
            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it; nomError = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nom du profil *") },
                isError = nomError,
                singleLine = true,
                supportingText = if (nomError) ({ Text("Le nom est requis") }) else null
            )
            if (modules.isEmpty()) {
                Surface(color = Color(0xFFFFF3CD), shape = RoundedCornerShape(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                        Text("Aucun module disponible. Les permissions seront vides.", fontSize = 12.sp, color = Color(0xFF78350F))
                    }
                }
            } else {
                Text("Permissions par module", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Module", modifier = Modifier.weight(2f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                    listOf("Lire", "Écrire", "Suppr.", "Export").forEach { label ->
                        Text(label, modifier = Modifier.width(58.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                    }
                }
                HorizontalDivider(color = Color(0xFFE2E8F0))
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.height(240.dp).verticalScroll(scrollState)) {
                    modules.forEachIndexed { idx, module ->
                        val perm = permissions[module.code] ?: ProfilPermissionDto(moduleSlug = module.code)
                        val rowBg = if (idx % 2 == 0) Color.White else Color(0xFFFAFAFA)
                        Row(
                            modifier = Modifier.fillMaxWidth().background(rowBg).padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(module.nom, modifier = Modifier.weight(2f), fontSize = 12.sp, color = Color(0xFF1E293B))
                            PermToggle(perm.canRead,   { permissions[module.code] = perm.copy(canRead = it)   })
                            PermToggle(perm.canWrite,  { permissions[module.code] = perm.copy(canWrite = it)  })
                            PermToggle(perm.canDelete, { permissions[module.code] = perm.copy(canDelete = it) })
                            PermToggle(perm.canExport, { permissions[module.code] = perm.copy(canExport = it) })
                        }
                        if (idx < modules.lastIndex) HorizontalDivider(color = Color(0xFFE2E8F0))
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
                        CreateProfilDto(
                            nom = nom.trim(),
                            permissions = permissions.values.filter { it.canRead || it.canWrite || it.canDelete || it.canExport }.toList()
                        )
                    )
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.cursorHand(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899))
            ) {
                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Créer le profil", fontSize = 13.sp)
            }
        }
    )
}

@Composable
private fun PermToggle(value: Boolean, onChange: (Boolean) -> Unit) {
    Box(modifier = Modifier.width(58.dp), contentAlignment = Alignment.CenterStart) {
        Checkbox(
            checked = value,
            onCheckedChange = onChange,
            modifier = Modifier.size(24.dp).cursorHand(),
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEC4899))
        )
    }
}
