package cg.epilote.desktop.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.Cursor

// ── Cursor helpers ──────────────────────────────────────────────

/** Cursor en main (pointer) pour les éléments cliquables. */
val HandCursor: PointerIcon = PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))

/** Applique le curseur main au survol. */
fun Modifier.cursorHand(): Modifier = this.pointerHoverIcon(HandCursor)

// ── Hover scale animation ───────────────────────────────────────

/**
 * Applique un léger agrandissement au survol (1.0 → 1.02 par défaut).
 * Usage : Modifier.hoverScale()
 */
@Composable
fun Modifier.hoverScale(scaleUp: Float = 1.02f): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val isHovered by interaction.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isHovered) scaleUp else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    return this then Modifier
        .hoverable(interaction)
        .scale(scale)
}

// ── Animated entrance ───────────────────────────────────────────

/**
 * Animation d'entrée fadeIn + slideUp pour les cartes/listes.
 * Usage : AnimatedCardEntrance(index = idx) { GroupeCard(...) }
 */
@Composable
fun AnimatedCardEntrance(
    index: Int,
    staggerMs: Int = 60,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * staggerMs).toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            animationSpec = tween(300, delayMillis = 0),
            initialOffsetY = { it / 8 }
        )
    ) {
        content()
    }
}

// ── Pulsing loading indicator ───────────────────────────────────

@Composable
fun PulsingLoadingBar(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    LinearProgressIndicator(
        modifier = modifier.fillMaxWidth().height(3.dp),
        color = EpiloteGreen.copy(alpha = pulseAlpha),
        trackColor = EpiloteGreen.copy(alpha = 0.12f)
    )
}

// ── Shimmer placeholder ─────────────────────────────────────────

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = androidx.compose.ui.graphics.Color(0xFFE2E8F0).copy(alpha = 0.5f + shimmerOffset * 0.3f)
    ) {}
}
