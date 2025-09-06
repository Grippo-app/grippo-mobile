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
import kotlin.math.floor
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
    // Justify-specific fields:
    val gapPlusOneCount: Int,   // how many early gaps get +1px
    val gridLeftOffsetPx: Float // symmetric padding leftover after gap bumps
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
    val requestedGapPx = style.layout.gap.toPx()
    val rawRx = style.layout.corner.toPx()

    val hasRowLabels = rowLabelLayouts.isNotEmpty()
    val hasColLabels = colLabelLayouts.isNotEmpty()

    // Left gutter: row labels + labelPadding (only if labels exist)
    var leftGutter = 0f
    if (hasRowLabels) {
        val maxW = rowLabelLayouts.maxOf { it.size.width }
        leftGutter = maxW + labelPadPx
    }

    // Bottom is split: xAxis block + legend block
    val colLabelsMaxH = if (hasColLabels) colLabelLayouts.maxOf { it.size.height }.toFloat() else 0f
    val bottomForColLabels = if (hasColLabels) labelPadPx + colLabelsMaxH else 0f

    var minLegendLayout: TextLayoutResult? = null
    var maxLegendLayout: TextLayoutResult? = null
    val bottomForLegend = when (val lg = style.legend) {
        is HeatmapStyle.Legend.Visible -> {
            // Only reserve space for legend labels if at least one is provided.
            val wantMin = lg.minText != null
            val wantMax = lg.maxText != null
            val minLayout = if (wantMin) {
                val txt = lg.minText.invoke(minVal)
                measurer.measure(AnnotatedString(txt), lg.labelStyle)
            } else null
            val maxLayout = if (wantMax) {
                val txt = lg.maxText.invoke(maxVal)
                measurer.measure(AnnotatedString(txt), lg.labelStyle)
            } else null

            minLegendLayout = minLayout
            maxLegendLayout = maxLayout

            val legendTextMaxH = max(
                minLayout?.size?.height ?: 0,
                maxLayout?.size?.height ?: 0
            ).toFloat()

            val gapBetweenXAxisAndLegend = if (hasColLabels) labelPadPx else 0f

            // If there's no label text at all, do NOT add spacing below the bar.
            val belowBar = if (legendTextMaxH > 0f) legendLabelSpacingPx + legendTextMaxH else 0f

            gapBetweenXAxisAndLegend + lg.height.toPx() + belowBar
        }

        is HeatmapStyle.Legend.None -> 0f
    }

    // Grid sizing (squares), with safe gap and pixel-snapped sides
    val rows = data.matrix.rows
    val cols = data.matrix.cols
    val widthForGridRaw = (widthPx - leftGutter).coerceAtLeast(0f)

    // Snap target width to integer pixels
    val widthForGrid = floor(widthForGridRaw).coerceAtLeast(0f)

    val safeGapPx = if (cols > 1) min(requestedGapPx, widthForGrid / cols) else 0f
    val fitToWidth = if (cols > 0) (widthForGrid - safeGapPx * (cols - 1)) / cols else 0f

    val maxCellPx = style.layout.maxCellSize?.toPx()
    var cell = if (maxCellPx != null) min(fitToWidth, maxCellPx) else fitToWidth

    // Snap cell and gap to whole pixels to avoid hairlines
    cell = floor(cell).coerceAtLeast(0f)
    val snappedGap = floor(safeGapPx).coerceAtLeast(0f)

    val cw = cell
    val ch = cell

    // Compute snapped grid size
    val gridW = cw * cols + snappedGap * (cols - 1)
    val gridH = ch * rows + snappedGap * (rows - 1)

    // JUSTIFY: distribute leftover pixels into early gaps (+1px), remainder as symmetric padding
    val extraPx = (widthForGrid - gridW).toInt().coerceAtLeast(0)
    val gaps = max(0, cols - 1)
    val gapPlusOneCount = if (gaps > 0) min(extraPx, gaps) else 0
    val rest = extraPx - gapPlusOneCount
    val gridLeftOffsetPx = if (rest > 0) floor(rest / 2f) else 0f

    val chartLeft = leftGutter + gridLeftOffsetPx
    val chartTop = 0f
    val chartRight = chartLeft + gridW
    val chart = Rect(chartLeft, chartTop, chartRight, chartTop + gridH)

    val totalHeightPx = gridH + bottomForColLabels + bottomForLegend

    return HeatmapPlan(
        totalHeightPx = totalHeightPx,
        chart = chart,
        gridLeft = chart.left,
        gridTop = chart.top,
        cw = cw,
        ch = ch,
        gapPx = snappedGap,
        rx = rawRx, // clamped per-cell at draw time
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
        maxVal = maxVal,
        gapPlusOneCount = gapPlusOneCount,
        gridLeftOffsetPx = gridLeftOffsetPx
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

    // Column helpers that account for justify bumps
    fun colStart(c: Int): Float {
        val bump = if (plan.gapPlusOneCount > 0) min(c, plan.gapPlusOneCount).toFloat() else 0f
        return plan.gridLeft + c * (plan.cw + plan.gapPx) + bump
    }

    fun colCenter(c: Int): Float = colStart(c) + plan.cw / 2f

    val chart = plan.chart
    val cw = plan.cw
    val ch = plan.ch
    val gap = plan.gapPx

    // Optional background (missing cells)
    style.palette.missingCellColor?.let { bg ->
        for (r in 0 until plan.rows) {
            var x = plan.gridLeft
            for (c in 0 until plan.cols) {
                val y = plan.gridTop + (ch + gap) * r
                val w = if (c == plan.cols - 1) (chart.right - x) else cw
                val h = if (r == plan.rows - 1) (chart.bottom - y) else ch
                val rxCell = minOf(plan.rx, w * 0.5f, h * 0.5f)
                drawRoundRect(
                    color = bg,
                    topLeft = Offset(x, y),
                    size = Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(rxCell, rxCell)
                )
                if (c < plan.cols - 1) {
                    val bump = if (c < plan.gapPlusOneCount) 1f else 0f
                    x += w + gap + bump
                }
            }
        }
    }

    // Data cells
    for (r in 0 until plan.rows) {
        var x = plan.gridLeft
        for (c in 0 until plan.cols) {
            val y = plan.gridTop + (ch + gap) * r
            val w = if (c == plan.cols - 1) (chart.right - x) else cw
            val h = if (r == plan.rows - 1) (chart.bottom - y) else ch
            val t = norm(data.matrix[r, c])
            val color = style.palette.colorScale(t)
            val rxCell = minOf(plan.rx, w * 0.5f, h * 0.5f)

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(rxCell, rxCell)
            )

            when (val b = style.borders) {
                is HeatmapStyle.Borders.Visible -> if (b.borderWidth.value > 0f) {
                    drawRoundRect(
                        color = b.borderColor,
                        topLeft = Offset(x, y),
                        size = Size(w, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(rxCell, rxCell),
                        style = Stroke(width = b.borderWidth.toPx())
                    )
                }

                is HeatmapStyle.Borders.None -> Unit
            }

            when (val v = style.values) {
                is HeatmapStyle.Values.Visible -> {
                    val txt = v.formatter(t, data)
                    val layout = measurer.measure(AnnotatedString(txt), v.textStyle)
                    val cx = x + (w - layout.size.width) / 2f
                    val cy = y + (h - layout.size.height) / 2f
                    drawText(layout, topLeft = Offset(cx, cy))
                }

                is HeatmapStyle.Values.None -> Unit
            }

            if (c < plan.cols - 1) {
                val bump = if (c < plan.gapPlusOneCount) 1f else 0f
                x += w + gap + bump
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

    // X axis labels (bottom) — use colCenter() to keep in sync with justify bumps
    when (val cLbl = style.colLabels) {
        is HeatmapStyle.AxisLabels.ShowAll -> if (data.colLabels.isNotEmpty()) {
            val y = chart.bottom + plan.labelPadPx
            for (c in 0 until plan.cols) {
                val layout = plan.colLabelLayouts[c]
                val cx = colCenter(c)
                val x = cx - layout.size.width / 2f
                drawText(layout, topLeft = Offset(x, y))
            }
        }

        is HeatmapStyle.AxisLabels.Adaptive -> if (data.colLabels.isNotEmpty()) {
            val minGapPx = cLbl.minGapDp.toPx()

            data class Box(val left: Float, val right: Float, val layout: TextLayoutResult)

            val boxes = (0 until plan.cols).map { c ->
                val layout = plan.colLabelLayouts[c]
                val w = layout.size.width.toFloat()
                val cx = colCenter(c)
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

    // Legend: quantized from data using current palette/normalization
    when (val lg = style.legend) {
        is HeatmapStyle.Legend.None -> Unit
        is HeatmapStyle.Legend.Visible -> {
            val hasColLabels = plan.colLabelsMaxHPx > 0f
            val yAfterXAxis = if (hasColLabels) (plan.labelPadPx + plan.colLabelsMaxHPx) else 0f
            val legendTop = chart.bottom + yAfterXAxis + if (hasColLabels) plan.labelPadPx else 0f

            val legendRect = Rect(
                chart.left, legendTop,
                chart.right, legendTop + lg.height.toPx()
            )

            val legendColors = deriveLegendColorsFromDataQuantized(
                data = data,
                style = style,
                minVal = plan.minVal,
                maxVal = plan.maxVal,
                bins = 6
            )

            if (legendColors.isNotEmpty()) {
                drawDiscreteLegend(legendRect, legendColors)
            } else {
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

            // Draw legend labels only if they exist; no extra spacing otherwise (height already planned).
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
 * Legend bins are sampled on the actual normalized data range [tMin tMax].
 * If the range collapses, a single band is drawn.
 */
private fun deriveLegendColorsFromDataQuantized(
    data: HeatmapData,
    style: HeatmapStyle,
    minVal: Float,
    maxVal: Float,
    bins: Int = 6
): List<Color> {
    val values = data.matrix.values
    if (values.isEmpty()) return emptyList()

    val norm: (Float) -> Float =
        if (style.palette.autoNormalize && maxVal != minVal) {
            { v -> ((v - minVal) / (maxVal - minVal)).coerceIn(0f, 1f) }
        } else {
            { v -> v.coerceIn(0f, 1f) }
        }

    var tMin = 1f
    var tMax = 0f
    for (v in values) {
        val t = norm(v)
        if (t < tMin) tMin = t
        if (t > tMax) tMax = t
    }
    val span = (tMax - tMin)
    if (span <= 1e-6f) return listOf(style.palette.colorScale(tMin))

    val count = max(2, bins)
    val tmp = ArrayList<Color>(count)
    for (i in 0 until count) {
        val center = tMin + (i + 0.5f) / count * span
        tmp += style.palette.colorScale(center)
    }

    // Collapse adjacent equal colors
    val out = ArrayList<Color>(tmp.size)
    var prev: Color? = null
    for (c in tmp) {
        if (prev == null || prev != c) out += c
        prev = c
    }
    return out
}

/** Paints N equal-width color bands for a discrete legend. */
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