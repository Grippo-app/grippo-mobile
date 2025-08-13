package com.grippo.chart.radar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
public fun RadarChart(
    modifier: Modifier = Modifier,
    data: RadarData,
    style: RadarStyle = RadarStyle(),
) {
    val measurer = rememberTextMeasurer()

    androidx.compose.foundation.Canvas(modifier) {
        val axes = data.axes
        val n = axes.size
        if (n == 0) return@Canvas

        // Bounds & center
        val pad = style.layout.padding.toPx()
        val chart = Rect(pad, pad, size.width - pad, size.height - pad)
        val r = min(chart.width, chart.height) / 2f * 0.95f
        val c = Offset(chart.left + chart.width / 2f, chart.top + chart.height / 2f)

        val dir = if (style.layout.clockwise) 1f else -1f
        val angleStep = (2 * PI.toFloat() / n) * dir
        val startRad = style.layout.startAngleDeg * PI.toFloat() / 180f

        fun polar(angle: Float, radius: Float) = Offset(
            x = c.x + radius * cos(angle),
            y = c.y + radius * sin(angle)
        )

        // Grid rings
        val gridStroke = Stroke(width = style.grid.strokeWidth.toPx())
        val levels = max(1, style.grid.levels)
        repeat(levels) { lvl ->
            val t = (lvl + 1) / levels.toFloat()
            val rr = r * t
            if (style.grid.asPolygon) {
                val path = Path()
                for (i in 0 until n) {
                    val a = startRad + i * angleStep
                    val p = polar(a, rr)
                    if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
                path.close()
                drawPath(path, color = style.grid.color, style = gridStroke)
            } else {
                drawCircle(color = style.grid.color, radius = rr, center = c, style = gridStroke)
            }

            if (style.grid.showLevelLabels) {
                val label = style.grid.levelFormatter(t)
                val layout = measurer.measure(AnnotatedString(label), style.grid.levelLabelStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        x = c.x - layout.size.width / 2f,
                        y = c.y - rr - layout.size.height - 4.dp.toPx()
                    )
                )
            }
        }

        // Spokes
        if (style.spokes.show) {
            val sw = style.spokes.strokeWidth.toPx()
            for (i in 0 until n) {
                val a = startRad + i * angleStep
                val p = polar(a, r)
                drawLine(style.spokes.color, c, p, sw)
            }
        }

        // Series polygons
        val strokeW = style.polygon.strokeWidth.toPx()
        data.series.forEach { s ->
            // Missing/complete checks when using ByAxisId
            val missingId = when (val v = s.values) {
                is RadarValues.ByAxisId -> data.axes.any { it.id !in v.map }
                else -> false
            }
            if (style.dataPolicy.requireCompleteSeries && missingId) return@forEach

            val path = Path()
            var firstPlotted = false
            for (i in 0 until n) {
                val a = startRad + i * angleStep
                val raw: Float? = when (val v = s.values) {
                    is RadarValues.ByAxisId -> v.map[data.axes[i].id]
                    is RadarValues.ByIndex -> v.list.getOrNull(i)
                }
                if (raw == null && !style.dataPolicy.missingAsZero) continue
                val vv = (raw ?: 0f).coerceIn(0f, 1f)
                val p = polar(a, r * vv)
                if (!firstPlotted) {
                    path.moveTo(p.x, p.y); firstPlotted = true
                } else path.lineTo(p.x, p.y)
            }
            if (!firstPlotted) return@forEach
            path.close()

            // Fill using series color with alpha
            drawPath(path, color = s.color.copy(alpha = style.polygon.fillAlpha))
            // Stroke
            drawPath(path, color = s.color, style = Stroke(width = strokeW))

            // Vertices
            if (style.vertices.show) {
                val vr = style.vertices.radius.toPx()
                val vc = style.vertices.colorOverride ?: s.color
                for (i in 0 until n) {
                    val a = startRad + i * angleStep
                    val raw: Float? = when (val v = s.values) {
                        is RadarValues.ByAxisId -> v.map[data.axes[i].id]
                        is RadarValues.ByIndex -> v.list.getOrNull(i)
                    }
                    if (raw == null && !style.dataPolicy.missingAsZero) continue
                    val vv = (raw ?: 0f).coerceIn(0f, 1f)
                    val p = polar(a, r * vv)
                    drawCircle(color = vc, radius = vr, center = p)
                }
            }

            // Values near vertices
            if (style.values.show) {
                val off = style.values.offset.toPx()
                for (i in 0 until n) {
                    val a = startRad + i * angleStep
                    val raw: Float? = when (val v = s.values) {
                        is RadarValues.ByAxisId -> v.map[data.axes[i].id]
                        is RadarValues.ByIndex -> v.list.getOrNull(i)
                    }
                    if (raw == null && !style.dataPolicy.missingAsZero) continue
                    val vv = (raw ?: 0f).coerceIn(0f, 1f)
                    val p = polar(a, r * vv)
                    val label = style.values.formatter(vv, data)
                    val layout = measurer.measure(AnnotatedString(label), style.values.textStyle)
                    val x = p.x + cos(a) * off
                    val y = p.y + sin(a) * off
                    drawText(
                        layout,
                        topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f)
                    )
                }
            }
        }

        // Axis labels
        if (style.labels.show) {
            val lp = style.layout.labelPadding.toPx()
            for (i in 0 until n) {
                val a = startRad + i * angleStep
                val p = polar(a, r + lp)
                val layout =
                    measurer.measure(AnnotatedString(axes[i].label), style.labels.textStyle)
                drawText(
                    layout,
                    topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f)
                )
            }
        }
    }
}