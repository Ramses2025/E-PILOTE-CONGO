package cg.epilote.desktop.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

data class ChartEntry(val label: String, val value: Float, val color: Color)

// ── Bar Chart ──────────────────────────────────────────────────────

@Composable
fun BarChart(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier,
    showValues: Boolean = true
) {
    if (entries.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucune donnée", color = Color(0xFF8C9BAB), fontSize = 12.sp)
        }
        return
    }
    val maxValue = entries.maxOf { it.value }.coerceAtLeast(1f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            entries.forEach { entry ->
                val fraction = (entry.value / maxValue).coerceIn(0.02f, 1f)
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (showValues) {
                        Text(
                            entry.value.toLong().toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = entry.color
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .fillMaxHeight(fraction)
                            .background(entry.color, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            entries.forEach { entry ->
                Text(
                    entry.label,
                    fontSize = 9.sp,
                    color = Color(0xFF8C9BAB),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                )
            }
        }
    }
}

// ── Donut / Pie Chart (Animé) ─────────────────────────────────────

@Composable
fun DonutChart(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 32f
) {
    if (entries.isEmpty() || entries.all { it.value == 0f }) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucune donnée", color = Color(0xFF8C9BAB), fontSize = 12.sp)
        }
        return
    }
    val total = entries.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val diameter = min(size.width, size.height) - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)

            // Track gris de fond
            drawArc(
                color = Color(0xFFF1F5F9),
                startAngle = 0f, sweepAngle = 360f,
                useCenter = false, topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            var startAngle = -90f
            entries.forEach { entry ->
                val sweep = (entry.value / total) * 360f * animProgress
                drawArc(
                    color = entry.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.forEach { entry ->
                val pct = ((entry.value / total) * 100).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier.size(10.dp)
                            .background(entry.color, CircleShape)
                    )
                    Text(
                        "${entry.label} ($pct%)",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── Progress Ring (anneau de progression) ─────────────────────────

@Composable
fun ProgressRing(
    progress: Float,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0xFFF1F5F9),
    strokeWidth: Float = 10f
) {
    val animProg by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1200, easing = FastOutSlowInEasing)
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(72.dp)) {
                val diameter = min(size.width, size.height) - strokeWidth
                val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                drawArc(
                    color = trackColor, startAngle = 0f, sweepAngle = 360f,
                    useCenter = false, topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = color, startAngle = -90f,
                    sweepAngle = 360f * animProg,
                    useCenter = false, topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Text(
                value, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = color, textAlign = TextAlign.Center
            )
        }
        Text(label, fontSize = 10.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
    }
}

// ── Horizontal Bar Chart (for departments / provinces) ─────────────

@Composable
fun HorizontalBarChart(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucune donnée", color = Color(0xFF8C9BAB), fontSize = 12.sp)
        }
        return
    }
    val maxValue = entries.maxOf { it.value }.coerceAtLeast(1f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.take(10).forEach { entry ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        entry.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        entry.value.toLong().toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = entry.color
                    )
                }
                val fraction = (entry.value / maxValue).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(8.dp)
                            .background(entry.color, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

// ── Mini Sparkline ─────────────────────────────────────────────────

@Composable
fun SparkLine(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF2A9D8F),
    fillColor: Color = lineColor.copy(alpha = 0.1f)
) {
    if (values.size < 2) return
    val maxV = values.max().coerceAtLeast(1f)
    val minV = values.min()

    Canvas(modifier = modifier) {
        val stepX = size.width / (values.size - 1)
        val range = (maxV - minV).coerceAtLeast(1f)

        val path = Path()
        val fillPath = Path()

        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - minV) / range) * size.height * 0.9f
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(fillPath, fillColor)
        drawPath(path, lineColor, style = Stroke(width = 2f))
    }
}
