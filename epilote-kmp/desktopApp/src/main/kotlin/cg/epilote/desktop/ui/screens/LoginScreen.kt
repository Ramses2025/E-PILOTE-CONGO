package cg.epilote.desktop.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.*
import cg.epilote.shared.domain.model.UserSession
import cg.epilote.shared.presentation.viewmodel.LoginUiState
import cg.epilote.shared.presentation.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (UserSession) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val email by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val logoPainter = remember {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("logo.svg")
        stream?.let { loadSvgPainter(it, density) }
    }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess((uiState as LoginUiState.Success).session)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF7F8FA))) {

        // ── Header bar — logo + nom plateforme + langue ──────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 2.dp,
            color = Color(0xFF1D3557)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (logoPainter != null) {
                        Image(
                            painter = logoPainter,
                            contentDescription = "Logo",
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Column {
                        Text(
                            "E-PILOTE CONGO",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Plateforme de Gestion Scolaire",
                            color = Color(0xFFDCE3EA),
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = Color(0xFFE9C46A),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Français",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ── Zone principale — formulaire centré ──────────────────
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.width(420.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        "Connexion",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF172B4D)
                    )
                    Text(
                        "Entrez vos identifiants pour continuer",
                        fontSize = 13.sp,
                        color = EpiloteTextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(4.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = viewModel::onUsernameChange,
                        placeholder = { Text("Email", color = Color(0xFFB0B8C4)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFDFE3E8),
                            focusedBorderColor = Color(0xFF1D3557)
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = viewModel::onPasswordChange,
                        placeholder = { Text("Mot de passe", color = Color(0xFFB0B8C4)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null,
                                    tint = Color(0xFFB0B8C4)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFDFE3E8),
                            focusedBorderColor = Color(0xFF1D3557)
                        )
                    )

                    // ── Remember me + Forgot password ────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1D3557))
                            )
                            Text("Se souvenir de moi", fontSize = 12.sp, color = Color(0xFF6B7280))
                        }
                        TextButton(onClick = { /* TODO */ }) {
                            Text(
                                "Mot de passe oublié ?",
                                fontSize = 12.sp,
                                color = Color(0xFF1D3557),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (uiState is LoginUiState.Error) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                (uiState as LoginUiState.Error).message,
                                modifier = Modifier.padding(12.dp),
                                color = Color(0xFFDC2626),
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (uiState is LoginUiState.NoNetwork) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF3CD),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Mode hors-ligne : connexion réseau requise pour la première connexion",
                                modifier = Modifier.padding(12.dp),
                                color = Color(0xFF856404),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.login() },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        enabled = uiState !is LoginUiState.Loading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D3557))
                    ) {
                        if (uiState is LoginUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Se connecter", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
