package com.grippo.chart.area

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
public fun AreaChart(
    modifier: Modifier = Modifier,
    data: AreaData,
    style: AreaStyle
) {
    val measurer = rememberTextMeasurer()
    val points = remember(data.points) { data.points.sortedBy { it.x } }

    Canvas(modifier) {
        if (points.isEmpty() || points.any { !it.x.isFinite() || !it.y.isFinite() }) {
            return@Canvas
        }

        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val spanX = (maxX.toDouble() - minX).coerceAtLeast(1e-6)

        val rawMin = points.minOf { it.y }.toDouble()
        val rawMax = points.maxOf { it.y }.toDouble()
        val rawSpan = (rawMax - rawMin).coerceAtLeast(1e-6)
        val pad = (rawSpan * 0.08).coerceAtLeast(1e-3)
        val yMinTarget = rawMin - pad
        val yMaxTarget = rawMax + pad

        val targetTicks = when (val yCfg = style.yAxis) {
            is AreaStyle.YAxis.Labels -> max(2, yCfg.targetTicks)
            is AreaStyle.YAxis.None -> 5
        }
        val yStep = niceAreaStep(yMaxTarget - yMinTarget, targetTicks)
        val minY = floor(yMinTarget / yStep) * yStep
        val maxY = ceil(yMaxTarget / yStep) * yStep
        val spanY = (maxY - minY).coerceAtLeast(1e-6)

        val labelPadPx = style.labelPadding.toPx()

        val yTickCount = ((maxY - minY) / yStep).roundToInt().coerceIn(1, 100)
        val yTicks = (0..yTickCount).map { index -> minY + index * yStep }
        val rawY = when (val yCfg = style.yAxis) {
            is AreaStyle.YAxis.Labels -> yTicks.map { yCfg.formatter(it.coerceIn(-Float.MAX_VALUE.toDouble(), Float.MAX_VALUE.toDouble()).toFloat(), data) }
            is AreaStyle.YAxis.None -> emptyList()
        }

        var sharedSuffix = commonAreaSuffix(rawY)
        if (sharedSuffix.any().not() || sharedSuffix.any { !it.isLetter() }) sharedSuffix = ""
        val yLabels = if (sharedSuffix.isEmpty()) rawY else rawY.map {
            it.removeSuffix(sharedSuffix).trimEnd()
        }

        val yLayouts = when (val yCfg = style.yAxis) {
            is AreaStyle.YAxis.Labels -> yLabels.map {
                measurer.measure(
                    AnnotatedString(it),
                    yCfg.textStyle
                )
            }

            is AreaStyle.YAxis.None -> emptyList()
        }

        val unitLayout = when (val yCfg = style.yAxis) {
            is AreaStyle.YAxis.Labels -> if (sharedSuffix.isNotEmpty()) measurer.measure(
                AnnotatedString(sharedSuffix),
                yCfg.textStyle
            ) else null

            is AreaStyle.YAxis.None -> null
        }

        val leftGutter = if (yLayouts.isNotEmpty()) {
            val wLabels = yLayouts.maxOf { it.size.width }.toFloat()
            val wUnit = (unitLayout?.size?.width ?: 0).toFloat()
            (wLabels + wUnit + labelPadPx * 1.5f)
        } else 0f

        val dotsRadius = when (val dots = style.dots) {
            is AreaStyle.Dots.None -> 0f
            is AreaStyle.Dots.Visible -> dots.radius.toPx()
        }
        val markerRadius = when (val extrema = style.extrema) {
            is AreaStyle.Extrema.None -> 0f
            is AreaStyle.Extrema.Visible -> extrema.markerRadius.toPx()
        }
        val inset = maxOf(style.line.strokeWidth.toPx() / 2f, dotsRadius, markerRadius)


        val xLayouts = when (val xCfg = style.xAxis) {
            is AreaStyle.XAxis.LabelsAdaptive -> points.map { p ->
                p.xLabel?.let { measurer.measure(
                    text = AnnotatedString(it),
                    style = xCfg.textStyle,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    constraints = Constraints(maxWidth = (size.width - leftGutter - inset * 2f).toInt().coerceAtLeast(1)),
                ) }
            }

            is AreaStyle.XAxis.LabelsShowAll -> points.map { p ->
                p.xLabel?.let { measurer.measure(
                    text = AnnotatedString(it),
                    style = xCfg.textStyle,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    constraints = Constraints(maxWidth = (size.width - leftGutter - inset * 2f).toInt().coerceAtLeast(1)),
                ) }
            }

            is AreaStyle.XAxis.None -> emptyList()
        }

        val maxXLabelH = xLayouts.filterNotNull().maxOfOrNull { it.size.height }?.toFloat() ?: 0f
        val bottomGutter = if (maxXLabelH > 0f) maxXLabelH + labelPadPx else 0f

        val chart = Rect(
            left = leftGutter + inset,
            top = inset,
            right = size.width - inset,
            bottom = size.height - bottomGutter - inset
        )
        if (chart.width <= 1f || chart.height <= 1f) return@Canvas

        val mapX: (Float) -> Float = { x ->
            if (minX == maxX) chart.center.x else chart.left + ((x.toDouble() - minX) / spanX * chart.width).toFloat()
        }
        val mapY: (Double) -> Float = { y ->
            chart.bottom - ((y - minY) / spanY * chart.height).toFloat()
        }

        val pts = points.map { Offset(mapX(it.x), mapY(it.y.toDouble())) }

        if (style.grid.show) {
            val gw = style.grid.strokeWidth.toPx()
            for (v in yTicks) {
                val y = mapY(v)
                drawLine(style.grid.color, Offset(chart.left, y), Offset(chart.right, y), gw)
            }
        }

        when (val yCfg = style.yAxis) {
            is AreaStyle.YAxis.Labels -> {
                val tickW = yCfg.tickMarkWidth.toPx()
                for (i in yTicks.indices) {
                    val y = mapY(yTicks[i])
                    drawLine(
                        color = yCfg.tickMarkColor,
                        start = Offset(chart.left, y),
                        end = Offset(chart.left + labelPadPx, y),
                        strokeWidth = tickW
                    )
                    val l = yLayouts[i]
                    val lW = l.size.width.toFloat()
                    val lH = l.size.height.toFloat()
                    val top = (y - lH / 2f).coerceIn(chart.top, max(chart.top, chart.bottom - lH))
                    val unitW = unitLayout?.size?.width?.toFloat() ?: 0f
                    drawText(
                        l,
                        topLeft = Offset(
                            x = chart.left - labelPadPx - unitW - lW,
                            y = top
                        )
                    )
                }
                unitLayout?.let { u ->
                    val uW = u.size.width.toFloat()
                    val uH = u.size.height.toFloat()
                    val ux = chart.left - labelPadPx - uW
                    val uy = (chart.top - uH - labelPadPx).coerceAtLeast(0f)
                    drawText(u, topLeft = Offset(ux, uy))
                }
            }

            is AreaStyle.YAxis.None -> Unit
        }
        style.yAxisLine?.let { axis ->
            drawLine(
                color = axis.color,
                start = Offset(chart.left, chart.top),
                end = Offset(chart.left, chart.bottom),
                strokeWidth = axis.width.toPx()
            )
        }

        when (val xCfg = style.xAxis) {
            is AreaStyle.XAxis.None -> Unit
            is AreaStyle.XAxis.LabelsShowAll -> {
                for (i in points.indices) {
                    val lay = xLayouts[i] ?: continue
                    val w = lay.size.width.toFloat()
                    val cx = pts[i].x
                    val left = (cx - w / 2f).coerceIn(chart.left, max(chart.left, chart.right - w))
                    drawText(lay, topLeft = Offset(left, chart.bottom + labelPadPx))
                }
            }

            is AreaStyle.XAxis.LabelsAdaptive -> {
                val minGapPx = xCfg.minGapDp.toPx()
                val order = points.indices.sortedBy { pts[it].x }

                data class L(val left: Float, val right: Float, val layout: TextLayoutResult)

                val bounds = mutableMapOf<Int, L>()
                for (i in order) {
                    val lay = xLayouts[i] ?: continue
                    val w = lay.size.width.toFloat()
                    val cx = pts[i].x
                    val left = (cx - w / 2f).coerceIn(chart.left, max(chart.left, chart.right - w))
                    bounds[i] = L(left = left, right = left + w, layout = lay)
                }

                val selected = mutableListOf<Int>()
                for (index in order) {
                    val bound = bounds[index] ?: continue
                    if (index == order.last()) {
                        while (selected.isNotEmpty() &&
                            bound.left < bounds.getValue(selected.last()).right + minGapPx
                        ) {
                            selected.removeAt(selected.lastIndex)
                        }
                        selected.add(index)
                    } else if (selected.isEmpty() ||
                        bound.left >= bounds.getValue(selected.last()).right + minGapPx
                    ) {
                        selected.add(index)
                    }
                }

                selected.sortedBy { pts[it].x }.forEach { i ->
                    val b = bounds[i] ?: return@forEach
                    drawText(b.layout, topLeft = Offset(b.left, chart.bottom + labelPadPx))
                }
            }
        }

        val linePath = buildAreaLinePath(pts, style.line)

        style.fill?.takeIf { pts.size > 1 }?.let { provider ->
            val area = Path().apply {
                addPath(linePath)
                lineTo(pts.last().x, chart.bottom)
                lineTo(pts.first().x, chart.bottom)
                close()
            }
            drawPath(area, brush = provider.brushProvider(size))
        }

        if (style.glow.width.value > 0f) {
            val gc = (style.glow.color ?: style.line.color).copy(alpha = 0.25f)
            drawPath(linePath, color = gc, style = Stroke(width = style.glow.width.toPx()))
        }

        val stroke = Stroke(width = style.line.strokeWidth.toPx(), cap = StrokeCap.Round)
        style.line.brushProvider?.let { br ->
            drawPath(linePath, brush = br(size), style = stroke)
        } ?: drawPath(linePath, color = style.line.color, style = stroke)

        when (val dCfg = style.dots) {
            is AreaStyle.Dots.Visible -> {
                val r = dCfg.radius.toPx()
                val c = dCfg.color ?: style.line.color
                pts.forEach { drawCircle(color = c, radius = r, center = it) }
            }

            is AreaStyle.Dots.None -> if (pts.size == 1) {
                drawCircle(color = style.line.color, radius = style.line.strokeWidth.toPx(), center = pts.first())
            }
        }

        if (pts.isNotEmpty()) {
            when (val eCfg = style.extrema) {
                is AreaStyle.Extrema.None -> Unit
                is AreaStyle.Extrema.Visible -> {
                    val maxIdx = points.indices.maxBy { points[it].y }
                    val minIdx = points.indices.minBy { points[it].y }
                    val yFormatter: (Float) -> String = when (val yCfg = style.yAxis) {
                        is AreaStyle.YAxis.Labels -> { v -> yCfg.formatter(v, data) }
                        is AreaStyle.YAxis.None -> { v -> v.toString() }
                    }

                    val drawTag: (Int) -> Unit = { idx ->
                        val p = pts[idx]
                        val value = points[idx].y
                        val label = yFormatter(value)
                        val layout = measurer.measure(
                            text = AnnotatedString(label),
                            style = eCfg.textStyle,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            constraints = Constraints(maxWidth = chart.width.toInt().coerceAtLeast(1)),
                        )
                        val x = (p.x - layout.size.width / 2f).coerceIn(
                            chart.left,
                            max(chart.left, chart.right - layout.size.width)
                        )
                        val y = (p.y - layout.size.height - labelPadPx).coerceAtLeast(chart.top)
                        drawCircle(
                            color = (eCfg.markerColor ?: style.line.color),
                            radius = eCfg.markerRadius.toPx(),
                            center = p
                        )
                        drawText(layout, topLeft = Offset(x, y))
                    }
                    drawTag(maxIdx)
                    if (minIdx != maxIdx) drawTag(minIdx)
                }
            }
        }
    }
}

