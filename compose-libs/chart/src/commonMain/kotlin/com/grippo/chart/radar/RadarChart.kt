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
    style: RadarStyle,
) {
    val measurer = rememberTextMeasurer()

    androidx.compose.foundation.Canvas(modifier) {
        val axes = data.axes
        val n = axes.size
        if (n == 0) return@Canvas

        // Bounds & center (no external padding). Compute radius so labels fit inside.
        val labelPad = style.layout.labelPadding.toPx()
        val chart = Rect(0f, 0f, size.width, size.height)
        val c = Offset(chart.left + chart.width / 2f, chart.top + chart.height / 2f)

        val dir = if (style.layout.clockwise) 1f else -1f
        val angleStep = (2 * PI.toFloat() / n) * dir
        val startRad = style.layout.startAngleDeg * PI.toFloat() / 180f

        fun polar(angle: Float, radius: Float) = Offset(
            x = c.x + radius * cos(angle),
            y = c.y + radius * sin(angle)
        )

        // Measure axis labels to compute max radial outward space needed
        val labelLayouts = when (val lbl = style.labels) {
            is RadarStyle.Labels.Visible -> axes.map {
                measurer.measure(
                    AnnotatedString(it.label),
                    lbl.textStyle
                )
            }

            is RadarStyle.Labels.None -> emptyList()
        }

        fun radialInsetsForLabels(radius: Float): Float {
            if (labelLayouts.isEmpty()) return 0f
            var maxOverflow = 0f
            for (i in 0 until n) {
                val a = startRad + i * angleStep
                val p = Offset(
                    x = c.x + (radius + labelPad) * cos(a),
                    y = c.y + (radius + labelPad) * sin(a)
                )
                val layout = labelLayouts[i]
                val left = p.x - layout.size.width / 2f
                val right = p.x + layout.size.width / 2f
                val top = p.y - layout.size.height / 2f
                val bottom = p.y + layout.size.height / 2f
                maxOverflow = max(
                    maxOverflow,
                    max(0f, left - chart.left) * -1f
                )
                maxOverflow = max(maxOverflow, max(0f, chart.right - right) * -1f)
                maxOverflow = max(maxOverflow, max(0f, top - chart.top) * -1f)
                maxOverflow = max(maxOverflow, max(0f, chart.bottom - bottom) * -1f)
            }
            return maxOverflow
        }

        var r = min(chart.width, chart.height) / 2f * 0.95f
        // Reduce radius if labels would overflow
        if (labelLayouts.isNotEmpty()) {
            // simple iterative shrink
            var iter = 0
            while (iter < 8) {
                val overflow = radialInsetsForLabels(r)
                if (overflow <= 0f) break
                r -= overflow
                iter++
            }
            r = r.coerceAtLeast(0f)
        }

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

            when (val ll = style.grid.levelLabels) {
                is RadarStyle.LevelLabels.None -> Unit
                is RadarStyle.LevelLabels.Visible -> {
                    val label = ll.levelFormatter(t)
                    val layout = measurer.measure(AnnotatedString(label), ll.levelLabelStyle)
                    val y = (c.y - rr - layout.size.height - 4.dp.toPx()).coerceAtLeast(chart.top)
                    drawText(
                        layout,
                        topLeft = Offset(
                            x = c.x - layout.size.width / 2f,
                            y = y
                        )
                    )
                }
            }
        }

        // Spokes
        when (val sp = style.spokes) {
            is RadarStyle.Spokes.None -> Unit
            is RadarStyle.Spokes.Visible -> {
                val sw = sp.strokeWidth.toPx()
                for (i in 0 until n) {
                    val a = startRad + i * angleStep
                    val p = polar(a, r)
                    drawLine(sp.color, c, p, sw)
                }
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
            when (val vcg = style.vertices) {
                is RadarStyle.Vertices.None -> Unit
                is RadarStyle.Vertices.Visible -> {
                    val vr = vcg.radius.toPx()
                    val vc = vcg.colorOverride ?: s.color
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
            }

            // Values near vertices
            when (val v = style.values) {
                is RadarStyle.Values.None -> Unit
                is RadarStyle.Values.Visible -> {
                    val off = v.offset.toPx()
                    for (i in 0 until n) {
                        val a = startRad + i * angleStep
                        val raw: Float? = when (val vvz = s.values) {
                            is RadarValues.ByAxisId -> vvz.map[data.axes[i].id]
                            is RadarValues.ByIndex -> vvz.list.getOrNull(i)
                        }
                        if (raw == null && !style.dataPolicy.missingAsZero) continue
                        val vv = (raw ?: 0f).coerceIn(0f, 1f)
                        val p0 = polar(a, r * vv)
                        val label = v.formatter(vv, data)
                        val layout = measurer.measure(AnnotatedString(label), v.textStyle)
                        val x = (p0.x + cos(a) * off - layout.size.width / 2f)
                            .coerceIn(chart.left, chart.right - layout.size.width.toFloat())
                        val y = (p0.y + sin(a) * off - layout.size.height / 2f)
                            .coerceIn(chart.top, chart.bottom - layout.size.height.toFloat())
                        drawText(
                            layout,
                            topLeft = Offset(x, y)
                        )
                    }
                }
            }
        }

        // Axis labels
        when (val lbl = style.labels) {
            is RadarStyle.Labels.None -> Unit
            is RadarStyle.Labels.Visible -> {
                val lp = labelPad
                for (i in 0 until n) {
                    val a = startRad + i * angleStep
                    val p = polar(a, r + lp)
                    val layout = measurer.measure(AnnotatedString(axes[i].label), lbl.textStyle)
                    val x = (p.x - layout.size.width / 2f).coerceIn(
                        chart.left,
                        chart.right - layout.size.width.toFloat()
                    )
                    val y = (p.y - layout.size.height / 2f).coerceIn(
                        chart.top,
                        chart.bottom - layout.size.height.toFloat()
                    )
                    drawText(
                        layout,
                        topLeft = Offset(x, y)
                    )
                }
            }
        }
    }
}