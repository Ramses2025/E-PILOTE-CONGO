package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.data.UpdateAdminUserDto
import cg.epilote.desktop.ui.theme.cursorHand
import cg.epilote.desktop.ui.screens.LogoSelectionResult

@Composable
fun EditAdminDialog(
    admin: AdminUserDto,
    groupes: List<GroupeDto>,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onUpdate: (String, UpdateAdminUserDto, (Boolean, String?) -> Unit) -> Unit
) {
    var prenom by remember { mutableStateOf(admin.firstName) }
    var nom by remember { mutableStateOf(admin.lastName) }
    var email by remember { mutableStateOf(admin.email) }
    var phone by remember { mutableStateOf(admin.phone ?: "") }
    var gender by remember { mutableStateOf(admin.gender) }
    var dateOfBirth by remember { mutableStateOf(admin.dateOfBirth) }
    var address by remember { mutableStateOf(admin.address ?: "") }
    var birthPlace by remember { mutableStateOf(admin.birthPlace ?: "") }
    var mustChangePassword by remember { mutableStateOf(admin.mustChangePassword) }
    var avatarData by remember { mutableStateOf(admin.avatar) }
    var avatarFileName by remember { mutableStateOf<String?>(null) }
    var avatarError by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    AdminDialogWindow(
        title = "Modifier l'administrateur",
        subtitle = "${admin.firstName} ${admin.lastName} — ${admin.role}",
        onDismiss = onDismiss,
        size = DpSize(720.dp, 640.dp),
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
                            singleLine = true, shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = nom, onValueChange = { nom = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Nom") },
                            singleLine = true, shape = RoundedCornerShape(14.dp)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = email, onValueChange = { email = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Email") },
                            singleLine = true, shape = RoundedCornerShape(14.dp),
                            leadingIcon = { Icon(Icons.Default.Email, null) }
                        )
                        OutlinedTextField(
                            value = phone, onValueChange = { phone = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Téléphone") },
                            singleLine = true, shape = RoundedCornerShape(14.dp),
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
                            singleLine = true, shape = RoundedCornerShape(14.dp)
                        )
                    }
                    OutlinedTextField(
                        value = address, onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Adresse") },
                        singleLine = true, shape = RoundedCornerShape(14.dp)
                    )
                }

                // ── Sécurité ──────────────────────────────────────
                AdminSectionCard(icon = Icons.Default.Lock, title = "Sécurité") {
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

                // ── Infos lecture seule ────────────────────────────
                AdminSectionCard(icon = Icons.Default.School, title = "Affectation (lecture seule)") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rôle", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(admin.role, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D3557))
                        }
                        if (admin.groupId != null) {
                            val groupName = groupes.find { it.id == admin.groupId }?.nom ?: admin.groupId
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Groupe scolaire", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text(groupName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D3557))
                            }
                        }
                    }
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
                        onUpdate(
                            admin.id,
                            UpdateAdminUserDto(
                                nom = nom.ifBlank { null },
                                prenom = prenom.ifBlank { null },
                                email = email.ifBlank { null },
                                phone = phone.ifBlank { null },
                                gender = gender,
                                dateOfBirth = dateOfBirth,
                                address = address.ifBlank { null },
                                birthPlace = birthPlace.ifBlank { null },
                                avatar = avatarData,
                                mustChangePassword = mustChangePassword
                            )
                        ) { _, _ -> }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D3557)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.cursorHand()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp).width(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Row(modifier = Modifier.padding(start = 8.dp)) { Text("Enregistrement…") }
                    } else {
                        Text("Enregistrer")
                    }
                }
            }
        }
    )
}