private fun niceAreaStep(span: Double, ticks: Int): Double {
    val raw = (span / ticks).coerceAtLeast(1e-6)
    val exp = floor(log10(raw)).toInt()
    val base = 10.0.pow(exp.toDouble())
    val mul = (raw / base)
    val niceMul = when {
        mul <= 1.0 -> 1.0
        mul <= 2.0 -> 2.0
        mul <= 5.0 -> 5.0
        else -> 10.0
    }
    return niceMul * base
}

private fun commonAreaSuffix(list: List<String>): String {
    if (list.isEmpty()) {
        return ""
    }
    val f = list.first()
    var i = 0
    while (i < f.length && list.all { it.length > i && it[it.length - 1 - i] == f[f.length - 1 - i] }) {
        i++
    }
    return if (i == 0) "" else f.substring(f.length - i)
}

private fun buildAreaLinePath(points: List<Offset>, style: AreaStyle.Line): Path {
    val path = Path()
    if (points.isEmpty()) {
        return path
    }
    if (!style.curved || points.size < 3) {
        path.moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
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
        var c1 = Offset(p1.x + (p2.x - p0.x) * s, p1.y + (p2.y - p0.y) * s)
        var c2 = Offset(p2.x - (p3.x - p1.x) * s, p2.y - (p3.y - p1.y) * s)
        if (style.clampOvershoot) {
            val minY = min(p1.y, p2.y)
            val maxY = max(p1.y, p2.y)
            c1 = Offset(x = c1.x.coerceIn(p1.x, p2.x), y = c1.y.coerceIn(minY, maxY))
            c2 = Offset(x = c2.x.coerceIn(p1.x, p2.x), y = c2.y.coerceIn(minY, maxY))
        }
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    return path
}
