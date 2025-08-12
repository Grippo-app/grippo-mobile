package com.grippo.chart.area

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Immutable
public data class AreaPoint(
    val x: Float,
    val y: Float
)

@Composable
public fun AreaChart(
    modifier: Modifier = Modifier,
    data: List<AreaPoint>,
    style: AreaStyle = AreaStyle(),
) {
    val measurer = rememberTextMeasurer()

    androidx.compose.foundation.Canvas(modifier) {
        if (data.isEmpty()) return@Canvas

        // ----- Domain -----
        val minX = data.minOf { it.x }
        val maxX = data.maxOf { it.x }
        val rawMaxY = max(1f, data.maxOf { it.y })
        val ticksY = max(1, style.yAxisTicks)

        // Nice Y scale (1/2/5 × 10^k)
        fun niceStep(maxVal: Float, ticks: Int): Float {
            val raw = maxVal / ticks
            val exp = floor(log10(raw.toDouble())).toInt()
            val base = 10.0.pow(exp.toDouble()).toFloat()
            val mul = raw / base
            val niceMul = when {
                mul <= 1f -> 1f
                mul <= 2f -> 2f
                mul <= 5f -> 5f
                else -> 10f
            }
            return niceMul * base
        }

        val yStep = niceStep(rawMaxY, ticksY)
        val maxY = ceil(rawMaxY / yStep) * yStep
        val minY = 0f

        // ----- Layout with axis labels reservation -----
        val pad = style.padding.toPx()
        val labelPadPx = style.labelPadding.toPx()

        // Measure Y labels to reserve left gutter
        var leftGutter = pad
        if (style.showYAxis) {
            val maxLabelWidth = (0..ticksY).maxOf { i ->
                val t = style.yValueFormatter(i * yStep)
                val layout = measurer.measure(AnnotatedString(t), style.yAxisTextStyle)
                layout.size.width
            }
            leftGutter += maxLabelWidth + labelPadPx
        }

        // Measure X labels to reserve bottom gutter (if enabled)
        var bottomGutter = pad
        if (style.showXAxis && style.xLabels.isNotEmpty()) {
            val maxH = style.xLabels.maxOf { (_, text) ->
                measurer.measure(AnnotatedString(text), style.xAxisTextStyle).size.height
            }
            bottomGutter += maxH + labelPadPx
        }

        val chart = Rect(leftGutter, pad, size.width - pad, size.height - bottomGutter)

        fun mapX(x: Float): Float =
            chart.left + (x - minX) / (maxX - minX).coerceAtLeast(1e-3f) * chart.width

        fun mapY(y: Float): Float =
            chart.bottom - (y - minY) / (maxY - minY).coerceAtLeast(1e-3f) * chart.height

        // Mapped points
        val pts = data.map { Offset(mapX(it.x), mapY(it.y)) }

        // ----- Grid (horizontal only) -----
        if (style.showGrid) {
            val gw = style.gridStrokeWidth.toPx()
            for (i in 0..ticksY) {
                val yVal = i * yStep
                val y = mapY(yVal)
                drawLine(style.gridColor, Offset(chart.left, y), Offset(chart.right, y), gw)
            }
        }

        // ----- Axes -----
        if (style.showYAxis) {
            // Labels
            for (i in 0..ticksY) {
                val yVal = i * yStep
                val y = mapY(yVal)
                val text = style.yValueFormatter(yVal)
                val layout = measurer.measure(AnnotatedString(text), style.yAxisTextStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        x = chart.left - labelPadPx - layout.size.width,
                        y = y - layout.size.height / 2f
                    )
                )
                // Small tick
                drawLine(
                    color = style.axisLineColor,
                    start = Offset(chart.left - 4.dp.toPx(), y),
                    end = Offset(chart.left, y),
                    strokeWidth = style.axisLineWidth.toPx()
                )
            }
            // Axis line
            if (style.showYAxisLine) {
                drawLine(
                    color = style.axisLineColor,
                    start = Offset(chart.left, chart.top),
                    end = Offset(chart.left, chart.bottom),
                    strokeWidth = style.axisLineWidth.toPx()
                )
            }
        }

        if (style.showXAxis && style.xLabels.isNotEmpty()) {
            style.xLabels.forEach { (xVal, label) ->
                val x = mapX(xVal)
                val layout = measurer.measure(AnnotatedString(label), style.xAxisTextStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        x = x - layout.size.width / 2f,
                        y = chart.bottom + labelPadPx / 2f
                    )
                )
            }
        }

        // ----- Line path (optionally smoothed and clamped) -----
        fun buildLinePath(points: List<Offset>): Path {
            val path = Path()
            if (points.isEmpty()) return path
            if (!style.curved || points.size < 3) {
                path.moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
                return path
            }
            val s = style.curveSmoothness.coerceIn(0f, 0.5f)
            val p = points
            path.moveTo(p.first().x, p.first().y)
            for (i in 0 until p.lastIndex) {
                val p0 = if (i == 0) p[i] else p[i - 1]
                val p1 = p[i]
                val p2 = p[i + 1]
                val p3 = if (i + 2 <= p.lastIndex) p[i + 2] else p[i + 1]

                var c1 = Offset(
                    p1.x + (p2.x - p0.x) * s,
                    p1.y + (p2.y - p0.y) * s
                )
                var c2 = Offset(
                    p2.x - (p3.x - p1.x) * s,
                    p2.y - (p3.y - p1.y) * s
                )
                if (style.clampOvershoot) {
                    val minYp = min(p0.y, min(p1.y, p2.y))
                    val maxYp = max(p0.y, max(p1.y, p2.y))
                    c1 = c1.copy(y = c1.y.coerceIn(minYp, maxYp))
                    val minYn = min(p1.y, min(p2.y, p3.y))
                    val maxYn = max(p1.y, max(p2.y, p3.y))
                    c2 = c2.copy(y = c2.y.coerceIn(minYn, maxYn))
                }
                path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
            }
            return path
        }

        val linePath = buildLinePath(pts)

        // ----- Fill under line -----
        style.fill?.let { provider ->
            val area = Path().apply {
                addPath(linePath)
                lineTo(pts.last().x, chart.bottom)
                lineTo(pts.first().x, chart.bottom)
                close()
            }
            drawPath(area, brush = provider(size))
        }

        // ----- Glow (drawn beneath the main stroke) -----
        if (style.lineGlowWidth.value > 0f) {
            val glowColor = (style.lineGlowColor ?: style.lineColor).copy(alpha = 0.25f)
            drawPath(
                linePath,
                color = glowColor,
                style = Stroke(width = style.lineGlowWidth.toPx())
            )
        }

        // ----- Main stroke -----
        val stroke = Stroke(width = style.strokeWidth.toPx(), cap = StrokeCap.Round)
        val lineBrush = style.lineBrush?.invoke(size)
        if (lineBrush != null) {
            drawPath(linePath, brush = lineBrush, style = stroke)
        } else {
            drawPath(linePath, color = style.lineColor, style = stroke)
        }

        // ----- Dots -----
        if (style.showDots) {
            val r = style.dotRadius.toPx()
            val c = style.dotColor ?: style.lineColor
            pts.forEach { drawCircle(color = c, radius = r, center = it) }
        }

        // ----- Extrema labels (min & max points) -----
        if (style.showExtrema && pts.isNotEmpty()) {
            val maxIdx = data.indices.maxBy { data[it].y }
            val minIdx = data.indices.minBy { data[it].y }
            fun drawTag(idx: Int) {
                val p = pts[idx]
                val value = data[idx].y
                val label = style.yValueFormatter(value)
                val layout = measurer.measure(AnnotatedString(label), style.extremaTextStyle)
                4.dp.toPx()
                val x = (p.x - layout.size.width / 2f).coerceIn(
                    chart.left,
                    chart.right - layout.size.width
                )
                val y = (p.y - layout.size.height - 6.dp.toPx()).coerceAtLeast(chart.top)
                // small marker dot
                drawCircle(color = style.lineColor, radius = 3.dp.toPx(), center = p)
                // text tag
                drawText(layout, topLeft = Offset(x, y))
            }
            drawTag(maxIdx)
            drawTag(minIdx)
        }
    }
}
