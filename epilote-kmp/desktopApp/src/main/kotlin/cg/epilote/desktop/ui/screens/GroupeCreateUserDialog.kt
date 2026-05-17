package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.CreateUserGroupeDto
import cg.epilote.desktop.data.EcoleApiDto
import cg.epilote.desktop.data.ProfilApiDto
import cg.epilote.desktop.ui.theme.cursorHand

@Composable
internal fun CreateUserGroupeDialog(
    ecole: EcoleApiDto,
    profils: List<ProfilApiDto>,
    onDismiss: () -> Unit,
    onConfirm: (CreateUserGroupeDto) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedProfilId by remember { mutableStateOf(profils.firstOrNull()?.id ?: "") }
    var nomError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    AdminDialogWindow(
        title     = "Nouvel utilisateur",
        subtitle  = "Créer un compte utilisateur pour ${ecole.nom}",
        onDismiss = onDismiss,
        size      = DpSize(540.dp, 500.dp),
        content   = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nom, onValueChange = { nom = it; nomError = false },
                    modifier = Modifier.weight(1f), label = { Text("Nom *") },
                    isError = nomError, singleLine = true
                )
                OutlinedTextField(
                    value = prenom, onValueChange = { prenom = it },
                    modifier = Modifier.weight(1f), label = { Text("Prénom") }, singleLine = true
                )
            }
            OutlinedTextField(
                value = email, onValueChange = { email = it; emailError = false },
                modifier = Modifier.fillMaxWidth(), label = { Text("Email *") },
                isError = emailError, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it; passwordError = false },
                modifier = Modifier.fillMaxWidth(), label = { Text("Mot de passe *") },
                isError = passwordError, singleLine = true,
                visualTransformation = if (passwordVisible)
                    androidx.compose.ui.text.input.VisualTransformation.None
                else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null, modifier = Modifier.size(18.dp))
                    }
                },
                supportingText = if (passwordError) ({ Text("8 caractères minimum") }) else null
            )
            if (profils.isNotEmpty()) {
                var profilExpanded by remember { mutableStateOf(false) }
                val profilNom = profils.find { it.id == selectedProfilId }?.nom ?: "Sélectionner un profil"
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = profilNom, onValueChange = {},
                        modifier = Modifier.fillMaxWidth(), label = { Text("Profil d'accès") },
                        readOnly = true, enabled = false,
                        trailingIcon = { Icon(Icons.Default.ExpandMore, null) }
                    )
                    Box(Modifier.matchParentSize().clickable { profilExpanded = true }.cursorHand())
                    DropdownMenu(expanded = profilExpanded, onDismissRequest = { profilExpanded = false }) {
                        profils.forEach { profil ->
                            DropdownMenuItem(
                                text = { Text(profil.nom, fontSize = 13.sp) },
                                onClick = { selectedProfilId = profil.id; profilExpanded = false },
                                leadingIcon = {
                                    if (profil.id == selectedProfilId)
                                        Icon(Icons.Default.Check, null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss, modifier = Modifier.cursorHand()) { Text("Annuler") }
            Button(
                onClick = {
                    nomError      = nom.isBlank()
                    emailError    = email.isBlank() || !email.contains("@")
                    passwordError = password.length < 8
                    if (nomError || emailError || passwordError) return@Button
                    onConfirm(CreateUserGroupeDto(
                        password  = password,
                        nom       = nom.trim(),
                        prenom    = prenom.trim(),
                        email     = email.trim(),
                        schoolId  = ecole.id,
                        profilId  = selectedProfilId
                    ))
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.cursorHand(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Créer le compte", fontSize = 13.sp)
            }
        }
    )
}

@Composable
internal fun OfflineBadge() {
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFEF3C7)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(Icons.Default.WifiOff, null, tint = Color(0xFFD97706), modifier = Modifier.size(13.dp))
            Text("Hors ligne", fontSize = 11.sp, color = Color(0xFF92400E),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
        }
    }
}
