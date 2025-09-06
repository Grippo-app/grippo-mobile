package com.grippo.chart.heatmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

@Composable
public fun HeatmapChart(
    modifier: Modifier = Modifier,
    data: HeatmapData,
    style: HeatmapStyle,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // We need container width to compute square size and auto height.
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthDp = this.maxWidth

        val plan = remember(widthDp, data, style) {
            buildPlan(
                widthDp = widthDp,
                data = data,
                style = style,
                measurer = measurer,
                density = density
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(with(density) { plan.totalHeightPx.toDp() })
        ) {
            drawHeatmap(plan, data, style, measurer)
        }
    }
}

// =================== PLAN BUILDER ===================

private fun buildPlan(
    widthDp: Dp,
    data: HeatmapData,
    style: HeatmapStyle,
    measurer: TextMeasurer,
    density: Density
): HeatmapPlan = with(density) {
    // Domain min/max (even if matrix is already [0..1], keep robust)
    val vals = data.matrix.values
    var minVal = 0f
    var maxVal = 1f
    if (style.palette.autoNormalize && vals.isNotEmpty()) {
        minVal = vals.minOrNull() ?: 0f
        maxVal = vals.maxOrNull() ?: 1f
        if (maxVal == minVal) maxVal += 1f
    }

    // Measure row/col labels once
    val rowLabelLayouts = when (val rLbl = style.rowLabels) {
        is HeatmapStyle.AxisLabels.ShowAll -> (0 until data.matrix.rows).map { r ->
            measurer.measure(AnnotatedString(data.rowLabels.getOrNull(r) ?: ""), rLbl.textStyle)
        }

        is HeatmapStyle.AxisLabels.Adaptive -> (0 until data.matrix.rows).map { r ->
            measurer.measure(AnnotatedString(data.rowLabels.getOrNull(r) ?: ""), rLbl.textStyle)
        }

        is HeatmapStyle.AxisLabels.None -> emptyList()
    }
    val colLabelLayouts = when (val cLbl = style.colLabels) {
        is HeatmapStyle.AxisLabels.ShowAll -> (0 until data.matrix.cols).map { c ->
            measurer.measure(AnnotatedString(data.colLabels.getOrNull(c) ?: ""), cLbl.textStyle)
        }

        is HeatmapStyle.AxisLabels.Adaptive -> (0 until data.matrix.cols).map { c ->
            measurer.measure(AnnotatedString(data.colLabels.getOrNull(c) ?: ""), cLbl.textStyle)
        }

        is HeatmapStyle.AxisLabels.None -> emptyList()
    }

    // Gutters
    val widthPx = widthDp.toPx()
    val labelPadPx = style.layout.labelPadding.toPx()
    val legendLabelSpacingPx = 2.dp.toPx()
    val gapPx = style.layout.gap.toPx()
    val rx = style.layout.corner.toPx()

    var leftGutter = 0f
    when (style.rowLabels) {
        is HeatmapStyle.AxisLabels.ShowAll,
        is HeatmapStyle.AxisLabels.Adaptive -> if (rowLabelLayouts.isNotEmpty()) {
            val maxW = rowLabelLayouts.maxOf { it.size.width }
            leftGutter += maxW + labelPadPx
        }

        is HeatmapStyle.AxisLabels.None -> Unit
    }

    var bottomGutter = 0f
    var colLabelsMaxH = 0f
    when (style.colLabels) {
        is HeatmapStyle.AxisLabels.ShowAll,
        is HeatmapStyle.AxisLabels.Adaptive -> if (colLabelLayouts.isNotEmpty()) {
            val maxH = colLabelLayouts.maxOf { it.size.height }
            colLabelsMaxH = maxH.toFloat()
            bottomGutter += colLabelsMaxH + labelPadPx
        }

        is HeatmapStyle.AxisLabels.None -> Unit
    }

    var minLegendLayout: TextLayoutResult? = null
    var maxLegendLayout: TextLayoutResult? = null
    when (val lg = style.legend) {
        is HeatmapStyle.Legend.Visible -> {
            val minText = (lg.minText?.let { it(minVal) }) ?: "0%"
            val maxText = (lg.maxText?.let { it(maxVal) }) ?: "100%"
            minLegendLayout = measurer.measure(AnnotatedString(minText), lg.labelStyle)
            maxLegendLayout = measurer.measure(AnnotatedString(maxText), lg.labelStyle)
            val legendTextMaxH =
                max(minLegendLayout.size.height, maxLegendLayout.size.height).toFloat()
            bottomGutter += lg.height.toPx() + legendLabelSpacingPx + legendTextMaxH + labelPadPx
        }

        is HeatmapStyle.Legend.None -> Unit
    }

    // Chart rect (height is unknown until cell side is known)
    val chartLeft = leftGutter
    val chartTop = 0f
    val chartRight = widthPx
    val chartW = (chartRight - chartLeft).coerceAtLeast(0f)

    // Square sizing (no placeholders)
    val rows = data.matrix.rows
    val cols = data.matrix.cols
    val fitToWidth = if (cols > 0) (chartW - gapPx * (cols - 1)) / cols else 0f
    val maxCellPx = style.layout.maxCellSize?.toPx()
    val cell = if (maxCellPx != null) min(fitToWidth, maxCellPx) else fitToWidth

    val cw = cell
    val ch = cell // always square

    val gridH = rows * ch + gapPx * (rows - 1)
    val totalHeightPx = gridH + bottomGutter
    val chart = Rect(chartLeft, chartTop, chartRight, totalHeightPx - bottomGutter)

    HeatmapPlan(
        totalHeightPx = totalHeightPx,
        chart = chart,
        gridLeft = chart.left,
        gridTop = chart.top,
        cw = cw,
        ch = ch,
        gapPx = gapPx,
        rx = rx,
        rows = rows,
        cols = cols,
        rowLabelLayouts = rowLabelLayouts,
        colLabelLayouts = colLabelLayouts,
        colLabelsMaxHPx = colLabelsMaxH,
        labelPadPx = labelPadPx,
        legendLabelSpacingPx = legendLabelSpacingPx,
        minLegendTextLayout = minLegendLayout,
        maxLegendTextLayout = maxLegendLayout,
        minVal = minVal,
        maxVal = maxVal
    )
}

