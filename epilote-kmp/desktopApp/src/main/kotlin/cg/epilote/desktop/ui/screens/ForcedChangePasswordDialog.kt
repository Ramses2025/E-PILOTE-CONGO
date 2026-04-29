package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cg.epilote.desktop.data.ChangePasswordRequestDto
import cg.epilote.desktop.data.DesktopAdminClient
import cg.epilote.desktop.ui.theme.EpiloteGreen
import cg.epilote.desktop.ui.theme.EpiloteTextMuted
import kotlinx.coroutines.launch

/**
 * Dialogue forcé de changement de mot de passe affiché à la première connexion
 * lorsque le backend renvoie `mustChangePassword=true` sur la `LoginResponse`.
 *
 * Politique de mot de passe (alignée avec [AuthService.changePassword] côté
 * backend) :
 * - 8 caractères au moins ;
 * - différent du mot de passe courant.
 *
 * Le dialogue est non-fermable (sans `onDismissRequest`) et capture l'entrée
 * tant que l'opération n'a pas réussi. À l'issue d'un succès,
 * [onPasswordChanged] propage le nouveau hash applicatif (le flag
 * `mustChangePassword` est désormais à `false` côté serveur, l'appelant doit
 * mettre à jour la session locale en conséquence).
 *
 * Référence Compose Multiplatform Dialog :
 * https://developer.android.com/jetpack/compose/components/dialog
 */
@Composable
fun ForcedChangePasswordDialog(
    adminClient: DesktopAdminClient,
    onPasswordChanged: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = { /* non-fermable : changement obligatoire */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.width(480.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = EpiloteGreen
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Changement de mot de passe requis",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D3557)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Pour la sécurité de votre compte, vous devez définir un nouveau mot de passe avant d'accéder à l'application.",
                    fontSize = 12.sp,
                    color = EpiloteTextMuted
                )
                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; error = null },
                    label = { Text("Mot de passe actuel") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; error = null },
                    label = { Text("Nouveau mot de passe (8 caractères minimum)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    label = { Text("Confirmer le nouveau mot de passe") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading
                )

                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error ?: "",
                            color = Color(0xFFB3261E),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Button(
                        onClick = {
                            // Validations locales (alignées avec backend AuthService.changePassword)
                            when {
                                currentPassword.isBlank() ->
                                    error = "Mot de passe actuel requis"
                                newPassword.length < 8 ->
                                    error = "Le nouveau mot de passe doit contenir au moins 8 caractères"
                                newPassword == currentPassword ->
                                    error = "Le nouveau mot de passe doit être différent de l'actuel"
                                newPassword != confirmPassword ->
                                    error = "La confirmation ne correspond pas au nouveau mot de passe"
                                else -> {
                                    loading = true
                                    error = null
                                    scope.launch {
                                        val result = runCatching {
                                            adminClient.changePassword(
                                                ChangePasswordRequestDto(
                                                    currentPassword = currentPassword,
                                                    newPassword = newPassword,
                                                    targetUserId = null
                                                )
                                            )
                                        }
                                        loading = false
                                        result.onSuccess { resp ->
                                            if (resp != null) {
                                                onPasswordChanged()
                                            } else {
                                                error = "Échec du changement de mot de passe (réponse vide). Réessayez."
                                            }
                                        }.onFailure { ex ->
                                            error = ex.message?.takeIf { it.isNotBlank() }
                                                ?: "Échec du changement de mot de passe"
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !loading,
                        colors = ButtonDefaults.buttonColors(containerColor = EpiloteGreen)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.height(18.dp).width(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Changer le mot de passe")
                    }
                }
            }
        }
    }
}
