package cg.epilote.desktop.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.skia.Image as SkiaImage
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cg.epilote.desktop.ui.theme.*
import cg.epilote.shared.domain.model.UserSession
import cg.epilote.shared.presentation.viewmodel.LoginUiState
import cg.epilote.shared.presentation.viewmodel.LoginViewModel
import java.awt.Cursor

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (UserSession) -> Unit
) {
    val uiState  by viewModel.uiState.collectAsState()
    val email    by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe      by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val logoPainter = remember {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("logo.svg")
        stream?.let { loadSvgPainter(it, density) }
    }
    val bgBitmap: ImageBitmap? = remember {
        runCatching {
            val bytes = Thread.currentThread().contextClassLoader
                .getResourceAsStream("bk.webp")!!.readBytes()
            SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
        }.getOrNull()
    }

    val navy   = Color(0xFF1D3557)
    val accent = Color(0xFF2A9D8F)
    val border = Color(0xFFE0E4EA)
    val label  = Color(0xFF6B7A8D)

    // ── Entrance animation ──────────────────────────────────────
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    // ── Subtle shimmer on brand line ─────────────────────────────
    val fx = rememberInfiniteTransition(label = "fx")
    val shimmerPhase by fx.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing)
        ), label = "shimmer"
    )

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess((uiState as LoginUiState.Success).session)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Background image ──────────────────────────────────────
        if (bgBitmap != null) {
            Image(
                bitmap = bgBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        // ── Dark overlay for readability ──────────────────────────
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )
        val cardW: Dp = when {
            maxWidth < 500.dp  -> maxWidth * 0.92f
            maxWidth < 800.dp  -> maxWidth * 0.48f
            maxWidth < 1200.dp -> 380.dp
            else               -> 400.dp
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.25f))

            // ── Card (branding intégré) ─────────────────────────
            Surface(
                modifier = Modifier.width(cardW).alpha(contentAlpha),
                shape = RoundedCornerShape(4.dp),
                color = Color.White.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, border),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // ── Branding inside card ────────────────────
                    if (logoPainter != null) {
                        Image(logoPainter, "Logo E-PILOTE", Modifier.size(44.dp))
                        Spacer(Modifier.height(10.dp))
                    }
                    Text(
                        "E-PILOTE CONGO",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = navy, letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .width(36.dp).height(2.dp)
                            .drawBehind {
                                drawRect(color = Color(0xFF2A9D8F))
                                val sw = size.width * 0.4f
                                val sx = shimmerPhase * (size.width + sw) - sw
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.7f),
                                            Color.Transparent
                                        ),
                                        startX = sx,
                                        endX = sx + sw
                                    )
                                )
                            }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Plateforme de Gestion Scolaire \u2022 République du Congo",
                        fontSize = 9.sp, color = label
                    )

                    Spacer(Modifier.height(28.dp))

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = viewModel::onEmailChange,
                        label = { Text("Adresse email", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = border,
                            focusedBorderColor = navy,
                            unfocusedLabelColor = label,
                            focusedLabelColor = navy
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = viewModel::onPasswordChange,
                        label = { Text("Mot de passe", fontSize = 13.sp) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    null, tint = label, modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = border,
                            focusedBorderColor = navy,
                            unfocusedLabelColor = label,
                            focusedLabelColor = navy
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // Se souvenir + Mot de passe oublié
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.offset(x = (-12).dp)
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = navy)
                            )
                            Text("Se souvenir de moi", fontSize = 12.sp, color = label)
                        }
                        TextButton(
                            onClick = {},
                            modifier = Modifier.pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR))),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "Mot de passe oublié ?",
                                fontSize = 12.sp, color = navy, fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Error
                    AnimatedVisibility(
                        visible = uiState is LoginUiState.Error,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit  = fadeOut(animationSpec = tween(200))
                    ) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                Text(
                                    (uiState as? LoginUiState.Error)?.message ?: "",
                                    color = Color(0xFFDC2626), fontSize = 12.sp
                                )
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = uiState is LoginUiState.NoNetwork,
                        enter = fadeIn(animationSpec = tween(300)),
                        exit  = fadeOut(animationSpec = tween(200))
                    ) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFFFFF3CD),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.WifiOff, null, tint = Color(0xFF856404), modifier = Modifier.size(16.dp))
                                Text(
                                    "Connexion réseau requise",
                                    color = Color(0xFF856404), fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Button
                    Button(
                        onClick = { viewModel.login() },
                        enabled = uiState !is LoginUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR))),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = navy),
                        elevation = ButtonDefaults.buttonElevation(2.dp, 4.dp, 0.dp),
                        contentPadding = PaddingValues(vertical = 0.dp)
                    ) {
                        if (uiState is LoginUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White, strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Ouvrir une session", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(0.38f))

            // ── Footer ──────────────────────────────────────────
            Text(
                "\u00A9 2026 E-PILOTE CONGO \u2022 République du Congo",
                modifier = Modifier.padding(bottom = 16.dp),
                fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
