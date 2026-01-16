package com.grippo.chart.bar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
public fun BarChart(
    modifier: Modifier = Modifier,
    data: BarData,
    style: BarStyle,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val layoutDir = androidx.compose.ui.platform.LocalLayoutDirection.current

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(data.items.size) {
        val idx = selectedIndex
        if (idx != null && idx !in data.items.indices) selectedIndex = null
    }

    // Precompute X-axis label layouts in composition to avoid measuring every draw
    val precomputedXLayouts: List<TextLayoutResult> = when (val xCfg = style.xAxis) {
        is BarStyle.XAxis.None -> emptyList()
        is BarStyle.XAxis.LabelsAdaptive -> data.items.map { e ->
            measurer.measure(AnnotatedString(e.label), xCfg.textStyle)
        }

        is BarStyle.XAxis.LabelsShowAll -> data.items.map { e ->
            measurer.measure(AnnotatedString(e.label), xCfg.textStyle)
        }
    }

    val layout by remember(
        canvasSize,
        data,
        style,
        layoutDir,
        density.density,
        density.fontScale,
        precomputedXLayouts
    ) {
        derivedStateOf {
            if (canvasSize.width <= 0 || canvasSize.height <= 0 || data.items.isEmpty()) {
                null
            } else {
                computeLayout(
                    canvasSize = canvasSize,
                    data = data,
                    style = style,
                    density = density,
                    layoutDir = layoutDir,
                    measurer = measurer,
                    precomputedXLayouts = precomputedXLayouts
                )
            }
        }
    }

    val peek = style.peek
    val hitSlopPx = remember(peek, density) {
        if (peek is BarStyle.Peek.Visible) with(density) { peek.hitSlop.toPx() } else 0f
    }

    Box(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .then(
                if (peek is BarStyle.Peek.Visible) {
                    Modifier.pointerInput(data, style, canvasSize, hitSlopPx) {
                        detectTapGestures { pos ->
                            val l = layout ?: return@detectTapGestures
                            val hit = findHitBarIndex(
                                bars = l.bars,
                                tap = pos,
                                hitSlopPx = hitSlopPx
                            )
                            selectedIndex = when {
                                hit == null -> null
                                selectedIndex == hit -> null
                                else -> hit
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Canvas(Modifier.matchParentSize()) {
            val l = layout ?: return@Canvas

            // ----- Grid -----
            if (style.grid.show) {
                val gw = style.grid.strokeWidth.toPx()
                l.gridLinesY.forEach { y ->
                    drawLine(
                        color = style.grid.color,
                        start = Offset(l.chart.left, y),
                        end = Offset(l.chart.right, y),
                        strokeWidth = gw
                    )
                }
            }

            // ----- Y axis labels + tick marks -----
            when (val yCfg = style.yAxis) {
                is BarStyle.YAxis.Labels -> {
                    l.yAxisLabels.forEach { item ->
                        drawText(item.layout, topLeft = item.topLeft)
                    }
                    l.yTickYs.forEach { y ->
                        drawLine(
                            color = yCfg.tickMarkColor,
                            start = Offset(l.chart.left, y),
                            end = Offset(l.chart.left + yCfg.tickMarkLength.toPx(), y),
                            strokeWidth = yCfg.tickMarkWidth.toPx()
                        )
                    }
                }

                is BarStyle.YAxis.None -> Unit
            }

            // ----- Axis lines -----
            style.yAxisLine?.let { axis ->
                drawLine(
                    color = axis.color,
                    start = Offset(l.chart.left, l.chart.top),
                    end = Offset(l.chart.left, l.chart.bottom),
                    strokeWidth = axis.width.toPx()
                )
            }

            style.xBaseline?.let { base ->
                drawLine(
                    color = base.color,
                    start = Offset(l.chart.left, l.baseY),
                    end = Offset(l.chart.right, l.baseY),
                    strokeWidth = base.width.toPx()
                )
            }

            // ----- Bars (top-only rounding at baseline side) -----
            val cornerPx = style.bars.corner.toPx()
            val strokeW = style.bars.strokeWidth.toPx()

            l.bars.forEachIndexed { i, b ->
                val entry = data.items[i]
                val rect = b.rect
                if (rect.width <= 0f || rect.height <= 0f) return@forEachIndexed

                val brush = style.bars.brushProvider?.invoke(entry, size, rect)
                drawTopOnlyRoundedBar(
                    rect = rect,
                    cornerPx = cornerPx,
                    isPositive = b.isPositive,
                    brush = brush,
                    color = entry.color
                )

                if (strokeW > 0f) {
                    drawTopOnlyRoundedBarStroke(
                        rect = rect,
                        cornerPx = cornerPx,
                        isPositive = b.isPositive,
                        color = style.bars.strokeColor,
                        strokeWidth = strokeW
                    )
                }
            }

            // ----- Peek overlay (selected bar) -----
            val sel = selectedIndex
            if (peek is BarStyle.Peek.Visible && sel != null && sel in l.bars.indices) {
                val b = l.bars[sel]
                val entry = data.items[sel]

                val focus = peek.focusColor ?: entry.color
                val guideWidthPx = peek.guideWidth.toPx()
                val guideDashPx = peek.guideDash.toPx()
                val guideGapPx = peek.guideGap.toPx()
                val focusRadiusPx = peek.focusRadius.toPx()
                val focusRingWidthPx = peek.focusRingWidth.toPx()
                val focusHaloRadiusPx = peek.focusHaloRadius.toPx()

                drawPeekOverlay(
                    point = b.anchor,
                    focusColor = focus,
                    guideColor = peek.guideColor,
                    guideWidthPx = guideWidthPx,
                    guideDashPx = guideDashPx,
                    guideGapPx = guideGapPx,
                    focusRadiusPx = focusRadiusPx,
                    focusRingWidthPx = focusRingWidthPx,
                    focusHaloRadiusPx = focusHaloRadiusPx,
                    chart = l.chart
                )

                drawSelectedBarHighlight(
                    rect = b.rect,
                    cornerPx = cornerPx,
                    isPositive = b.isPositive,
                    focusColor = focus,
                    overlayColor = peek.selectedBarOverlay,
                    strokeColor = peek.selectedBarStrokeColor,
                    strokeWidthPx = peek.selectedBarStrokeWidth.toPx()
                )
            }

            // ----- X axis labels -----
            l.xLabels.forEach { item ->
                drawText(item.layout, topLeft = item.topLeft)
            }

            // ----- Value labels (always on top) -----
            l.valueLabels.forEach { item ->
                drawText(item.layout, topLeft = item.topLeft)
            }
        }

        // ----- Tooltip (Composable overlay) -----
        if (peek is BarStyle.Peek.Visible) {
            val l = layout
            val idx = selectedIndex
            if (l != null && idx != null && idx in data.items.indices && idx in l.bars.indices) {
                val bar = l.bars[idx]
                val entry = data.items[idx]

                val valueText = peek.valueFormatter?.invoke(entry.value, data)
                    ?: formatValue(entry.value, peek.decimals)

                val labelText = if (peek.showLabel) entry.label else null

                val tooltipSize = remember(
                    valueText,
                    labelText,
                    peek,
                    density
                ) {
                    measureTooltipSize(
                        textMeasurer = measurer,
                        density = density,
                        valueText = valueText,
                        labelText = labelText,
                        padH = peek.tooltipPaddingH,
                        padV = peek.tooltipPaddingV
                    )
                }

                BarChartTooltip(
                    anchor = bar.anchor,
                    canvasSize = canvasSize,
                    tooltipSize = tooltipSize,
                    margin = peek.tooltipMargin,
                    background = peek.tooltipBackground,
                    border = peek.tooltipBorder,
                    textColor = peek.tooltipText,
                    corner = peek.tooltipCornerRadius,
                    padH = peek.tooltipPaddingH,
                    padV = peek.tooltipPaddingV,
                    valueText = valueText,
                    labelText = labelText
                )
            }
        }
    }
}

private data class AxisLabelLayout(
    val layout: TextLayoutResult,
    val topLeft: Offset,
)

private data class BarLayout(
    val rect: Rect,
    val centerX: Float,
    val anchor: Offset,
    val isPositive: Boolean,
)

private data class BarChartLayoutResult(
    val chart: Rect,
    val baseY: Float,
    val gridLinesY: List<Float>,
    val yTickYs: List<Float>,
    val yAxisLabels: List<AxisLabelLayout>,
    val xLabels: List<AxisLabelLayout>,
    val bars: List<BarLayout>,
    val valueLabels: List<AxisLabelLayout>,
)

private fun computeLayout(
    canvasSize: IntSize,
    data: BarData,
    style: BarStyle,
    density: Density,
    layoutDir: LayoutDirection,
    measurer: TextMeasurer,
    precomputedXLayouts: List<TextLayoutResult>
): BarChartLayoutResult {
    val widthPx = canvasSize.width.toFloat()
    val heightPx = canvasSize.height.toFloat()

    // ----- Domain -----
    val rawMin = data.items.minOf { it.value }
    val rawMax = data.items.maxOf { it.value }
    val baseSpan = (rawMax - rawMin).coerceAtLeast(1e-6f)
    val headroom = (baseSpan * 0.08f).coerceAtLeast(1e-3f)
    val yMinTarget = 0f
    var yMaxTarget = rawMax + headroom

    // Predict vertical space needed for Above labels and enlarge max if necessary
    val valuePadPx = with(density) { style.layout.valueLabelSpacing.toPx() }
    val valueAboveMaxH = when (val vCfg = style.values) {
        is BarStyle.Values.Above -> data.items.maxOf {
            val txt = vCfg.formatter(it.value, data)
            measurer.measure(AnnotatedString(txt), vCfg.textStyle).size.height
        }.toFloat()

        else -> 0f
    }

    if (valueAboveMaxH > 0f) {
        val bottomCandidate = when (val xCfg = style.xAxis) {
            is BarStyle.XAxis.LabelsAdaptive -> precomputedXLayouts.maxOfOrNull { it.size.height }
                ?.toFloat() ?: 0f

            is BarStyle.XAxis.LabelsShowAll -> precomputedXLayouts.maxOfOrNull { it.size.height }
                ?.toFloat() ?: 0f

            is BarStyle.XAxis.None -> 0f
        }
        val chartHApprox = (heightPx - bottomCandidate).coerceAtLeast(1f)
        val k = ((valueAboveMaxH + valuePadPx) / chartHApprox).coerceIn(0f, 0.95f)
        val requiredMax = if (k < 0.95f) rawMax / (1f - k) else rawMax + headroom
        yMaxTarget = max(yMaxTarget, requiredMax)
    }

    val ticksY = when (val yCfg = style.yAxis) {
        is BarStyle.YAxis.Labels -> max(1, yCfg.ticks)
        is BarStyle.YAxis.None -> 5
    }

    fun niceStep(span: Float, ticks: Int): Float {
        val raw = (span / ticks).coerceAtLeast(1e-6f)
        val exp = floor(log10(raw.toDouble())).toInt()
        val base = 10.0.pow(exp.toDouble()).toFloat()
        val mul = (raw / base).toFloat()
        val niceMul = when {
            mul <= 1f -> 1f
            mul <= 2f -> 2f
            mul <= 5f -> 5f
            else -> 10f
        }
        return niceMul * base
    }

    val spanRaw = (yMaxTarget - yMinTarget).coerceAtLeast(1e-6f)
    val yStep = niceStep(spanRaw, ticksY)
    val maxY = ceil(yMaxTarget / yStep) * yStep
    val minY = floor(yMinTarget / yStep) * yStep
    val spanY = (maxY - minY).coerceAtLeast(1e-6f)
    val yTickCount = ((maxY - minY) / yStep).toInt().coerceAtLeast(1)

    // ----- Gutters -----
    val yAxisLabelPadPx = with(density) { style.layout.yAxisLabelSpacing.toPx() }

    val yAxisLabelsText = when (val yCfg = style.yAxis) {
        is BarStyle.YAxis.Labels -> (0..yTickCount).map { i ->
            yCfg.formatter(minY + i * yStep, data) to yCfg.textStyle
        }

        is BarStyle.YAxis.None -> emptyList()
    }

    val leftGutter = when (style.yAxis) {
        is BarStyle.YAxis.Labels -> {
            val maxLabelW = yAxisLabelsText.maxOf { (text, ts) ->
                measurer.measure(AnnotatedString(text), ts).size.width
            }
            maxLabelW + yAxisLabelPadPx
        }

        is BarStyle.YAxis.None -> 0f
    }

    val bottomGutter = when (style.xAxis) {
        is BarStyle.XAxis.LabelsAdaptive,
        is BarStyle.XAxis.LabelsShowAll -> (precomputedXLayouts.maxOfOrNull { it.size.height }
            ?: 0).toFloat()

        is BarStyle.XAxis.None -> 0f
    }

    val padding = style.layout.chartPadding
    val startPadding = with(density) {
        when (layoutDir) {
            LayoutDirection.Ltr -> padding.start.toPx()
            LayoutDirection.Rtl -> padding.end.toPx()
        }
    }
    val endPadding = with(density) {
        when (layoutDir) {
            LayoutDirection.Ltr -> padding.end.toPx()
            LayoutDirection.Rtl -> padding.start.toPx()
        }
    }
    val topPadding = with(density) { padding.top.toPx() }
    val bottomPadding = with(density) { padding.bottom.toPx() }

    val chart = Rect(
        left = leftGutter + startPadding,
        top = 0f + topPadding,
        right = widthPx - 0f - endPadding,
        bottom = heightPx - bottomGutter - bottomPadding
    )

    val chartW = chart.width.coerceAtLeast(0f)
    val chartH = chart.height.coerceAtLeast(0f)
    if (chartW <= 0f || chartH <= 0f) {
        return BarChartLayoutResult(
            chart = Rect(0f, 0f, 0f, 0f),
            baseY = 0f,
            gridLinesY = emptyList(),
            yTickYs = emptyList(),
            yAxisLabels = emptyList(),
            xLabels = emptyList(),
            bars = emptyList(),
            valueLabels = emptyList()
        )
    }

    val barsAreaLeft = chart.left + with(density) { style.layout.barsAxisInsetStart.toPx() }
    val barsAreaRight = chart.right - with(density) { style.layout.barsAxisInsetEnd.toPx() }
    val barsAreaWidth = (barsAreaRight - barsAreaLeft).coerceAtLeast(0f)

    fun mapY(v: Float): Float = chart.bottom - (v - minY) / spanY * chartH

    val baseY = mapY(0f)

    // ----- Grid lines -----
    val gridLinesY = (0..yTickCount).map { i ->
        mapY(minY + i * yStep)
    }

    // ----- Y axis label layouts + positions -----
    val yAxisLabelLayouts = when (style.yAxis) {
        is BarStyle.YAxis.Labels -> yAxisLabelsText.map { (text, ts) ->
            measurer.measure(AnnotatedString(text), ts)
        }

        is BarStyle.YAxis.None -> emptyList()
    }

    val yTickYs = (0..yTickCount).map { i ->
        mapY(minY + i * yStep)
    }

    val yAxisLabels = if (style.yAxis is BarStyle.YAxis.Labels) {
        yTickYs.mapIndexed { i, y ->
            val layout = yAxisLabelLayouts[i]
            val x = (chart.left - yAxisLabelPadPx - layout.size.width).coerceAtLeast(0f)
            val top = (y - layout.size.height / 2f)
                .coerceIn(chart.top, chart.bottom - layout.size.height.toFloat())
            AxisLabelLayout(layout = layout, topLeft = Offset(x, top))
        }
    } else {
        emptyList()
    }

    // ----- Bars sizing -----
    val nBars = data.items.size
    val sizing = style.bars.sizing

    val (bw, sp, startX) = when (sizing) {
        is BarStyle.BarsSizing.AutoEqualBarsAndGaps -> {
            val maxWidthValue = sizing.maxBarWidth.value
            val maxWidthPx = when {
                maxWidthValue.isNaN() -> Float.POSITIVE_INFINITY
                maxWidthValue.isInfinite() -> Float.POSITIVE_INFINITY
                else -> with(density) { sizing.maxBarWidth.toPx() }
            }

            val ratio = if (nBars <= 1) {
                0f
            } else {
                val wEqual = barsAreaWidth / (2f * nBars - 1f)
                val midPx = with(density) { sizing.midThresholdDp.toPx() }
                val densePx = with(density) { sizing.denseThresholdDp.toPx() }

                when {
                    wEqual < densePx -> sizing.denseRatio
                    wEqual < midPx -> sizing.midRatio
                    else -> 1f
                }.coerceIn(0f, 1f)
            }

            val baseWidth = when {
                nBars <= 1 -> barsAreaWidth
                ratio == 1f -> barsAreaWidth / (2f * nBars - 1f)
                else -> barsAreaWidth / (nBars + (nBars - 1) * ratio)
            }

            val width = baseWidth
                .coerceAtLeast(0f)
                .coerceAtMost(min(maxWidthPx, barsAreaWidth))

            val gap = if (nBars <= 1) 0f else ratio * width
            val totalWidth = if (nBars <= 0) 0f else nBars * width + (nBars - 1) * gap
            val startOffset = ((barsAreaWidth - totalWidth) / 2f).coerceAtLeast(0f)
            Triple(width, gap, barsAreaLeft + startOffset)
        }

        is BarStyle.BarsSizing.FixedBarWidth -> {
            val desired = with(density) { sizing.width.toPx() }
            val fitted = if (nBars > 0) min(desired, barsAreaWidth / nBars) else 0f
            val gap = if (nBars > 1) (barsAreaWidth - fitted * nBars) / (nBars - 1) else 0f
            Triple(fitted, gap.coerceAtLeast(0f), barsAreaLeft)
        }

        is BarStyle.BarsSizing.Explicit -> {
            val bw0 = with(density) { sizing.width.toPx() }
            val sp0 = with(density) { sizing.spacing.toPx() }
            Triple(bw0, sp0, barsAreaLeft)
        }

        is BarStyle.BarsSizing.GapAsBarRatio -> {
            val r = sizing.ratio.coerceAtLeast(0f)
            val w = if (nBars == 0) 0f else barsAreaWidth / (nBars + (nBars - 1) * r)
            val g = r * w
            Triple(w, g, barsAreaLeft)
        }
    }

    val minBarHeightPx = with(density) { style.layout.minBarHeight.toPx() }
    val baselineSpacingPx = with(density) { style.layout.baselineSpacing.toPx() }

    // ----- Bars rects -----
    val bars = data.items.mapIndexed { i, e ->
        val left = startX + i * (bw + sp)
        val yVal = e.value

        var top = if (yVal >= 0f) mapY(yVal) else baseY
        var bottom = if (yVal >= 0f) baseY else mapY(yVal)

        if (baselineSpacingPx > 0f && yVal != 0f) {
            if (yVal > 0f) {
                bottom = (baseY - baselineSpacingPx).coerceAtMost(baseY)
                if (bottom < top) bottom = top
            } else {
                top = (baseY + baselineSpacingPx).coerceAtLeast(baseY)
                if (bottom < top) top = bottom
            }
        }

        if (minBarHeightPx > 0f && yVal != 0f) {
            val h = abs(bottom - top)
            if (h < minBarHeightPx) {
                if (yVal > 0f) {
                    top = (bottom - minBarHeightPx).coerceAtLeast(chart.top)
                } else {
                    bottom = (top + minBarHeightPx).coerceAtMost(chart.bottom)
                }
            }
        }

        val rect = Rect(left, top, left + bw, bottom)
        val cx = left + bw / 2f

        val isPositive = yVal >= 0f
        val anchor = if (isPositive) Offset(cx, rect.top) else Offset(cx, rect.bottom)

        BarLayout(
            rect = rect,
            centerX = cx,
            anchor = anchor,
            isPositive = isPositive
        )
    }

    // ----- Value labels -----
    val valueLabels = when (val vCfg = style.values) {
        is BarStyle.Values.None -> emptyList()
        is BarStyle.Values.Above -> {
            data.items.mapIndexedNotNull { i, e ->
                val bar = bars.getOrNull(i) ?: return@mapIndexedNotNull null
                val txt = vCfg.formatter(e.value, data)
                val layout = measurer.measure(AnnotatedString(txt), vCfg.textStyle)
                val y = (bar.rect.top - valuePadPx - layout.size.height).coerceAtLeast(0f)
                val x = bar.rect.left + (bar.rect.width - layout.size.width) / 2f
                AxisLabelLayout(layout = layout, topLeft = Offset(x, y))
            }
        }
    }

    // ----- X labels -----
    val xLabels = when (val xCfg = style.xAxis) {
        is BarStyle.XAxis.None -> emptyList()

        is BarStyle.XAxis.LabelsShowAll -> {
            bars.mapIndexed { i, b ->
                val maxWidth = b.rect.width
                val measured = measureEllipsized(
                    measurer = measurer,
                    text = data.items[i].label,
                    style = xCfg.textStyle,
                    maxWidthPx = maxWidth
                )
                val w = measured.size.width.toFloat()
                val left = (b.centerX - w / 2f).coerceIn(chart.left, chart.right - w)
                AxisLabelLayout(measured, Offset(left, chart.bottom))
            }
        }

        is BarStyle.XAxis.LabelsAdaptive -> {
            val minGapPx = with(density) { xCfg.minGapDp.toPx() }
            val layouts = precomputedXLayouts

            data class Bound(val left: Float, val right: Float, val layout: TextLayoutResult)

            val bounds = bars.indices.associateWith { i ->
                val lay = layouts[i]
                val w = lay.size.width.toFloat()
                val left = (bars[i].centerX - w / 2f).coerceIn(chart.left, chart.right - w)
                Bound(left = left, right = left + w, layout = lay)
            }

            val order = bars.indices.toList()
            val selected = mutableListOf<Int>()
            var lastRight = Float.NEGATIVE_INFINITY

            run {
                for (i in order) {
                    val b = bounds[i] ?: continue
                    selected.add(i)
                    lastRight = b.right
                    break
                }
            }

            for (k in 1 until order.lastIndex) {
                val i = order[k]
                val b = bounds[i] ?: continue
                if (b.left >= lastRight + minGapPx) {
                    selected.add(i)
                    lastRight = b.right
                }
            }

            val lastIdx = order.lastOrNull()
            val lastB = lastIdx?.let { bounds[it] }
            if (lastIdx != null && lastB != null) {
                while (selected.isNotEmpty()) {
                    val tail = bounds[selected.last()] ?: break
                    if (lastB.left >= tail.right + minGapPx) break
                    if (selected.size == 1) break
                    selected.removeAt(selected.lastIndex)
                }
                selected.add(lastIdx)
            }

            selected.distinct().sorted().mapNotNull { i ->
                val b = bounds[i] ?: return@mapNotNull null
                AxisLabelLayout(b.layout, Offset(b.left, chart.bottom))
            }
        }
    }

    return BarChartLayoutResult(
        chart = chart,
        baseY = baseY,
        gridLinesY = gridLinesY,
        yTickYs = yTickYs,
        yAxisLabels = yAxisLabels,
        xLabels = xLabels,
        bars = bars,
        valueLabels = valueLabels
    )
}

private fun findHitBarIndex(
    bars: List<BarLayout>,
    tap: Offset,
    hitSlopPx: Float
): Int? {
    if (bars.isEmpty()) return null

    var bestIdx: Int? = null
    var bestDist = Float.POSITIVE_INFINITY

    for (i in bars.indices) {
        val r = bars[i].rect
        val hit = Rect(
            left = r.left - hitSlopPx,
            top = r.top - hitSlopPx,
            right = r.right + hitSlopPx,
            bottom = r.bottom + hitSlopPx
        )

        if (hit.contains(tap)) {
            val cx = (r.left + r.right) * 0.5f
            val cy = (r.top + r.bottom) * 0.5f
            val dx = tap.x - cx
            val dy = tap.y - cy
            val d = dx * dx + dy * dy
            if (d < bestDist) {
                bestDist = d
                bestIdx = i
            }
        }
    }

    return bestIdx
}

private fun DrawScope.drawTopOnlyRoundedBar(
    rect: Rect,
    cornerPx: Float,
    isPositive: Boolean,
    brush: Brush?,
    color: androidx.compose.ui.graphics.Color
) {
    val r = cornerPx.coerceAtLeast(0f).coerceAtMost(min(rect.width, rect.height) / 2f)
    val rr = RoundRect(
        rect = rect,
        topLeft = CornerRadius(if (isPositive) r else 0f, if (isPositive) r else 0f),
        topRight = CornerRadius(if (isPositive) r else 0f, if (isPositive) r else 0f),
        bottomRight = CornerRadius(if (!isPositive) r else 0f, if (!isPositive) r else 0f),
        bottomLeft = CornerRadius(if (!isPositive) r else 0f, if (!isPositive) r else 0f)
    )

    val path = Path().apply { addRoundRect(rr) }

    if (brush != null) {
        drawPath(path = path, brush = brush)
    } else {
        drawPath(path = path, color = color)
    }
}

private fun DrawScope.drawTopOnlyRoundedBarStroke(
    rect: Rect,
    cornerPx: Float,
    isPositive: Boolean,
    color: androidx.compose.ui.graphics.Color,
    strokeWidth: Float
) {
    if (strokeWidth <= 0f) return

    val r = cornerPx.coerceAtLeast(0f).coerceAtMost(min(rect.width, rect.height) / 2f)
    val rr = RoundRect(
        rect = rect,
        topLeft = CornerRadius(if (isPositive) r else 0f, if (isPositive) r else 0f),
        topRight = CornerRadius(if (isPositive) r else 0f, if (isPositive) r else 0f),
        bottomRight = CornerRadius(if (!isPositive) r else 0f, if (!isPositive) r else 0f),
        bottomLeft = CornerRadius(if (!isPositive) r else 0f, if (!isPositive) r else 0f)
    )

    val path = Path().apply { addRoundRect(rr) }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth.coerceAtLeast(1f),
            cap = StrokeCap.Round
        )
    )
}

private fun DrawScope.drawSelectedBarHighlight(
    rect: Rect,
    cornerPx: Float,
    isPositive: Boolean,
    focusColor: androidx.compose.ui.graphics.Color,
    overlayColor: androidx.compose.ui.graphics.Color?,
    strokeColor: androidx.compose.ui.graphics.Color?,
    strokeWidthPx: Float
) {
    val fill = overlayColor ?: focusColor.copy(alpha = 0.12f)
    val stroke = strokeColor ?: focusColor.copy(alpha = 0.72f)
    val sw = strokeWidthPx.coerceAtLeast(0f)
    if (rect.width <= 0f || rect.height <= 0f) return

    drawTopOnlyRoundedBar(
        rect = rect,
        cornerPx = cornerPx,
        isPositive = isPositive,
        brush = null,
        color = fill
    )

    if (sw > 0f) {
        drawTopOnlyRoundedBarStroke(
            rect = rect,
            cornerPx = cornerPx,
            isPositive = isPositive,
            color = stroke,
            strokeWidth = sw
        )
    }
}

private fun DrawScope.drawPeekOverlay(
    point: Offset,
    focusColor: androidx.compose.ui.graphics.Color,
    guideColor: androidx.compose.ui.graphics.Color,
    guideWidthPx: Float,
    guideDashPx: Float,
    guideGapPx: Float,
    focusRadiusPx: Float,
    focusRingWidthPx: Float,
    focusHaloRadiusPx: Float,
    chart: Rect,
) {
    val guide = Path().apply {
        moveTo(point.x, chart.top)
        lineTo(point.x, chart.bottom)
    }

    drawPath(
        path = guide,
        color = guideColor,
        style = Stroke(
            width = guideWidthPx.coerceAtLeast(1f),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(
                    guideDashPx.coerceAtLeast(1f),
                    guideGapPx.coerceAtLeast(1f)
                ),
                phase = 0f
            )
        )
    )

    val halo = Brush.radialGradient(
        colors = listOf(
            focusColor.copy(alpha = 0.20f),
            focusColor.copy(alpha = 0f)
        ),
        center = point,
        radius = focusHaloRadiusPx.coerceAtLeast(focusRadiusPx * 2f)
    )

    drawCircle(brush = halo, radius = focusHaloRadiusPx, center = point)
    drawCircle(
        color = focusColor.copy(alpha = 0.48f),
        radius = (focusRadiusPx * 1.9f).coerceAtLeast(focusRadiusPx + 1f),
        center = point,
        style = Stroke(width = focusRingWidthPx.coerceAtLeast(1f), cap = StrokeCap.Round)
    )
    drawCircle(color = focusColor, radius = focusRadiusPx.coerceAtLeast(1f), center = point)
}

private fun measureEllipsized(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    maxWidthPx: Float
): TextLayoutResult {
    var layout = measurer.measure(AnnotatedString(text), style)
    if (layout.size.width <= maxWidthPx) return layout

    val ellipsis = "…"
    var cut = text.length
    while (cut > 0) {
        val candidate = text.take(cut) + ellipsis
        val candidateLayout = measurer.measure(AnnotatedString(candidate), style)
        if (candidateLayout.size.width <= maxWidthPx) return candidateLayout
        cut--
    }
    return measurer.measure(AnnotatedString(ellipsis), style)
}

private fun formatValue(value: Float, decimals: Int): String {
    if (decimals <= 0) return value.roundToInt().toString()

    val scale = 10.0.pow(decimals.toDouble())
    val v = round(value.toDouble() * scale) / scale

    val sign = if (v < 0.0) "-" else ""
    val absV = abs(v)

    val intPart = floor(absV).toLong()
    var frac = absV - intPart.toDouble()

    var fracInt = round(frac * scale).toLong()
    var ip = intPart
    if (fracInt >= scale.toLong()) {
        ip += 1L
        fracInt = 0L
    }

    val fracStr = fracInt.toString().padStart(decimals, '0')
    return "$sign$ip.$fracStr"
}

private fun measureTooltipSize(
    textMeasurer: TextMeasurer,
    density: Density,
    valueText: String,
    labelText: String?,
    padH: Dp,
    padV: Dp,
): IntSize {
    val value = textMeasurer.measure(
        text = AnnotatedString(valueText),
        style = TextStyle(fontSize = 12.sp, textAlign = TextAlign.Center)
    ).size

    val label = if (!labelText.isNullOrBlank()) {
        textMeasurer.measure(
            text = AnnotatedString(labelText),
            style = TextStyle(fontSize = 10.sp, textAlign = TextAlign.Center)
        ).size
    } else {
        null
    }

    val padHPx = with(density) { padH.toPx() }
    val padVPx = with(density) { padV.toPx() }
    val gapPx = if (label != null) with(density) { 2.dp.toPx() } else 0f

    val contentW = max(value.width, label?.width ?: 0)
    val contentH = value.height + (label?.height ?: 0) + gapPx.roundToInt()

    val w = ceil(contentW + padHPx * 2f + 2f).toInt()
    val h = ceil(contentH + padVPx * 2f + 2f).toInt()

    return IntSize(w, h)
}

@Composable
private fun BarChartTooltip(
    anchor: Offset,
    canvasSize: IntSize,
    tooltipSize: IntSize,
    margin: Dp,
    background: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    corner: Dp,
    padH: Dp,
    padV: Dp,
    valueText: String,
    labelText: String?,
) {
    val density = LocalDensity.current
    val marginPx = with(density) { margin.toPx() }.roundToInt()

    val w = tooltipSize.width
    val h = tooltipSize.height
    if (w <= 0 || h <= 0 || canvasSize.width <= 0 || canvasSize.height <= 0) return

    val ax = anchor.x.roundToInt()
    val ay = anchor.y.roundToInt()

    fun clampX(x: Int): Int = x.coerceIn(0, max(0, canvasSize.width - w))
    fun clampY(y: Int): Int = y.coerceIn(0, max(0, canvasSize.height - h))

    fun fits(x: Int, y: Int): Boolean {
        return x >= 0 && y >= 0 && x + w <= canvasSize.width && y + h <= canvasSize.height
    }

    data class Candidate(val x: Int, val y: Int)

    val top = Candidate(
        x = ax - w / 2,
        y = ay - h - marginPx
    )
    val bottom = Candidate(
        x = ax - w / 2,
        y = ay + marginPx
    )
    val right = Candidate(
        x = ax + marginPx,
        y = ay - h / 2
    )
    val left = Candidate(
        x = ax - w - marginPx,
        y = ay - h / 2
    )

    val candidates = listOf(top, bottom, right, left)

    val chosen = candidates.firstOrNull { fits(it.x, it.y) } ?: run {
        candidates.minBy { c ->
            val cx = clampX(c.x)
            val cy = clampY(c.y)
            abs(cx - c.x) + abs(cy - c.y)
        }
    }

    val x = clampX(chosen.x)
    val y = clampY(chosen.y)

    Box(
        modifier = Modifier
            .offset { IntOffset(x, y) }
            .background(
                background,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(corner)
            )
            .border(
                width = 1.dp,
                color = border,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(corner)
            )
            .padding(horizontal = padH, vertical = padV)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.foundation.text.BasicText(
                text = valueText,
                style = TextStyle(
                    color = textColor,
                    fontSize = 12.sp
                )
            )
            if (!labelText.isNullOrBlank()) {
                androidx.compose.foundation.text.BasicText(
                    text = labelText,
                    style = TextStyle(
                        color = textColor.copy(alpha = 0.80f),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
