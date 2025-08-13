package com.grippo.chart.area

import androidx.compose.runtime.Composable
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

@Composable
public fun AreaChart(
    modifier: Modifier = Modifier,
    data: AreaData,
    style: AreaStyle,
) {
    val measurer = rememberTextMeasurer()

    androidx.compose.foundation.Canvas(modifier) {
        if (data.points.isEmpty()) return@Canvas

        // ----- Domain -----
        val minX = data.points.minOf { it.x }
        val maxX = data.points.maxOf { it.x }

        val dataMinY = data.points.minOf { it.y }
        val dataMaxY = data.points.maxOf { it.y }

        // Expand to include zero and snap to nice ticks
        val ticksY = max(1, style.yAxis.ticks)
        fun niceStep(span: Float, ticks: Int): Float {
            val raw = (span / ticks).coerceAtLeast(1e-6f)
            val exp = floor(log10(raw.toDouble())).toInt()
            val base = 10.0.pow(exp.toDouble()).toFloat()
            val mul = (raw / base)
            val niceMul = when {
                mul <= 1f -> 1f
                mul <= 2f -> 2f
                mul <= 5f -> 5f
                else -> 10f
            }
            return niceMul * base
        }

        val yMinTarget = min(0f, dataMinY)
        val yMaxTarget = max(0f, dataMaxY)
        val spanRaw = (yMaxTarget - yMinTarget).coerceAtLeast(1e-6f)
        val yStep = niceStep(spanRaw, ticksY)
        val minY = floor(yMinTarget / yStep) * yStep
        val maxY = ceil(yMaxTarget / yStep) * yStep
        val spanY = (maxY - minY).coerceAtLeast(1e-6f)

        // ----- Layout with axis labels reservation -----
        val pad = style.layout.padding.toPx()
        val labelPadPx = style.layout.labelPadding.toPx()

        var leftGutter = pad
        if (style.yAxis.show) {
            val maxLabelWidth = (0..ticksY).maxOf { i ->
                val t = style.yAxis.formatter(minY + i * yStep, data)
                val layout = measurer.measure(AnnotatedString(t), style.yAxis.textStyle)
                layout.size.width
            }
            leftGutter += maxLabelWidth + labelPadPx
        }

        var bottomGutter = pad
        if (style.xAxis.show && data.xLabels.isNotEmpty()) {
            val maxH = data.xLabels.maxOf { (_, text) ->
                measurer.measure(AnnotatedString(text), style.xAxis.textStyle).size.height
            }
            bottomGutter += maxH + labelPadPx
        }

        val chart = Rect(leftGutter, pad, size.width - pad, size.height - bottomGutter)
        val chartW = chart.width.coerceAtLeast(0f)
        val chartH = chart.height.coerceAtLeast(0f)
        if (chartW <= 0f || chartH <= 0f) return@Canvas

        fun mapX(x: Float): Float =
            chart.left + (x - minX) / (maxX - minX).coerceAtLeast(1e-3f) * chartW

        fun mapY(y: Float): Float =
            chart.bottom - (y - minY) / spanY * chartH

        // Mapped points
        val pts = data.points.map { Offset(mapX(it.x), mapY(it.y)) }

        // ----- Grid (horizontal only) -----
        if (style.grid.show) {
            val gw = style.grid.strokeWidth.toPx()
            for (i in 0..ticksY) {
                val yVal = minY + i * yStep
                val y = mapY(yVal)
                drawLine(style.grid.color, Offset(chart.left, y), Offset(chart.right, y), gw)
            }
        }

        // ----- Y axis -----
        if (style.yAxis.show) {
            for (i in 0..ticksY) {
                val yVal = minY + i * yStep
                val y = mapY(yVal)
                val text = style.yAxis.formatter(yVal, data)
                val layout = measurer.measure(AnnotatedString(text), style.yAxis.textStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        x = chart.left - labelPadPx - layout.size.width,
                        y = y - layout.size.height / 2f
                    )
                )
                // tick
                drawLine(
                    color = style.yAxis.axisLineColor,
                    start = Offset(chart.left - 4.dp.toPx(), y),
                    end = Offset(chart.left, y),
                    strokeWidth = style.yAxis.axisLineWidth.toPx()
                )
            }
            if (style.yAxis.showLine) {
                drawLine(
                    color = style.yAxis.axisLineColor,
                    start = Offset(chart.left, chart.top),
                    end = Offset(chart.left, chart.bottom),
                    strokeWidth = style.yAxis.axisLineWidth.toPx()
                )
            }
        }

        // ----- X axis labels -----
        if (style.xAxis.show && data.xLabels.isNotEmpty()) {
            data.xLabels.forEach { (xVal, label) ->
                val x = mapX(xVal)
                val layout = measurer.measure(AnnotatedString(label), style.xAxis.textStyle)
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
            if (!style.line.curved || points.size < 3) {
                path.moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
                return path
            }
            val s = style.line.curveSmoothness.coerceIn(0f, 0.5f)
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
                if (style.line.clampOvershoot) {
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
            drawPath(area, brush = provider.brushProvider(size))
        }

        // ----- Glow (beneath main stroke) -----
        if (style.glow.width.value > 0f) {
            val gc = (style.glow.color ?: style.line.color).copy(alpha = 0.25f)
            drawPath(
                linePath,
                color = gc,
                style = Stroke(width = style.glow.width.toPx())
            )
        }

        // ----- Main stroke -----
        val stroke = Stroke(width = style.line.strokeWidth.toPx(), cap = StrokeCap.Round)
        style.line.brushProvider?.let { br ->
            drawPath(linePath, brush = br(size), style = stroke)
        } ?: run {
            drawPath(linePath, color = style.line.color, style = stroke)
        }

        // ----- Dots -----
        if (style.dots.show) {
            val r = style.dots.radius.toPx()
            val c = style.dots.color ?: style.line.color
            pts.forEach { drawCircle(color = c, radius = r, center = it) }
        }

        // ----- Extrema labels (min & max) -----
        if (style.extrema.show && pts.isNotEmpty()) {
            val maxIdx = data.points.indices.maxBy { data.points[it].y }
            val minIdx = data.points.indices.minBy { data.points[it].y }
            fun drawTag(idx: Int) {
                val p = pts[idx]
                val value = data.points[idx].y
                val label = style.yAxis.formatter(value, data)
                val layout = measurer.measure(AnnotatedString(label), style.extrema.textStyle)
                val x = (p.x - layout.size.width / 2f).coerceIn(
                    chart.left,
                    chart.right - layout.size.width
                )
                val y = (p.y - layout.size.height - 6.dp.toPx()).coerceAtLeast(chart.top)
                drawCircle(
                    color = (style.extrema.markerColor ?: style.line.color),
                    radius = style.extrema.markerRadius.toPx(),
                    center = p
                )
                drawText(layout, topLeft = Offset(x, y))
            }
            drawTag(maxIdx)
            drawTag(minIdx)
        }
    }
}