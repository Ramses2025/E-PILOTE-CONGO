package cg.epilote.desktop.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Line Chart (full-featured, multi-series) ──────────────────────────────────

data class LineChartSeries(val entries: List<ChartEntry>, val lineColor: Color, val label: String = "")

@Composable
fun LineChart(
    series: List<LineChartSeries>,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
    showPoints: Boolean = true
) {
    if (series.isEmpty() || series.all { it.entries.isEmpty() }) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Pas assez de données", color = Color(0xFF8C9BAB), fontSize = 12.sp)
        }
        return
    }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
    )

    val allValues = series.flatMap { s -> s.entries.map { it.value } }
    val maxV = allValues.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val labels = series.first().entries.map { it.label }

    Column(modifier = modifier) {
        if (series.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                series.forEach { s ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(Modifier.size(10.dp).background(s.lineColor, CircleShape))
                        Text(s.label, fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val padding = 4f
            val chartWidth = size.width - padding * 2
            val chartHeight = size.height - padding * 2

            series.forEach { s ->
                if (s.entries.size < 2) return@forEach
                val stepX = chartWidth / (s.entries.size - 1)

                val path = Path()
                val fillPath = Path()

                s.entries.forEachIndexed { i, entry ->
                    val x = padding + i * stepX
                    val y = padding + chartHeight - (entry.value / maxV) * chartHeight * animProgress
                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, padding + chartHeight)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo(padding + (s.entries.size - 1) * stepX, padding + chartHeight)
                fillPath.close()

                drawPath(fillPath, s.lineColor.copy(alpha = 0.08f))
                drawPath(path, s.lineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

                if (showPoints) {
                    s.entries.forEachIndexed { i, entry ->
                        val x = padding + i * stepX
                        val y = padding + chartHeight - (entry.value / maxV) * chartHeight * animProgress
                        drawCircle(Color.White, radius = 5f, center = Offset(x, y))
                        drawCircle(s.lineColor, radius = 3.5f, center = Offset(x, y))
                    }
                }
            }
        }

        if (showLabels && labels.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        label,
                        fontSize = 9.sp,
                        color = Color(0xFF8C9BAB),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── Mini Sparkline ────────────────────────────────────────────────────────────

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
