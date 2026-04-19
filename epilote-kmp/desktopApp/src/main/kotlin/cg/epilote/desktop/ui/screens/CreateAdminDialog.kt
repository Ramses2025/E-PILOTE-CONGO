package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.CreateAdminUserDto
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import cg.epilote.desktop.ui.theme.cursorHand
import cg.epilote.desktop.ui.screens.LogoSelectionResult

@Composable
fun CreateAdminDialog(
    groupes: List<GroupeDto>,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (CreateAdminUserDto) -> Unit
) {
    var role by remember { mutableStateOf("ADMIN_GROUPE") }
    var prenom by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<String?>(null) }
    var dateOfBirth by remember { mutableStateOf<String?>(null) }
    var address by remember { mutableStateOf("") }
    var birthPlace by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableStateOf("") }
    var mustChangePassword by remember { mutableStateOf(true) }
    var avatarData by remember { mutableStateOf<String?>(null) }
    var avatarFileName by remember { mutableStateOf<String?>(null) }
    var avatarError by remember { mutableStateOf<String?>(null) }

    var showGroupMenu by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val groupOptions = groupes.map { it.id to it.nom }

    fun validate(): String? {
        if (prenom.isBlank()) return "Le prénom est requis"
        if (nom.isBlank()) return "Le nom est requis"
        if (email.isBlank()) return "L'email est requis"
        if (!email.contains("@")) return "Email invalide"
        if (password.length < 8) return "Le mot de passe doit contenir au moins 8 caractères"
        if (password != passwordConfirm) return "Les mots de passe ne correspondent pas"
        if (role == "ADMIN_GROUPE" && selectedGroupId.isBlank()) return "Le groupe scolaire est requis pour un admin groupe"
        return null
    }

    AdminDialogWindow(
        title = "Nouvel administrateur",
        subtitle = "Créer un compte admin groupe ou super admin",
        onDismiss = onDismiss,
        size = DpSize(780.dp, 680.dp),
        content = {
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Identité ──────────────────────────────────────
                AdminSectionCard(icon = Icons.Default.Person, title = "Identité") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = prenom, onValueChange = { prenom = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Prénom") },
                            singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = nom, onValueChange = { nom = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Nom") },
                            singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = email, onValueChange = { email = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Email") },
                            singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            leadingIcon = { Icon(Icons.Default.Email, null) }
                        )
                        OutlinedTextField(
                            value = phone, onValueChange = { phone = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Téléphone") },
                            singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            placeholder = { Text("+242 …") },
                            leadingIcon = { Icon(Icons.Default.Phone, null) }
                        )
                    }
                    GenderSelector(selectedGender = gender, onGenderSelected = { gender = it })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DateField(value = dateOfBirth, onValueChange = { dateOfBirth = it }, label = "Date de naissance", modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = birthPlace, onValueChange = { birthPlace = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Lieu de naissance") },
                            singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                        )
                    }
                    OutlinedTextField(
                        value = address, onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Adresse") },
                        singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                    )
                }

                // ── Rôle & Affectation ────────────────────────────
                AdminSectionCard(icon = Icons.Default.School, title = "Rôle & Affectation") {
                    RoleSelector(selectedRole = role, onRoleSelected = { role = it })
                    if (role == "ADMIN_GROUPE") {
                        GroupSelectorField(
                            value = selectedGroupId,
                            label = "Groupe scolaire",
                            placeholder = "Sélectionnez un groupe",
                            expanded = showGroupMenu,
                            enabled = true,
                            options = groupOptions,
                            onExpandedChange = { showGroupMenu = it },
                            onOptionSelected = { selectedGroupId = it }
                        )
                    }
                }

                // ── Sécurité ──────────────────────────────────────
                AdminSectionCard(icon = Icons.Default.Lock, title = "Sécurité") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = password, onValueChange = { password = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Mot de passe") },
                            singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            visualTransformation = PasswordVisualTransformation()
                        )
                        OutlinedTextField(
                            value = passwordConfirm, onValueChange = { passwordConfirm = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Confirmer le mot de passe") },
                            singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }
                    MustChangePasswordSwitch(checked = mustChangePassword, onCheckedChange = { mustChangePassword = it })
                }

                // ── Avatar ─────────────────────────────────────────
                AdminSectionCard(icon = Icons.Default.Person, title = "Photo de profil") {
                    AdminAvatarPicker(
                        avatarData = avatarData,
                        avatarFileName = avatarFileName,
                        errorMessage = avatarError,
                        onAvatarSelected = {
                            val result = pickLogoSelection()
                            when (result) {
                                is LogoSelectionResult.Selected -> {
                                    avatarData = result.dataUrl
                                    avatarFileName = result.fileName
                                    avatarError = null
                                }
                                is LogoSelectionResult.Failure -> avatarError = result.message
                                LogoSelectionResult.Cancelled -> {}
                            }
                        },
                        onAvatarCleared = {
                            avatarData = null
                            avatarFileName = null
                            avatarError = null
                        }
                    )
                }

                errorMessage?.let {
                    AdminFeedbackBanner(
                        feedback = AdminFeedbackMessage(it, isError = true),
                        onDismiss = {}
                    )
                }
            }
        },
        actions = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting, modifier = Modifier.cursorHand()) {
                Text("Annuler")
            }
            Row(modifier = Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        val err = validate()
                        if (err != null) {
                            // Show inline — handled by errorMessage from parent
                            return@Button
                        }
                        onCreate(
                            CreateAdminUserDto(
                                role = role,
                                password = password,
                                nom = nom,
                                prenom = prenom,
                                email = email,
                                phone = phone.ifBlank { null },
                                gender = gender,
                                dateOfBirth = dateOfBirth,
                                address = address.ifBlank { null },
                                birthPlace = birthPlace.ifBlank { null },
                                avatar = avatarData,
                                groupId = if (role == "ADMIN_GROUPE") selectedGroupId else null,
                                mustChangePassword = mustChangePassword
                            )
                        )
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D3557)),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp).width(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Row(modifier = Modifier.padding(start = 8.dp)) { Text("Création…") }
                    } else {
                        Text("Créer l'administrateur")
                    }
                }
            }
        }
    )
}