// =================== DRAW ===================

private fun DrawScope.drawHeatmap(
    plan: HeatmapPlan,
    data: HeatmapData,
    style: HeatmapStyle,
    measurer: TextMeasurer
) {
    if (plan.rows <= 0 || plan.cols <= 0) return

    fun norm(v: Float): Float =
        if (!style.palette.autoNormalize) v
        else ((v - plan.minVal) / (plan.maxVal - plan.minVal)).coerceIn(0f, 1f)

    val chart = plan.chart
    val cw = plan.cw
    val ch = plan.ch
    val gap = plan.gapPx
    val rx = plan.rx

    // Background for cells (optional)
    style.palette.missingCellColor?.let { bg ->
        for (r in 0 until plan.rows) {
            for (c in 0 until plan.cols) {
                val x = plan.gridLeft + (cw + gap) * c
                val y = plan.gridTop + (ch + gap) * r
                drawRoundRect(
                    color = bg,
                    topLeft = Offset(x, y),
                    size = Size(cw, ch),
                    cornerRadius = CornerRadius(rx, rx)
                )
            }
        }
    }

    // Data cells
    for (r in 0 until plan.rows) {
        for (c in 0 until plan.cols) {
            val x = plan.gridLeft + (cw + gap) * c
            val y = plan.gridTop + (ch + gap) * r
            val t = norm(data.matrix[r, c])
            val color = style.palette.colorScale(t)

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(cw, ch),
                cornerRadius = CornerRadius(rx, rx)
            )

            when (val b = style.borders) {
                is HeatmapStyle.Borders.Visible -> if (b.borderWidth.value > 0f) {
                    drawRoundRect(
                        color = b.borderColor,
                        topLeft = Offset(x, y),
                        size = Size(cw, ch),
                        cornerRadius = CornerRadius(rx, rx),
                        style = Stroke(width = b.borderWidth.toPx())
                    )
                }

                is HeatmapStyle.Borders.None -> Unit
            }

            when (val v = style.values) {
                is HeatmapStyle.Values.Visible -> {
                    val txt = v.formatter(t, data)
                    val layout = measurer.measure(AnnotatedString(txt), v.textStyle)
                    val cx = x + (cw - layout.size.width) / 2f
                    val cy = y + (ch - layout.size.height) / 2f
                    drawText(layout, topLeft = Offset(cx, cy))
                }

                is HeatmapStyle.Values.None -> Unit
            }
        }
    }

    // Row labels (left)
    when (val rLbl = style.rowLabels) {
        is HeatmapStyle.AxisLabels.ShowAll -> if (data.rowLabels.isNotEmpty()) {
            for (r in 0 until plan.rows) {
                val layout = plan.rowLabelLayouts[r]
                val y = plan.gridTop + r * (ch + gap) + (ch - layout.size.height) / 2f
                val x = chart.left - plan.labelPadPx - layout.size.width
                drawText(layout, topLeft = Offset(x, y))
            }
        }

        is HeatmapStyle.AxisLabels.Adaptive -> if (data.rowLabels.isNotEmpty()) {
            val minGapPx = rLbl.minGapDp.toPx()
            val layouts = plan.rowLabelLayouts
            var step = 1
            // Choose step so label boxes don't overlap vertically
            while (true) {
                var ok = true
                var prevBottom = Float.NEGATIVE_INFINITY
                var r = 0
                while (r < plan.rows) {
                    val l = layouts[r]
                    val y = plan.gridTop + r * (ch + gap) + (ch - l.size.height) / 2f
                    val top = y
                    if (top < prevBottom + minGapPx) {
                        ok = false; break
                    }
                    prevBottom = top + l.size.height
                    r += step
                }
                if (ok) break
                step++
                if (step > plan.rows) break
            }
            var r = 0
            while (r < plan.rows) {
                val l = layouts[r]
                val y = plan.gridTop + r * (ch + gap) + (ch - l.size.height) / 2f
                val x = chart.left - plan.labelPadPx - l.size.width
                drawText(l, topLeft = Offset(x, y))
                r += step
            }
        }

        is HeatmapStyle.AxisLabels.None -> Unit
    }

    // Column labels (bottom)
    when (val cLbl = style.colLabels) {
        is HeatmapStyle.AxisLabels.ShowAll -> if (data.colLabels.isNotEmpty()) {
            for (c in 0 until plan.cols) {
                val layout = plan.colLabelLayouts[c]
                val x = plan.gridLeft + c * (cw + gap) + (cw - layout.size.width) / 2f
                val y = chart.bottom + plan.labelPadPx / 2f
                drawText(layout, topLeft = Offset(x, y))
            }
        }

        is HeatmapStyle.AxisLabels.Adaptive -> if (data.colLabels.isNotEmpty()) {
            val minGapPx = cLbl.minGapDp.toPx()

            data class Box(val left: Float, val right: Float, val layout: TextLayoutResult)

            val boxes = (0 until plan.cols).map { c ->
                val layout = plan.colLabelLayouts[c]
                val w = layout.size.width.toFloat()
                val cx = plan.gridLeft + c * (cw + gap) + cw / 2f
                val left = (cx - w / 2f).coerceIn(chart.left, chart.right - w)
                Box(left, left + w, layout)
            }

            val selected = mutableListOf<Int>()
            var lastRight = Float.NEGATIVE_INFINITY
            for (i in boxes.indices) {
                val b = boxes[i]
                if (b.left >= lastRight + minGapPx) {
                    selected.add(i)
                    lastRight = b.right
                }
            }
            // Try to keep the last label if possible
            if (selected.isNotEmpty() && selected.last() != boxes.lastIndex) {
                val lastB = boxes.last()
                while (selected.isNotEmpty()) {
                    val tail = boxes[selected.last()]
                    if (lastB.left >= tail.right + minGapPx) break
                    selected.removeLast()
                }
                selected.add(boxes.lastIndex)
            }

            val y = chart.bottom + plan.labelPadPx / 2f
            for (i in selected) {
                val b = boxes[i]
                drawText(b.layout, topLeft = Offset(b.left, y))
            }
        }

        is HeatmapStyle.AxisLabels.None -> Unit
    }

    // Legend
    when (val lg = style.legend) {
        is HeatmapStyle.Legend.None -> Unit
        is HeatmapStyle.Legend.Visible -> {
            val legendTop =
                chart.bottom + (if (plan.colLabelsMaxHPx > 0f) plan.colLabelsMaxHPx + plan.labelPadPx else plan.labelPadPx)
            val legendRect = Rect(chart.left, legendTop, chart.right, legendTop + lg.height.toPx())
            val brush = legendBrush(lg.stops)
            drawRect(
                brush = brush,
                topLeft = Offset(legendRect.left, legendRect.top),
                size = Size(legendRect.width, legendRect.height)
            )
            plan.minLegendTextLayout?.let {
                drawText(
                    it,
                    topLeft = Offset(legendRect.left, legendRect.bottom + plan.legendLabelSpacingPx)
                )
            }
            plan.maxLegendTextLayout?.let {
                drawText(
                    it,
                    topLeft = Offset(
                        legendRect.right - it.size.width,
                        legendRect.bottom + plan.legendLabelSpacingPx
                    )
                )
            }
        }
    }
}

// =================== COLOR / UTILS ===================

private fun legendBrush(stops: List<Pair<Float, Color>>?): Brush {
    val s = stops ?: listOf(
        0f to Color(0xFF3A86FF),
        0.5f to Color(0xFFB049F8),
        1f to Color(0xFFFF7A33)
    )
    return Brush.horizontalGradient(colorStops = s.toTypedArray())
}

/** Default cool→magenta→warm palette. Input is clamped to [0..1]. */
public fun defaultCoolWarm(vIn: Float): Color {
    val v = vIn.coerceIn(0f, 1f)
    val c1 = Color(0xFF3A86FF)
    val c2 = Color(0xFFB049F8)
    val c3 = Color(0xFFFF7A33)
    return if (v < 0.5f) lerpColor(c1, c2, v * 2f) else lerpColor(c2, c3, (v - 0.5f) * 2f)
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return Color(
        red = (a.red + (b.red - a.red) * k),
        green = (a.green + (b.green - a.green) * k),
        blue = (a.blue + (b.blue - a.blue) * k),
        alpha = (a.alpha + (b.alpha - a.alpha) * k)
    )
}