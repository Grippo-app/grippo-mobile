package com.grippo.chart.radar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import kotlin.math.min
import kotlin.math.sin

@Immutable
public data class RadarAxis(val label: String, val value01: Float)

@Composable
public fun RadarChart(
    modifier: Modifier = Modifier,
    axes: List<RadarAxis>,
    style: RadarStyle = RadarStyle(),
) {
    val measurer = rememberTextMeasurer()

    androidx.compose.foundation.Canvas(modifier) {
        if (axes.isEmpty()) return@Canvas

        // bounds & center
        val pad = style.padding.toPx()
        val chart = Rect(pad, pad, size.width - pad, size.height - pad)
        val r = min(chart.width, chart.height) / 2f * 0.95f
        val c = Offset(chart.left + chart.width / 2f, chart.top + chart.height / 2f)

        val n = axes.size
        val dir = if (style.clockwise) 1f else -1f
        val angleStep = (2 * PI / n).toFloat() * dir
        val startRad = style.startAngleDeg * PI.toFloat() / 180f

        fun polar(angle: Float, radius: Float) = Offset(
            x = c.x + radius * cos(angle),
            y = c.y + radius * sin(angle)
        )

        // grid rings
        val gridStroke = Stroke(width = style.gridStrokeWidth.toPx())
        repeat(style.levels) { lvl ->
            val t = (lvl + 1) / style.levels.toFloat()
            val rr = r * t
            if (style.gridAsPolygon) {
                val path = Path()
                for (i in 0 until n) {
                    val a = startRad + i * angleStep
                    val p = polar(a, rr)
                    if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
                path.close()
                drawPath(path, color = style.gridColor, style = gridStroke)
            } else {
                // circle
                drawCircle(color = style.gridColor, radius = rr, center = c, style = gridStroke)
            }

            if (style.showLevelLabels) {
                val label = style.levelFormatter(t)
                val layout = measurer.measure(AnnotatedString(label), style.levelLabelStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        x = c.x - layout.size.width / 2f,
                        y = c.y - rr - layout.size.height - 4.dp.toPx()
                    )
                )
            }
        }

        // spokes
        if (style.showSpokes) {
            val spokeStroke = Stroke(width = style.spokeStrokeWidth.toPx())
            for (i in 0 until n) {
                val a = startRad + i * angleStep
                val p = polar(a, r)
                drawLine(style.spokeColor, c, p, spokeStroke.width)
            }
        }

        // data polygon
        val poly = Path()
        axes.forEachIndexed { i, ax ->
            val a = startRad + i * angleStep
            val rr = r * ax.value01.coerceIn(0f, 1f)
            val p = polar(a, rr)
            if (i == 0) poly.moveTo(p.x, p.y) else poly.lineTo(p.x, p.y)
        }
        poly.close()

        style.fillBrush?.let { drawPath(poly, brush = it) }
        drawPath(poly, color = style.strokeColor, style = Stroke(width = style.strokeWidth.toPx()))

        // vertices
        if (style.showVertexDots) {
            val vr = style.vertexDotRadius.toPx()
            val vColor = style.vertexDotColor ?: style.strokeColor
            axes.forEachIndexed { i, ax ->
                val a = startRad + i * angleStep
                val rr = r * ax.value01.coerceIn(0f, 1f)
                val p = polar(a, rr)
                drawCircle(color = vColor, radius = vr, center = p)
            }
        }

        // axis labels
        if (style.showAxisLabels) {
            val lp = style.labelPadding.toPx()
            axes.forEachIndexed { i, ax ->
                val a = startRad + i * angleStep
                val p = polar(a, r + lp)
                val layout = measurer.measure(AnnotatedString(ax.label), style.labelStyle)
                drawText(
                    layout,
                    topLeft = Offset(p.x - layout.size.width / 2f, p.y - layout.size.height / 2f)
                )
            }
        }

        // vertex values
        if (style.showVertexValues) {
            axes.forEachIndexed { i, ax ->
                val a = startRad + i * angleStep
                val rr = r * ax.value01.coerceIn(0f, 1f)
                val p = polar(a, rr)
                val label = style.vertexValueFormatter(ax.value01.coerceIn(0f, 1f))
                val layout = measurer.measure(AnnotatedString(label), style.vertexValueStyle)
                val offset = 8.dp.toPx()
                val x = p.x + cos(a) * offset
                val y = p.y + sin(a) * offset
                drawText(
                    layout,
                    topLeft = Offset(x - layout.size.width / 2f, y - layout.size.height / 2f)
                )
            }
        }
    }
}