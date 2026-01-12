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

// ============================== PUBLIC API ==============================

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
            drawHeatmap(plan, data, style)
        }
    }
}

// ============================== PLAN ==============================

@androidx.compose.runtime.Immutable
private data class HeatmapPlan(
    val totalHeightPx: Float,
    val chart: Rect,
    val gridLeft: Float,
    val gridTop: Float,
    val cw: Float,
    val ch: Float,
    val gapXPx: Float,           // horizontal gap between columns
    val gapYPx: Float,           // vertical gap between rows
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
    // JUSTIFY (horizontal only):
    val gapPlusOneCount: Int,    // how many first gaps get +1px
    val gridLeftOffsetPx: Float  // symmetric offset after distributing +1px
)

private fun buildPlan(
    widthDp: Dp,
    data: HeatmapData,
    style: HeatmapStyle,
    measurer: TextMeasurer,
    density: Density
): HeatmapPlan = with(density) {
    // Value range is fixed (data is already normalized)
    val minVal = 0f
    val maxVal = 1f

    // Measure labels
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

    // Left gutter: width of longest y-labels + labelPadding
    var leftGutter = 0f
    if (hasRowLabels) {
        val maxW = rowLabelLayouts.maxOf { it.size.width }
        leftGutter = maxW + labelPadPx
    }

    // Bottom: X-axis + (optionally) legend
    val colLabelsMaxH = if (hasColLabels) colLabelLayouts.maxOf { it.size.height }.toFloat() else 0f
    val bottomForColLabels = if (hasColLabels) labelPadPx + colLabelsMaxH else 0f

    var minLegendLayout: TextLayoutResult? = null
    var maxLegendLayout: TextLayoutResult? = null
    val bottomForLegend = when (val lg = style.legend) {
        is HeatmapStyle.Legend.Visible -> {
            val wantMin = lg.minText != null
            val wantMax = lg.maxText != null
            minLegendLayout = if (wantMin) {
                measurer.measure(AnnotatedString(lg.minText.invoke(minVal)), lg.labelStyle)
            } else null
            maxLegendLayout = if (wantMax) {
                measurer.measure(AnnotatedString(lg.maxText.invoke(maxVal)), lg.labelStyle)
            } else null

            val legendTextMaxH = max(
                minLegendLayout?.size?.height ?: 0,
                maxLegendLayout?.size?.height ?: 0
            ).toFloat()

            val gapBetweenXAxisAndLegend = if (hasColLabels) labelPadPx else 0f
            val belowBar = if (legendTextMaxH > 0f) legendLabelSpacingPx + legendTextMaxH else 0f

            gapBetweenXAxisAndLegend + lg.height.toPx() + belowBar
        }

        is HeatmapStyle.Legend.None -> 0f
    }

    // Grid size: stretch horizontally, rectangular cells (cw fills width; ch = maxCellSize)
    val rows = data.matrix.rows
    val cols = data.matrix.cols
    val widthForGridRaw = (widthPx - leftGutter).coerceAtLeast(0f)
    val widthForGrid = floor(widthForGridRaw).coerceAtLeast(0f)

    // Horizontal gap: zeroed when there's only one column. Vertical gap: always requested.
    val safeGapXPx = if (cols > 1) min(requestedGapPx, widthForGrid / cols) else 0f
    val safeGapYPx = requestedGapPx

    // Width per cell to fill available width (no clamp by maxCellSize)
    val fitCw = if (cols > 0) (widthForGrid - safeGapXPx * (cols - 1)) / cols else 0f

    // Height per cell fixed by maxCellSize (non-null)
    val maxCellPx = style.layout.maxCellSize.toPx()

    var cw = fitCw
    var ch = maxCellPx

    // Snap to whole pixels (avoid hairlines)
    cw = floor(cw).coerceAtLeast(0f)
    ch = floor(ch).coerceAtLeast(0f)
    val snappedGapX = floor(safeGapXPx).coerceAtLeast(0f)
    val snappedGapY = floor(safeGapYPx).coerceAtLeast(0f)

    // Base grid size
    val gridW = cw * cols + snappedGapX * (cols - 1)
    val gridH = ch * rows + snappedGapY * (rows - 1)

    // JUSTIFY residual pixels horizontally
    val extraPx = (widthForGrid - gridW).toInt().coerceAtLeast(0)
    val gaps = max(0, cols - 1)
    val gapPlusOneCount = if (gaps > 0) min(extraPx, gaps) else 0
    val rest = extraPx - gapPlusOneCount
    val gridLeftOffsetPx = if (rest > 0) floor(rest / 2f) else 0f

    val chartLeft = leftGutter + gridLeftOffsetPx
    val chartTop = 0f
    // Include +1px bumps on early gaps in the right edge
    val chartRight = chartLeft + gridW + gapPlusOneCount
    val chart = Rect(chartLeft, chartTop, chartRight, chartTop + gridH)

    val totalHeightPx = gridH + bottomForColLabels + bottomForLegend

    return HeatmapPlan(
        totalHeightPx = totalHeightPx,
        chart = chart,
        gridLeft = chart.left,
        gridTop = chart.top,
        cw = cw,
        ch = ch,
        gapXPx = snappedGapX,
        gapYPx = snappedGapY,
        rx = rawRx,
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

// ============================== DRAW ==============================

private fun DrawScope.drawHeatmap(
    plan: HeatmapPlan,
    data: HeatmapData,
    style: HeatmapStyle,
) {
    if (plan.rows <= 0 || plan.cols <= 0) return

    // Column start with +1px bumps distributed into early gaps
    fun colStart(c: Int): Float {
        val bump = if (plan.gapPlusOneCount > 0) min(c, plan.gapPlusOneCount).toFloat() else 0f
        return plan.gridLeft + c * (plan.cw + plan.gapXPx) + bump
    }

    // Column edges computed from starts (prevents accumulation errors)
    fun colEdges(c: Int): Pair<Float, Float> {
        val x0 = colStart(c)
        val x1 = if (c == plan.cols - 1) {
            plan.chart.right
        } else {
            colStart(c + 1) - plan.gapXPx
        }
        return x0 to x1
    }

    fun colCenter(c: Int): Float {
        val (x0, x1) = colEdges(c)
        return (x0 + x1) / 2f
    }

    val chart = plan.chart

    // Cells
    for (r in 0 until plan.rows) {
        for (c in 0 until plan.cols) {
            val (x0, x1) = colEdges(c)
            val w = max(0f, x1 - x0)

            val y0 = plan.gridTop + (plan.ch + plan.gapYPx) * r
            val y1 = if (r == plan.rows - 1) plan.chart.bottom else y0 + plan.ch
            val h = max(0f, y1 - y0)

            val t = data.matrix[r, c].coerceIn(0f, 1f)
            val color = style.palette.colorScale(t)
            val rxCell = min(plan.rx, min(w, h) * 0.5f)

            drawRoundRect(
                color = color,
                topLeft = Offset(x0, y0),
                size = Size(w, h),
                cornerRadius = CornerRadius(rxCell, rxCell)
            )
        }
    }

    // Y labels
    when (val rLbl = style.rowLabels) {
        is HeatmapStyle.AxisLabels.ShowAll -> if (data.rowLabels.isNotEmpty()) {
            for (r in 0 until plan.rows) {
                val layout = plan.rowLabelLayouts[r]
                val y =
                    plan.gridTop + r * (plan.ch + plan.gapYPx) + (plan.ch - layout.size.height) / 2f
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
                    val y =
                        plan.gridTop + r * (plan.ch + plan.gapYPx) + (plan.ch - l.size.height) / 2f
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
                val y = plan.gridTop + r * (plan.ch + plan.gapYPx) + (plan.ch - l.size.height) / 2f
                val x = chart.left - plan.labelPadPx - l.size.width
                drawText(l, topLeft = Offset(x, y))
                r += step
            }
        }

        is HeatmapStyle.AxisLabels.None -> Unit
    }

    // X labels at column centers
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
            // Try to preserve the last label
            if (selected.isNotEmpty() && selected.last() != boxes.lastIndex) {
                val lastB = boxes.last()
                while (selected.isNotEmpty()) {
                    val tail = boxes[selected.last()]
                    if (lastB.left >= tail.right + minGapPx) break
                    selected.removeAt(selected.lastIndex)
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

    // Legend (discrete strips from current palette)
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

            // Text under legend — only if provided
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

// ============================== HELPERS ==============================

/**
 * Generate discrete legend colors based on actual range of normalized values.
 * If range collapsed — one strip.
 */
private fun deriveLegendColorsFromDataQuantized(
    data: HeatmapData,
    style: HeatmapStyle,
    bins: Int = 6
): List<Color> {
    val values = data.matrix.values
    if (values.isEmpty()) return emptyList()

    val norm: (Float) -> Float = { v -> v.coerceIn(0f, 1f) }

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

    // Collapse adjacent identical colors
    val out = ArrayList<Color>(tmp.size)
    var prev: Color? = null
    for (c in tmp) {
        if (prev == null || prev != c) out += c
        prev = c
    }
    return out
}

/** Draw N equal-width legend strips. */
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
