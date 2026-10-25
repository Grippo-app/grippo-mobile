package com.grippo.chart.bar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
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
    val layoutDir = LocalLayoutDirection.current
    val peek = style.peek
    val isPeekVisible = peek is BarStyle.Peek.Visible

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedIndex by remember(data, isPeekVisible) { mutableStateOf<Int?>(null) }

    val precomputedXLayouts = remember(data, style.xAxis, measurer) {
        when (val xAxis = style.xAxis) {
            is BarStyle.XAxis.None -> emptyList()
            is BarStyle.XAxis.LabelsAdaptive -> data.items.map { entry ->
                measurer.measure(AnnotatedString(entry.label), xAxis.textStyle)
            }

            is BarStyle.XAxis.LabelsShowAll -> data.items.map { entry ->
                measurer.measure(AnnotatedString(entry.label), xAxis.textStyle)
            }
        }
    }

    val layout = remember(
        canvasSize,
        data,
        style,
        layoutDir,
        density.density,
        density.fontScale,
        precomputedXLayouts,
        measurer,
    ) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0 ||
            data.items.isEmpty() || data.items.any { !it.value.isFinite() }
        ) {
            null
        } else {
            computeLayout(
                canvasSize = canvasSize,
                data = data,
                style = style,
                density = density,
                layoutDir = layoutDir,
                measurer = measurer,
                precomputedXLayouts = precomputedXLayouts,
            )
        }
    }

    val hitSlopPx = if (peek is BarStyle.Peek.Visible) with(density) { peek.hitSlop.toPx() } else 0f

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
                    }.pointerInput(data, style, canvasSize) {
                        var isScrubbing = false
                        detectHorizontalDragGestures(
                            onDragStart = { position ->
                                val currentLayout = layout
                                isScrubbing = currentLayout?.chart?.contains(position) == true
                                if (isScrubbing && currentLayout != null) {
                                    selectedIndex = findNearestBarIndex(
                                        bars = currentLayout.bars,
                                        positionX = position.x,
                                    )
                                }
                            },
                            onDragEnd = {
                                isScrubbing = false
                            },
                            onDragCancel = {
                                isScrubbing = false
                            },
                            onHorizontalDrag = { change, _ ->
                                val currentLayout = layout
                                if (isScrubbing && currentLayout != null) {
                                    selectedIndex = findNearestBarIndex(
                                        bars = currentLayout.bars,
                                        positionX = change.position.x,
                                    )
                                }
                            },
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Canvas(Modifier.matchParentSize()) {
            val l = layout ?: return@Canvas

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

            val cornerPx = style.bars.corner.toPx()
            val strokeW = style.bars.strokeWidth.toPx()

            l.bars.forEachIndexed { i, b ->
                val entry = data.items[i]
                val rect = b.rect
                if (rect.width <= 0f || rect.height <= 0f) {
                    return@forEachIndexed
                }

                val brush = style.bars.brushProvider?.invoke(entry, size, rect)
                drawRoundedBar(
                    rect = rect,
                    cornerPx = cornerPx,
                    brush = brush,
                    color = entry.color
                )

                if (strokeW > 0f) {
                    drawRoundedBarStroke(
                        rect = rect,
                        cornerPx = cornerPx,
                        color = style.bars.strokeColor,
                        strokeWidth = strokeW
                    )
                }
            }

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
                    focusColor = focus,
                    overlayColor = peek.selectedBarOverlay,
                    strokeColor = peek.selectedBarStrokeColor,
                    strokeWidthPx = peek.selectedBarStrokeWidth.toPx()
                )
            }

            l.xLabels.forEach { item ->
                drawText(item.layout, topLeft = item.topLeft)
            }

            l.valueLabels.forEach { item ->
                drawText(item.layout, topLeft = item.topLeft)
            }
        }

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
                    density,
                    canvasSize.width,
                    measurer,
                ) {
                    measureTooltipSize(
                        textMeasurer = measurer,
                        density = density,
                        valueText = valueText,
                        labelText = labelText,
                        style = peek,
                        canvasWidth = canvasSize.width,
                    )
                }

                BarChartTooltip(
                    anchor = bar.anchor,
                    canvasSize = canvasSize,
                    tooltipSize = tooltipSize,
                    style = peek,
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

private data class XAxisLabelBounds(
    val left: Float,
    val right: Float,
    val layout: TextLayoutResult,
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

    val rawMin = data.items.minOf { it.value }.toDouble()
    val rawMax = data.items.maxOf { it.value }.toDouble()
    val baseSpan = (rawMax - rawMin).coerceAtLeast(1e-6)
    val headroom = (baseSpan * 0.08).coerceAtLeast(1e-3)
    val yMinTarget = if (rawMin < 0.0) rawMin - headroom else 0.0
    var yMaxTarget = if (rawMax >= 0.0) rawMax + headroom else 0.0

    val valuePadPx = with(density) { style.layout.valueLabelSpacing.toPx() }
    val valueAboveMaxH = when (val vCfg = style.values) {
        is BarStyle.Values.Above -> data.items.maxOf {
            val txt = vCfg.formatter(it.value, data)
            measurer.measure(AnnotatedString(txt), vCfg.textStyle).size.height
        }.toFloat()

        else -> 0f
    }

    if (valueAboveMaxH > 0f) {
        val bottomCandidate = when (style.xAxis) {
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

    val spanRaw = (yMaxTarget - yMinTarget).coerceAtLeast(1e-6)
    val yStep = niceStep(spanRaw, ticksY)
    val maxY = ceil(yMaxTarget / yStep) * yStep
    val minY = floor(yMinTarget / yStep) * yStep
    val spanY = (maxY - minY).coerceAtLeast(1e-6)
    val yTickCount = ((maxY - minY) / yStep).toInt().coerceAtLeast(1)

    val yAxisLabelPadPx = with(density) { style.layout.yAxisLabelSpacing.toPx() }

    val yAxisLabelsText = when (val yCfg = style.yAxis) {
        is BarStyle.YAxis.Labels -> (0..yTickCount).map { i ->
            val tickValue = (minY + i * yStep)
                .coerceIn(-Float.MAX_VALUE.toDouble(), Float.MAX_VALUE.toDouble())
                .toFloat()
            yCfg.formatter(tickValue, data) to yCfg.textStyle
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

    val xAxisLabelSpacingPx = with(density) { style.layout.xAxisLabelSpacing.toPx() }
    val bottomGutter = when (style.xAxis) {
        is BarStyle.XAxis.LabelsAdaptive,
        is BarStyle.XAxis.LabelsShowAll -> (precomputedXLayouts.maxOfOrNull { it.size.height }
            ?: 0).toFloat() + xAxisLabelSpacingPx

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

    val baseY = mapY(value = 0.0, chart = chart, minY = minY, spanY = spanY)

    val gridLinesY = (0..yTickCount).map { i ->
        mapY(value = minY + i * yStep, chart = chart, minY = minY, spanY = spanY)
    }

    val yAxisLabelLayouts = when (style.yAxis) {
        is BarStyle.YAxis.Labels -> yAxisLabelsText.map { (text, ts) ->
            measurer.measure(AnnotatedString(text), ts)
        }

        is BarStyle.YAxis.None -> emptyList()
    }

    val yTickYs = (0..yTickCount).map { i ->
        mapY(value = minY + i * yStep, chart = chart, minY = minY, spanY = spanY)
    }

    val yAxisLabels = if (style.yAxis is BarStyle.YAxis.Labels) {
        yTickYs.mapIndexed { i, y ->
            val layout = yAxisLabelLayouts[i]
            val x = (chart.left - yAxisLabelPadPx - layout.size.width).coerceAtLeast(0f)
            val top = (y - layout.size.height / 2f)
                .coerceIn(chart.top, max(chart.top, chart.bottom - layout.size.height.toFloat()))
            AxisLabelLayout(layout = layout, topLeft = Offset(x, top))
        }
    } else {
        emptyList()
    }

    val nBars = data.items.size
    val sizing = style.bars.sizing

    val (bw, sp, startX) = when (sizing) {
        is BarStyle.BarsSizing.EvenSlots -> {
            val slotWidth = barsAreaWidth / nBars
            val fraction = if (sizing.widthFraction.isFinite()) {
                sizing.widthFraction.coerceIn(0f, 1f)
            } else {
                0f
            }
            val maxWidthPx = with(density) { sizing.maxBarWidth.toPx() }
            val widthCap = if (maxWidthPx.isNaN()) 0f else maxWidthPx.coerceAtLeast(0f)
            val width = min(slotWidth * fraction, widthCap)
            Triple(
                first = width,
                second = slotWidth - width,
                third = barsAreaLeft + (slotWidth - width) / 2f,
            )
        }

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

    val bars = data.items.mapIndexed { i, e ->
        val left = startX + i * (bw + sp)
        val yVal = e.value

        val valueY = mapY(value = yVal.toDouble(), chart = chart, minY = minY, spanY = spanY)
        var top = if (yVal >= 0f) valueY else baseY
        var bottom = if (yVal >= 0f) baseY else valueY

        if (baselineSpacingPx > 0f && yVal != 0f) {
            if (yVal > 0f) {
                bottom = (baseY - baselineSpacingPx).coerceAtMost(baseY)
                if (bottom < top) {
                    bottom = top
                }
            } else {
                top = (baseY + baselineSpacingPx).coerceAtLeast(baseY)
                if (bottom < top) {
                    top = bottom
                }
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
        )
    }

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

    val xLabels = when (val xCfg = style.xAxis) {
        is BarStyle.XAxis.None -> emptyList()

        is BarStyle.XAxis.LabelsShowAll -> {
            bars.mapIndexed { i, b ->
                val maxWidth = max(b.rect.width, barsAreaWidth / nBars).coerceAtMost(chartW)
                val measured = measureEllipsized(
                    measurer = measurer,
                    text = data.items[i].label,
                    style = xCfg.textStyle,
                    maxWidthPx = maxWidth
                )
                val w = measured.size.width.toFloat()
                val left = (b.centerX - w / 2f).coerceIn(chart.left, max(chart.left, chart.right - w))
                AxisLabelLayout(
                    layout = measured,
                    topLeft = Offset(x = left, y = chart.bottom + xAxisLabelSpacingPx),
                )
            }
        }

        is BarStyle.XAxis.LabelsAdaptive -> {
            val minGapPx = with(density) { xCfg.minGapDp.toPx() }
            val bounds = bars.mapIndexed { index, bar ->
                val original = precomputedXLayouts[index]
                val measured = if (original.size.width > chartW) {
                    measureEllipsized(
                        measurer = measurer,
                        text = data.items[index].label,
                        style = xCfg.textStyle,
                        maxWidthPx = chartW,
                    )
                } else {
                    original
                }
                val width = measured.size.width.toFloat()
                val left = (bar.centerX - width / 2f)
                    .coerceIn(chart.left, max(chart.left, chart.right - width))
                XAxisLabelBounds(left = left, right = left + width, layout = measured)
            }

            val selected = mutableListOf<Int>()
            for (index in bounds.indices) {
                val bound = bounds[index]
                if (index == bounds.lastIndex) {
                    while (selected.isNotEmpty() &&
                        bound.left < bounds[selected.last()].right + minGapPx
                    ) {
                        selected.removeAt(selected.lastIndex)
                    }
                    selected.add(index)
                } else if (selected.isEmpty() ||
                    bound.left >= bounds[selected.last()].right + minGapPx
                ) {
                    selected.add(index)
                }
            }

            selected.map { index ->
                val bound = bounds[index]
                AxisLabelLayout(
                    layout = bound.layout,
                    topLeft = Offset(x = bound.left, y = chart.bottom + xAxisLabelSpacingPx),
                )
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

private fun niceStep(span: Double, ticks: Int): Double {
    val raw = (span / ticks).coerceAtLeast(1e-6)
    val exponent = floor(log10(raw)).toInt()
    val base = 10.0.pow(exponent.toDouble())
    val multiplier = raw / base
    val roundedMultiplier = when {
        multiplier <= 1.0 -> 1.0
        multiplier <= 2.0 -> 2.0
        multiplier <= 5.0 -> 5.0
        else -> 10.0
    }
    return roundedMultiplier * base
}

private fun mapY(value: Double, chart: Rect, minY: Double, spanY: Double): Float {
    return chart.bottom - ((value - minY) / spanY * chart.height).toFloat()
}

private fun findHitBarIndex(
    bars: List<BarLayout>,
    tap: Offset,
    hitSlopPx: Float
): Int? {
    if (bars.isEmpty()) {
        return null
    }

    var nearestIndex: Int? = null
    var nearestDistance = Float.POSITIVE_INFINITY

    for (index in bars.indices) {
        val rect = bars[index].rect
        val hit = Rect(
            left = rect.left - hitSlopPx,
            top = rect.top - hitSlopPx,
            right = rect.right + hitSlopPx,
            bottom = rect.bottom + hitSlopPx,
        )

        if (hit.contains(tap)) {
            val distance = abs(bars[index].centerX - tap.x)
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearestIndex = index
            }
        }
    }

    return nearestIndex
}

private fun findNearestBarIndex(bars: List<BarLayout>, positionX: Float): Int? {
    var nearestIndex: Int? = null
    var nearestDistance = Float.POSITIVE_INFINITY
    for (index in bars.indices) {
        val distance = abs(bars[index].centerX - positionX)
        if (distance < nearestDistance) {
            nearestDistance = distance
            nearestIndex = index
        }
    }
    return nearestIndex
}

private fun DrawScope.drawRoundedBar(
    rect: Rect,
    cornerPx: Float,
    brush: Brush?,
    color: Color,
) {
    val radius = cornerPx.coerceAtLeast(0f).coerceAtMost(min(rect.width, rect.height) / 2f)
    val cornerRadius = CornerRadius(x = radius, y = radius)

    if (brush != null) {
        drawRoundRect(
            brush = brush,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = cornerRadius,
        )
    } else {
        drawRoundRect(
            color = color,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = cornerRadius,
        )
    }
}

private fun DrawScope.drawRoundedBarStroke(
    rect: Rect,
    cornerPx: Float,
    color: Color,
    strokeWidth: Float,
) {
    if (strokeWidth <= 0f) {
        return
    }
    val radius = cornerPx.coerceAtLeast(0f).coerceAtMost(min(rect.width, rect.height) / 2f)

    drawRoundRect(
        color = color,
        topLeft = rect.topLeft,
        size = rect.size,
        cornerRadius = CornerRadius(x = radius, y = radius),
        style = Stroke(width = strokeWidth),
    )
}

private fun DrawScope.drawSelectedBarHighlight(
    rect: Rect,
    cornerPx: Float,
    focusColor: Color,
    overlayColor: Color?,
    strokeColor: Color?,
    strokeWidthPx: Float,
) {
    val fill = overlayColor ?: focusColor.copy(alpha = 0.12f)
    val stroke = strokeColor ?: focusColor.copy(alpha = 0.72f)
    val sw = strokeWidthPx.coerceAtLeast(0f)
    if (rect.width <= 0f || rect.height <= 0f) {
        return
    }

    drawRoundedBar(
        rect = rect,
        cornerPx = cornerPx,
        brush = null,
        color = fill
    )

    if (sw > 0f) {
        drawRoundedBarStroke(
            rect = rect,
            cornerPx = cornerPx,
            color = stroke,
            strokeWidth = sw
        )
    }
}

private fun DrawScope.drawPeekOverlay(
    point: Offset,
    focusColor: Color,
    guideColor: Color,
    guideWidthPx: Float,
    guideDashPx: Float,
    guideGapPx: Float,
    focusRadiusPx: Float,
    focusRingWidthPx: Float,
    focusHaloRadiusPx: Float,
    chart: Rect,
) {
    if (guideWidthPx > 0f) {
        val dash = if (guideDashPx > 0f && guideGapPx > 0f) {
            PathEffect.dashPathEffect(
                intervals = floatArrayOf(
                    guideDashPx,
                    guideGapPx,
                ),
                phase = 0f,
            )
        } else {
            null
        }
        drawLine(
            color = guideColor,
            start = Offset(point.x, chart.top),
            end = Offset(point.x, chart.bottom),
            strokeWidth = guideWidthPx,
            cap = StrokeCap.Round,
            pathEffect = dash,
        )
    }

    if (focusHaloRadiusPx > 0f) {
        val halo = Brush.radialGradient(
            colors = listOf(
                focusColor.copy(alpha = 0.20f),
                focusColor.copy(alpha = 0f),
            ),
            center = point,
            radius = focusHaloRadiusPx,
        )
        drawCircle(brush = halo, radius = focusHaloRadiusPx, center = point)
    }
    if (focusRadiusPx > 0f && focusRingWidthPx > 0f) {
        drawCircle(
            color = focusColor.copy(alpha = 0.48f),
            radius = (focusRadiusPx * 1.9f).coerceAtLeast(focusRadiusPx + 1f),
            center = point,
            style = Stroke(width = focusRingWidthPx, cap = StrokeCap.Round),
        )
    }
    if (focusRadiusPx > 0f) {
        drawCircle(color = focusColor, radius = focusRadiusPx, center = point)
    }
}

private fun measureEllipsized(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    maxWidthPx: Float
): TextLayoutResult {
    return measurer.measure(
        text = AnnotatedString(text),
        style = style,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        constraints = Constraints(maxWidth = maxWidthPx.toInt().coerceAtLeast(1)),
    )
}

private fun formatValue(value: Float, decimals: Int): String {
    if (decimals <= 0) {
        return value.roundToInt().toString()
    }

    val scale = 10.0.pow(decimals.toDouble())
    val v = round(value.toDouble() * scale) / scale

    val sign = if (v < 0.0) "-" else ""
    val absV = abs(v)

    val intPart = floor(absV).toLong()
    val frac = absV - intPart.toDouble()

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
    style: BarStyle.Peek.Visible,
    canvasWidth: Int,
): IntSize {
    val padHPx = with(density) { style.tooltipPaddingH.roundToPx() }
    val padVPx = with(density) { style.tooltipPaddingV.roundToPx() }
    val contentWidth = (canvasWidth - padHPx * 2).coerceAtLeast(1)
    val value = textMeasurer.measure(
        text = AnnotatedString(valueText),
        style = style.valueTextStyle,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        constraints = Constraints(maxWidth = contentWidth),
    ).size

    val label = if (!labelText.isNullOrBlank()) {
        textMeasurer.measure(
            text = AnnotatedString(labelText),
            style = style.labelTextStyle,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            constraints = Constraints(maxWidth = contentWidth),
        ).size
    } else {
        null
    }

    val gapPx = if (label != null) with(density) { style.tooltipLabelSpacing.roundToPx() } else 0

    val contentW = max(value.width, label?.width ?: 0)
    val contentH = value.height + (label?.height ?: 0) + gapPx

    val w = (contentW + padHPx * 2).coerceAtMost(canvasWidth)
    val h = contentH + padVPx * 2

    return IntSize(w, h)
}

@Composable
private fun BarChartTooltip(
    modifier: Modifier = Modifier,
    anchor: Offset,
    canvasSize: IntSize,
    tooltipSize: IntSize,
    style: BarStyle.Peek.Visible,
    valueText: String,
    labelText: String?,
) {
    val density = LocalDensity.current
    val marginPx = with(density) { style.tooltipMargin.toPx() }.roundToInt()

    if (tooltipSize.width <= 0 || tooltipSize.height <= 0 ||
        canvasSize.width <= 0 || tooltipSize.height > canvasSize.height
    ) {
        return
    }
    val position = tooltipPosition(
        anchor = anchor,
        canvasSize = canvasSize,
        tooltipSize = tooltipSize,
        marginPx = marginPx,
    )
    val shape = RoundedCornerShape(style.tooltipCornerRadius)

    Box(
        modifier = modifier
            .offset { position }
            .width(with(density) { tooltipSize.width.toDp() })
            .background(color = style.tooltipBackground, shape = shape)
            .then(
                if (style.tooltipBorderWidth > Dp.Hairline) {
                    Modifier.border(
                        width = style.tooltipBorderWidth,
                        color = style.tooltipBorder,
                        shape = shape,
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = style.tooltipPaddingH, vertical = style.tooltipPaddingV)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(style.tooltipLabelSpacing),
        ) {
            BasicText(
                text = valueText,
                style = style.valueTextStyle.copy(color = style.tooltipText),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!labelText.isNullOrBlank()) {
                BasicText(
                    text = labelText,
                    style = style.labelTextStyle.copy(color = style.tooltipText),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun tooltipPosition(
    anchor: Offset,
    canvasSize: IntSize,
    tooltipSize: IntSize,
    marginPx: Int,
): IntOffset {
    val width = tooltipSize.width
    val height = tooltipSize.height
    val anchorX = anchor.x.roundToInt()
    val anchorY = anchor.y.roundToInt()
    val candidates = listOf(
        IntOffset(x = anchorX - width / 2, y = anchorY - height - marginPx),
        IntOffset(x = anchorX - width / 2, y = anchorY + marginPx),
        IntOffset(x = anchorX + marginPx, y = anchorY - height / 2),
        IntOffset(x = anchorX - width - marginPx, y = anchorY - height / 2),
    )
    val maxX = max(0, canvasSize.width - width)
    val maxY = max(0, canvasSize.height - height)
    val chosen = candidates.firstOrNull { candidate ->
        candidate.x in 0..maxX && candidate.y in 0..maxY
    } ?: candidates.minBy { candidate ->
        abs(candidate.x.coerceIn(0, maxX) - candidate.x) +
            abs(candidate.y.coerceIn(0, maxY) - candidate.y)
    }
    return IntOffset(x = chosen.x.coerceIn(0, maxX), y = chosen.y.coerceIn(0, maxY))
}
