package com.grippo.chart.area

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
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
    style: AreaStyle
) {
    val measurer = rememberTextMeasurer()

    Canvas(modifier) {
        if (data.points.isEmpty()) return@Canvas

        // ----- X domain -----
        val minX = data.points.minOf { it.x }
        val maxX = data.points.maxOf { it.x }
        val spanX = (maxX - minX).coerceAtLeast(1e-3f)

        // ----- Y domain (auto with headroom) -----
        val rawMin = data.points.minOf { it.y }
        val rawMax = data.points.maxOf { it.y }
        val rawSpan = (rawMax - rawMin).coerceAtLeast(1e-6f)
        val pad = (rawSpan * 0.08f).coerceAtLeast(1e-3f)
        val yMinTarget = rawMin - pad
        val yMaxTarget = rawMax + pad

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

        val yStep = niceStep(yMaxTarget - yMinTarget, max(2, style.yAxis.targetTicks))
        val minY = floor(yMinTarget / yStep) * yStep
        val maxY = ceil(yMaxTarget / yStep) * yStep
        val spanY = (maxY - minY).coerceAtLeast(1e-6f)

        // ----- Measure labels (all inside the canvas; no external paddings) -----
        val labelPadPx = style.labelPadding.toPx()

        // Y labels prepared first to compute left gutter
        val yTicks = buildList {
            var v = minY
            while (v <= maxY + 1e-6f) {
                add(v); v += yStep
            }
        }
        val rawY = yTicks.map { style.yAxis.formatter(it, data) }

        fun commonSuffix(list: List<String>): String {
            if (list.isEmpty()) return ""
            val f = list.first()
            var i = 0
            while (i < f.length && list.all { it.length > i && it[it.length - 1 - i] == f[f.length - 1 - i] }) i++
            return if (i == 0) "" else f.substring(f.length - i)
        }

        var sharedSuffix = commonSuffix(rawY)
        if (sharedSuffix.any().not() || sharedSuffix.any { !it.isLetter() }) sharedSuffix = ""
        val yLabels = if (sharedSuffix.isEmpty()) rawY else rawY.map {
            it.removeSuffix(sharedSuffix).trimEnd()
        }

        val yLayouts = if (style.yAxis.show)
            yLabels.map { measurer.measure(AnnotatedString(it), style.yAxis.textStyle) }
        else emptyList()

        val unitLayout = if (style.yAxis.show && sharedSuffix.isNotEmpty())
            measurer.measure(AnnotatedString(sharedSuffix), style.yAxis.textStyle)
        else null

        val leftGutter = if (style.yAxis.show && yLayouts.isNotEmpty()) {
            val wLabels = yLayouts.maxOf { it.size.width }.toFloat()
            val wUnit = (unitLayout?.size?.width ?: 0).toFloat()
            (wLabels + wUnit + labelPadPx * 1.5f)
        } else 0f

        // X labels come from points; we’ll thin them to avoid overlap
        val xLayouts = if (style.xAxis.show)
            data.points.map { p ->
                p.xLabel?.let {
                    measurer.measure(
                        AnnotatedString(it),
                        style.xAxis.textStyle
                    )
                }
            }
        else emptyList()

        // FIX: remove extra half padding at the bottom
        val maxXLabelH = xLayouts.filterNotNull().maxOfOrNull { it.size.height }?.toFloat() ?: 0f
        val bottomGutter = if (style.xAxis.show && maxXLabelH > 0f) maxXLabelH else 0f

        // ----- Chart rect (fully inside canvas; no external margins) -----
        val chart = Rect(
            left = leftGutter,
            top = 0f,
            right = size.width,
            bottom = size.height - bottomGutter
        )
        if (chart.width <= 1f || chart.height <= 1f) return@Canvas

        fun mapX(x: Float) = chart.left + (x - minX) / spanX * chart.width
        fun mapY(y: Float) = chart.bottom - (y - minY) / spanY * chart.height

        val pts = data.points.map { Offset(mapX(it.x), mapY(it.y)) }

        // ----- Grid (horizontal only; inside) -----
        if (style.grid.show) {
            val gw = style.grid.strokeWidth.toPx()
            for (v in yTicks) {
                val y = mapY(v)
                drawLine(style.grid.color, Offset(chart.left, y), Offset(chart.right, y), gw)
            }
        }

        // ----- Y axis (labels inside; no overflow) -----
        if (style.yAxis.show && yLayouts.isNotEmpty()) {
            val axisW = style.yAxis.axisLineWidth.toPx()
            for (i in yTicks.indices) {
                val y = mapY(yTicks[i])
                drawLine(
                    color = style.yAxis.axisLineColor,
                    start = Offset(chart.left, y),
                    end = Offset(chart.left + 4.dp.toPx(), y),
                    strokeWidth = axisW
                )
                val l = yLayouts[i]
                val lW = l.size.width.toFloat()
                val lH = l.size.height.toFloat()
                val top = (y - lH / 2f).coerceIn(chart.top, chart.bottom - lH)
                val unitW = unitLayout?.size?.width?.toFloat() ?: 0f
                drawText(
                    l,
                    topLeft = Offset(
                        x = chart.left - labelPadPx - unitW - lW,
                        y = top
                    )
                )
            }
            if (style.yAxis.showLine) {
                drawLine(
                    color = style.yAxis.axisLineColor,
                    start = Offset(chart.left, chart.top),
                    end = Offset(chart.left, chart.bottom),
                    strokeWidth = axisW
                )
            }
            unitLayout?.let { u ->
                val uW = u.size.width.toFloat()
                val uH = u.size.height.toFloat()
                val ux = chart.left - labelPadPx - uW
                val uy = (chart.top - uH - 2.dp.toPx()).coerceAtLeast(0f)
                drawText(u, topLeft = Offset(ux, uy))
            }
        }

        // ----- X axis (labels with strict min gap; keep tail sane) -----
        if (style.xAxis.show && xLayouts.isNotEmpty()) {
            val minGapPx = style.xAxis.minGapDp.toPx()

            if (style.xAxis.showAll) {
                // Draw every label; clamp into chart; allow overlaps by design
                for (i in data.points.indices) {
                    val lay = xLayouts[i] ?: continue
                    val w = lay.size.width.toFloat()
                    val cx = pts[i].x
                    val left = (cx - w / 2f).coerceIn(chart.left, chart.right - w)
                    drawText(lay, topLeft = Offset(left, chart.bottom))
                }
            } else {
                // Sort by actual X
                val order = data.points.indices.sortedBy { pts[it].x }

                // Precompute layout bounds for each index
                data class L(val left: Float, val right: Float, val layout: TextLayoutResult)

                val bounds = mutableMapOf<Int, L>()
                for (i in order) {
                    val lay = xLayouts[i] ?: continue
                    val w = lay.size.width.toFloat()
                    val cx = pts[i].x
                    val left = (cx - w / 2f).coerceIn(chart.left, chart.right - w)
                    bounds[i] = L(left = left, right = left + w, layout = lay)
                }

                val selected = mutableListOf<Int>()
                var lastRight = Float.NEGATIVE_INFINITY

                // Pick the first available label (if any)
                run {
                    for (i in order) {
                        val b = bounds[i] ?: continue
                        selected.add(i)
                        lastRight = b.right
                        break
                    }
                }

                // Middle labels with strict gap
                for (k in 1 until order.lastIndex) {
                    val i = order[k]
                    val b = bounds[i] ?: continue
                    if (b.left >= lastRight + minGapPx) {
                        selected.add(i)
                        lastRight = b.right
                    }
                }

                // Add the last label; drop trailing ones if it would overlap
                val lastIdx = order.last()
                val lastB = bounds[lastIdx]
                if (lastB != null) {
                    while (selected.isNotEmpty()) {
                        val tail = bounds[selected.last()] ?: break
                        if (lastB.left >= tail.right + minGapPx) break
                        if (selected.size == 1) break // keep the very first if it's the only one
                        selected.removeLast()
                    }
                    selected.add(lastIdx)
                }

                // Draw only the selected labels
                selected.sortedBy { pts[it].x }.forEach { i ->
                    val b = bounds[i] ?: return@forEach
                    drawText(b.layout, topLeft = Offset(b.left, chart.bottom))
                }
            }
        }

        // ----- Line path (curved with overshoot clamp) -----
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
                var c1 = Offset(p1.x + (p2.x - p0.x) * s, p1.y + (p2.y - p0.y) * s)
                var c2 = Offset(p2.x - (p3.x - p1.x) * s, p2.y - (p3.y - p1.y) * s)
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

        // ----- Area fill (inside) -----
        style.fill?.let { provider ->
            val area = Path().apply {
                addPath(linePath)
                lineTo(pts.last().x, chart.bottom)
                lineTo(pts.first().x, chart.bottom)
                close()
            }
            drawPath(area, brush = provider.brushProvider(size))
        }

        // ----- Glow -----
        if (style.glow.width.value > 0f) {
            val gc = (style.glow.color ?: style.line.color).copy(alpha = 0.25f)
            drawPath(linePath, color = gc, style = Stroke(width = style.glow.width.toPx()))
        }

        // ----- Stroke -----
        val stroke = Stroke(width = style.line.strokeWidth.toPx(), cap = StrokeCap.Round)
        style.line.brushProvider?.let { br ->
            drawPath(linePath, brush = br(size), style = stroke)
        } ?: drawPath(linePath, color = style.line.color, style = stroke)

        // ----- Dots -----
        if (style.dots.show) {
            val r = style.dots.radius.toPx()
            val c = style.dots.color ?: style.line.color
            pts.forEach { drawCircle(color = c, radius = r, center = it) }
        }

        // ----- Extrema -----
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
                val y = (p.y - layout.size.height - 4.dp.toPx()).coerceAtLeast(chart.top)
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