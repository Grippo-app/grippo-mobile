package com.grippo.chart.heatmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

// =================================== PUBLIC API ===================================

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

// =================================== PLAN ===================================

@Immutable
private data class HeatmapPlan(
    val totalHeightPx: Float,
    val chart: Rect,
    val gridLeft: Float,
    val gridTop: Float,
    val cw: Float,
    val ch: Float,
    val gapPx: Float,
    val rx: Float,
    val rows: Int,
    val cols: Int,
    val rowLabelLayouts: List<TextLayoutResult>,
    val colLabelLayouts: List<TextLayoutResult>,
    val colLabelsMaxHPx: Float,
    val labelPadPx: Float,
    val legendLabelSpacingPx: Float,
    val minLegendTextLayout: TextLayoutResult?,
    val maxLegendTextLayout: TextLayoutResult?,
    val minVal: Float,
    val maxVal: Float,
)

private fun buildPlan(
    widthDp: Dp,
    data: HeatmapData,
    style: HeatmapStyle,
    measurer: TextMeasurer,
    density: Density
): HeatmapPlan = with(density) {
    // Domain min/max (robust even if values01 already in [0..1])
    val vals = data.matrix.values
    var minVal = 0f
    var maxVal = 1f
    if (style.palette.autoNormalize && vals.isNotEmpty()) {
        minVal = vals.minOrNull() ?: 0f
        maxVal = vals.maxOrNull() ?: 1f
        if (maxVal == minVal) maxVal += 1f
    }

    // Measure labels once
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

    // Metrics
    val widthPx = widthDp.toPx()
    val labelPadPx = style.layout.labelPadding.toPx()
    val legendLabelSpacingPx = 2.dp.toPx()
    val gapPx = style.layout.gap.toPx()
    val rx = style.layout.corner.toPx()

    val hasRowLabels = rowLabelLayouts.isNotEmpty()
    val hasColLabels = colLabelLayouts.isNotEmpty()

    // Left gutter: row labels + labelPadding (only if labels exist)
    var leftGutter = 0f
    if (hasRowLabels) {
        val maxW = rowLabelLayouts.maxOf { it.size.width }
        leftGutter = maxW + labelPadPx
    }

    // Bottom is split:
    // 1) xAxis block: labelPadding (between grid and labels) + labels' height
    val colLabelsMaxH = if (hasColLabels) colLabelLayouts.maxOf { it.size.height }.toFloat() else 0f
    val bottomForColLabels = if (hasColLabels) labelPadPx + colLabelsMaxH else 0f

    // 2) legend block: labelPadding (between xAxis and legend, only if xAxis exists) + legend block
    var minLegendLayout: TextLayoutResult? = null
    var maxLegendLayout: TextLayoutResult? = null
    val bottomForLegend = when (val lg = style.legend) {
        is HeatmapStyle.Legend.Visible -> {
            val minText = (lg.minText?.let { it(minVal) }) ?: "0%"
            val maxText = (lg.maxText?.let { it(maxVal) }) ?: "100%"
            minLegendLayout = measurer.measure(AnnotatedString(minText), lg.labelStyle)
            maxLegendLayout = measurer.measure(AnnotatedString(maxText), lg.labelStyle)
            val legendTextMaxH =
                max(minLegendLayout.size.height, maxLegendLayout.size.height).toFloat()
            val gapBetweenXAxisAndLegend = if (hasColLabels) labelPadPx else 0f
            gapBetweenXAxisAndLegend + lg.height.toPx() + legendLabelSpacingPx + legendTextMaxH
        }

        is HeatmapStyle.Legend.None -> 0f
    }

    // Grid sizing (always squares, optional cap)
    val widthForGrid = (widthPx - leftGutter).coerceAtLeast(0f)
    val rows = data.matrix.rows
    val cols = data.matrix.cols
    val fitToWidth = if (cols > 0) (widthForGrid - gapPx * (cols - 1)) / cols else 0f
    val maxCellPx = style.layout.maxCellSize?.toPx()
    val cell = if (maxCellPx != null) min(fitToWidth, maxCellPx) else fitToWidth
    val cw = cell
    val ch = cell

    val gridH = rows * ch + gapPx * (rows - 1)
    val totalHeightPx = gridH + bottomForColLabels + bottomForLegend

    val chartLeft = leftGutter
    val chartTop = 0f
    val chartRight = widthPx
    val chart = Rect(chartLeft, chartTop, chartRight, chartTop + gridH)

    return HeatmapPlan(
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

// =================================== DRAW ===================================

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

    // Optional background (missing cells)
    style.palette.missingCellColor?.let { bg ->
        for (r in 0 until plan.rows) {
            for (c in 0 until plan.cols) {
                val x = plan.gridLeft + (cw + gap) * c
                val y = plan.gridTop + (ch + gap) * r
                drawRoundRect(
                    color = bg,
                    topLeft = Offset(x, y),
                    size = Size(cw, ch),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(rx, rx)
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
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(rx, rx)
            )

            when (val b = style.borders) {
                is HeatmapStyle.Borders.Visible -> if (b.borderWidth.value > 0f) {
                    drawRoundRect(
                        color = b.borderColor,
                        topLeft = Offset(x, y),
                        size = Size(cw, ch),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(rx, rx),
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

    // Y axis labels (left)
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
            // Increase step until vertical boxes do not collide.
            while (true) {
                var ok = true
                var prevBottom = Float.NEGATIVE_INFINITY
                var r = 0
                while (r < plan.rows) {
                    val l = layouts[r]
                    val y = plan.gridTop + r * (ch + gap) + (ch - l.size.height) / 2f
                    if (y < prevBottom + minGapPx) {
                        ok = false; break
                    }
                    prevBottom = y + l.size.height
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

    // X axis labels (bottom)
    when (val cLbl = style.colLabels) {
        is HeatmapStyle.AxisLabels.ShowAll -> if (data.colLabels.isNotEmpty()) {
            val y = chart.bottom + plan.labelPadPx
            for (c in 0 until plan.cols) {
                val layout = plan.colLabelLayouts[c]
                val x = plan.gridLeft + c * (cw + gap) + (cw - layout.size.width) / 2f
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
                    selected.add(i); lastRight = b.right
                }
            }
            // Keep the last label if possible
            if (selected.isNotEmpty() && selected.last() != boxes.lastIndex) {
                val lastB = boxes.last()
                while (selected.isNotEmpty()) {
                    val tail = boxes[selected.last()]
                    if (lastB.left >= tail.right + minGapPx) break
                    selected.removeLast()
                }
                selected.add(boxes.lastIndex)
            }

            val y = chart.bottom + plan.labelPadPx
            for (i in selected) {
                val b = boxes[i]
                drawText(b.layout, topLeft = Offset(b.left, y))
            }
        }

        is HeatmapStyle.AxisLabels.None -> Unit
    }

    // Legend: no color map passed; build discrete bands from actual data via palette.colorScale.
    when (val lg = style.legend) {
        is HeatmapStyle.Legend.None -> Unit
        is HeatmapStyle.Legend.Visible -> {
            val hasColLabels = plan.colLabelsMaxHPx > 0f
            val yAfterXAxis = if (hasColLabels) (plan.labelPadPx + plan.colLabelsMaxHPx) else 0f
            // Add labelPadding between xAxis and legend if xAxis exists
            val legendTop = chart.bottom + yAfterXAxis + if (hasColLabels) plan.labelPadPx else 0f

            val legendRect = Rect(
                chart.left, legendTop,
                chart.right, legendTop + lg.height.toPx()
            )

            // Build legend colors from data using current palette and normalization window.
            val legendColors = deriveLegendColorsFromData(
                data = data,
                style = style,
                minVal = plan.minVal,
                maxVal = plan.maxVal
            )

            if (legendColors.isNotEmpty()) {
                drawDiscreteLegend(legendRect, legendColors)
            } else {
                // Fallback (rare): draw neutral gradient if data empty.
                val brush = Brush.horizontalGradient(
                    0f to Color(0xFFE0E0E0),
                    1f to Color(0xFFBDBDBD)
                )
                drawRect(
                    brush = brush,
                    topLeft = Offset(legendRect.left, legendRect.top),
                    size = Size(legendRect.width, legendRect.height)
                )
            }

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

// =================================== HELPERS ===================================

/**
 * Derives a discrete set of legend colors from the actual data values using palette.colorScale.
 * Colors are taken in ascending normalized t order; duplicates are collapsed. Size is capped to avoid overdraw.
 */
private fun deriveLegendColorsFromData(
    data: HeatmapData,
    style: HeatmapStyle,
    minVal: Float,
    maxVal: Float,
    maxColors: Int = 12
): List<Color> {
    val values = data.matrix.values
    if (values.isEmpty()) return emptyList()

    val norm: (Float) -> Float =
        if (style.palette.autoNormalize && maxVal != minVal) {
            { v -> ((v - minVal) / (maxVal - minVal)).coerceIn(0f, 1f) }
        } else {
            { v -> v.coerceIn(0f, 1f) }
        }

    // Sort by t, then collect unique colors in that order.
    val uniq = LinkedHashSet<Color>()
    values.asSequence()
        .map { v -> norm(v) }
        .sorted()
        .forEach { t ->
            uniq += style.palette.colorScale(t)
            if (uniq.size >= maxColors) return@forEach
        }

    // In case all values map to same color, return single band.
    return uniq.toList()
}

/** Paints N equal-width color bands for a discrete legend (first color is always visible). */
private fun DrawScope.drawDiscreteLegend(rect: Rect, colors: List<Color>) {
    if (colors.isEmpty()) return
    val n = colors.size
    val w = rect.width
    val h = rect.height
    for (i in 0 until n) {
        val left = rect.left + w * i / n
        val right = if (i == n - 1) rect.right else rect.left + w * (i + 1) / n
        drawRect(
            color = colors[i],
            topLeft = Offset(left, rect.top),
            size = Size(right - left, h)
        )
    }
}
