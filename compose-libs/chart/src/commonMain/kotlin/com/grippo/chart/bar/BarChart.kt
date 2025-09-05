package com.grippo.chart.bar

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
public fun BarChart(
    modifier: Modifier = Modifier,
    data: BarData,
    style: BarStyle,
) {
    val measurer = rememberTextMeasurer()

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

    Canvas(modifier) {
        if (data.items.isEmpty()) return@Canvas

        // ----- Domain (data-driven with headroom; ensure room for Above value labels) -----
        val rawMin = data.items.minOf { it.value }
        val rawMax = data.items.maxOf { it.value }
        val baseSpan = (rawMax - rawMin).coerceAtLeast(1e-6f)
        val headroom = (baseSpan * 0.08f).coerceAtLeast(1e-3f)
        val yMinTarget = 0f
        var yMaxTarget = (rawMax + headroom)

        // Predict vertical space needed for Above labels and enlarge max if necessary
        val valuePadPx = style.layout.labelPadding.toPx()
        val valueAboveMaxH = when (val vCfg = style.values) {
            is BarStyle.Values.Above -> data.items.maxOf {
                val txt = vCfg.formatter(it.value, data)
                measurer.measure(AnnotatedString(txt), vCfg.textStyle).size.height
            }.toFloat()

            else -> 0f
        }
        if (valueAboveMaxH > 0f) {
            val bottomCandidate = when (val xCfg = style.xAxis) {
                is BarStyle.XAxis.LabelsAdaptive -> data.items.maxOf {
                    measurer.measure(AnnotatedString(it.label), xCfg.textStyle).size.height
                }.toFloat()

                is BarStyle.XAxis.LabelsShowAll -> data.items.maxOf {
                    measurer.measure(AnnotatedString(it.label), xCfg.textStyle).size.height
                }.toFloat()

                is BarStyle.XAxis.None -> 0f
            }
            val chartHApprox = (size.height - bottomCandidate).coerceAtLeast(1f)
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
        val maxYAligned = ceil(yMaxTarget / yStep) * yStep
        val minYAligned = floor(yMinTarget / yStep) * yStep
        val maxY = maxYAligned
        val minY = minYAligned
        val spanY = (maxY - minY).coerceAtLeast(1e-6f)
        val yTickCount = ((maxY - minY) / yStep).toInt().coerceAtLeast(1)

        // ----- Gutters (inside-only; no external paddings) -----
        val labelPad = style.layout.labelPadding.toPx()

        val yAxisSpecs = when (val yCfg = style.yAxis) {
            is BarStyle.YAxis.Labels -> (0..yTickCount).map { i ->
                val t = yCfg.formatter(minY + i * yStep, data)
                t to yCfg.textStyle
            }

            is BarStyle.YAxis.None -> emptyList()
        }

        var leftGutter: Float
        when (style.yAxis) {
            is BarStyle.YAxis.Labels -> {
                val maxLabelW = yAxisSpecs.maxOf { (text, style) ->
                    measurer.measure(AnnotatedString(text), style).size.width
                }
                leftGutter = maxLabelW + labelPad
            }

            is BarStyle.YAxis.None -> leftGutter = 0f
        }

        val xAxisSpecs = when (style.xAxis) {
            is BarStyle.XAxis.LabelsAdaptive, is BarStyle.XAxis.LabelsShowAll -> precomputedXLayouts
            is BarStyle.XAxis.None -> emptyList()
        }

        var bottomGutter: Float
        when (style.xAxis) {
            is BarStyle.XAxis.LabelsAdaptive -> {
                val maxH = xAxisSpecs.maxOfOrNull { it.size.height } ?: 0
                bottomGutter = maxH.toFloat()
            }

            is BarStyle.XAxis.LabelsShowAll -> {
                val maxH = xAxisSpecs.maxOfOrNull { it.size.height } ?: 0
                bottomGutter = maxH.toFloat()
            }

            is BarStyle.XAxis.None -> bottomGutter = 0f
        }

        val topGutter = 0f // chart area goes to the very top; values clamp to y>=0
        val rightGutter = when (val vCfg = style.values) {
            is BarStyle.Values.Outside -> {
                val samples = data.items.map { e -> vCfg.formatter(e.value, data) }
                val maxW = samples.maxOfOrNull { t ->
                    measurer.measure(AnnotatedString(t), vCfg.textStyle).size.width
                } ?: 0
                (maxW + labelPad)
            }

            else -> 0f
        }

        val chart =
            Rect(leftGutter, topGutter, size.width - rightGutter, size.height - bottomGutter)
        val chartW = chart.width.coerceAtLeast(0f)
        val chartH = chart.height.coerceAtLeast(0f)
        if (chartW <= 0f || chartH <= 0f) return@Canvas

        fun mapY(v: Float): Float = chart.bottom - (v - minY) / spanY * chartH

        // ----- Grid & axes -----
        if (style.grid.show) {
            val gw = style.grid.strokeWidth.toPx()
            for (i in 0..yTickCount) {
                val yVal = minY + i * yStep
                val y = mapY(yVal)
                drawLine(style.grid.color, Offset(chart.left, y), Offset(chart.right, y), gw)
            }
        }
        when (val yCfg = style.yAxis) {
            is BarStyle.YAxis.Labels -> {
                val layouts = yAxisSpecs.map { (text, style) ->
                    measurer.measure(
                        AnnotatedString(text),
                        style
                    )
                }
                for (i in 0..yTickCount) {
                    val yVal = minY + i * yStep
                    val y = mapY(yVal)
                    val layout = layouts[i]
                    val x = (chart.left - labelPad - layout.size.width)
                        .coerceAtLeast(0f)
                    val top = (y - layout.size.height / 2f)
                        .coerceIn(chart.top, chart.bottom - layout.size.height.toFloat())
                    drawText(
                        layout,
                        topLeft = Offset(
                            x = x,
                            y = top
                        )
                    )
                    drawLine(
                        color = yCfg.tickMarkColor,
                        start = Offset(chart.left, y),
                        end = Offset(chart.left + 4.dp.toPx(), y),
                        strokeWidth = yCfg.tickMarkWidth.toPx()
                    )
                }
            }

            is BarStyle.YAxis.None -> Unit
        }
        style.yAxisLine?.let { axis ->
            drawLine(
                color = axis.color,
                start = Offset(chart.left, chart.top),
                end = Offset(chart.left, chart.bottom),
                strokeWidth = axis.width.toPx()
            )
        }
        style.xBaseline?.let { base ->
            val baseY = mapY(0f)
            drawLine(
                color = base.color,
                start = Offset(chart.left, baseY),
                end = Offset(chart.right, baseY),
                strokeWidth = base.width.toPx()
            )
        }

        // Optional target line (horizontal at some Y value)
        style.target?.let { t ->
            val y = mapY(t.value)
            drawLine(
                color = t.color,
                start = Offset(chart.left, y),
                end = Offset(chart.right, y),
                strokeWidth = t.width.toPx()
            )
        }

        // ----- Bars sizing -----
        val nBars = data.items.size
        val sizing = style.bars.sizing

        val (bw, sp, startX) = when (sizing) {
            is BarStyle.BarsSizing.AutoEqualBarsAndGaps -> {
                if (nBars == 1) {
                    val w = chartW / 3f
                    val sx = chart.left + (chartW - w) / 2f
                    Triple(w, 0f, sx)
                } else {
                    val wEqual = chartW / (2f * nBars - 1f)
                    val midPx = sizing.midThresholdDp.toPx()
                    val densePx = sizing.denseThresholdDp.toPx()

                    val ratio = when {
                        wEqual < densePx -> sizing.denseRatio
                        wEqual < midPx -> sizing.midRatio
                        else -> 1f
                    }.coerceIn(0f, 1f)

                    if (ratio == 1f) {
                        Triple(wEqual, wEqual, chart.left)
                    } else {
                        val w = chartW / (nBars + (nBars - 1) * ratio)
                        val gap = ratio * w
                        Triple(w, gap, chart.left)
                    }
                }
            }

            is BarStyle.BarsSizing.FixedBarWidth -> {
                val desired = sizing.width.toPx()
                val fitted = if (nBars > 0) min(desired, chartW / nBars) else 0f
                val gap = if (nBars > 1) (chartW - fitted * nBars) / (nBars - 1) else 0f
                Triple(fitted, gap.coerceAtLeast(0f), chart.left)
            }

            is BarStyle.BarsSizing.Explicit -> {
                val bw0 = sizing.width.toPx()
                val sp0 = sizing.spacing.toPx()
                val totalW = nBars * bw0 + (nBars - 1) * sp0
                val sx = chart.left + (chartW - totalW).coerceAtLeast(0f) / 2f
                Triple(bw0, sp0, sx)
            }

            is BarStyle.BarsSizing.GapAsBarRatio -> {
                // Gap = ratio * barWidth; solve bar width from chart width
                val r = sizing.ratio.coerceAtLeast(0f)
                val w = if (nBars == 0) 0f else chartW / (nBars + (nBars - 1) * r)
                val g = r * w
                Triple(w, g, chart.left)
            }
        }

        val rx = style.bars.corner.toPx()
        val strokeW = style.bars.strokeWidth.toPx()
        val baseY = mapY(0f)

        // Collect value labels to draw after bars (so they are never occluded by later bars)
        data class ValueLabel(val layout: TextLayoutResult, val x: Float, val y: Float)

        val deferredValueLabels = mutableListOf<ValueLabel>()

        data.items.forEachIndexed { i, e ->
            val left = startX + i * (bw + sp)
            val yVal = e.value
            val top = if (yVal >= 0f) mapY(yVal) else baseY
            val bottom = if (yVal >= 0f) baseY else mapY(yVal)
            val barRect = Rect(left, top, left + bw, bottom)

            // track shape is not typical for bars; draw only foreground
            val brush = style.bars.brushProvider?.invoke(e, size, barRect)
            if (brush != null) {
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(barRect.left, barRect.top),
                    size = Size(barRect.width, barRect.height),
                    cornerRadius = CornerRadius(rx, rx)
                )
            } else {
                drawRoundRect(
                    color = e.color,
                    topLeft = Offset(barRect.left, barRect.top),
                    size = Size(barRect.width, barRect.height),
                    cornerRadius = CornerRadius(rx, rx)
                )
            }

            if (strokeW > 0f) {
                drawRoundRect(
                    color = style.bars.strokeColor,
                    topLeft = Offset(barRect.left, barRect.top),
                    size = Size(barRect.width, barRect.height),
                    cornerRadius = CornerRadius(rx, rx),
                    style = Stroke(width = strokeW)
                )
            }

            // Value label(s)
            when (val vCfg = style.values) {
                is BarStyle.Values.None -> Unit
                is BarStyle.Values.Above -> {
                    val txt = vCfg.formatter(e.value, data)
                    val layout = measurer.measure(AnnotatedString(txt), vCfg.textStyle)
                    val y = (top - style.layout.labelPadding.toPx() - layout.size.height)
                        .coerceAtLeast(0f)
                    val x = left + (bw - layout.size.width) / 2f
                    deferredValueLabels += ValueLabel(layout, x, y)
                }

                is BarStyle.Values.Outside -> {
                    val txt = vCfg.formatter(e.value, data)
                    val layout = measurer.measure(AnnotatedString(txt), vCfg.textStyle)
                    val unclampedX = barRect.right + style.layout.labelPadding.toPx()
                    val x = unclampedX.coerceIn(
                        chart.left,
                        size.width - layout.size.width.toFloat()
                    )
                    val centerY = (top + bottom) / 2f
                    val y = centerY - layout.size.height / 2f
                    deferredValueLabels += ValueLabel(layout, x, y)
                }

                is BarStyle.Values.Inside -> {
                    val txt = vCfg.formatter(e.value, data)
                    val baseLayout = measurer.measure(AnnotatedString(txt), vCfg.textStyle)
                    val innerPad = vCfg.minInnerPadding.toPx()
                    val fits = (bw - 2f * innerPad) >= baseLayout.size.width
                    if (fits) {
                        val x = left + bw - innerPad - baseLayout.size.width
                        val centerY = (top + bottom) / 2f
                        val y = centerY - baseLayout.size.height / 2f
                        val insideColor = vCfg.insideColor ?: e.color
                        val insideLayout = measurer.measure(
                            AnnotatedString(txt),
                            vCfg.textStyle.copy(color = insideColor)
                        )
                        deferredValueLabels += ValueLabel(insideLayout, x, y)
                    } else {
                        val unclampedX = barRect.right + style.layout.labelPadding.toPx()
                        val x = unclampedX.coerceIn(
                            chart.left,
                            size.width - baseLayout.size.width.toFloat()
                        )
                        val centerY = (top + bottom) / 2f
                        val y = centerY - baseLayout.size.height / 2f
                        deferredValueLabels += ValueLabel(baseLayout, x, y)
                    }
                }
            }

            // X label(s)
            // Deferred: actual drawing is handled after bar loop when showAll=false to resolve overlaps.
        }

        // ----- X axis labels (adaptive / show-all / none) -----
        when (val xCfg = style.xAxis) {
            is BarStyle.XAxis.None -> Unit
            is BarStyle.XAxis.LabelsShowAll -> {
                val centers = data.items.indices.map { i ->
                    val left = startX + i * (bw + sp)
                    left + bw / 2f
                }

                data.items.indices.forEach { i ->
                    val maxWidth = bw
                    val style = xCfg.textStyle

                    var text = data.items[i].label
                    var layout = measurer.measure(AnnotatedString(text), style)

                    // truncate with ellipsis if needed
                    if (layout.size.width > maxWidth) {
                        val ellipsis = "…"
                        var cut = text.length
                        while (cut > 0) {
                            val candidate = text.take(cut) + ellipsis
                            val candidateLayout =
                                measurer.measure(AnnotatedString(candidate), style)
                            if (candidateLayout.size.width <= maxWidth) {
                                text = candidate
                                layout = candidateLayout
                                break
                            }
                            cut--
                        }
                    }

                    val w = layout.size.width.toFloat()
                    val cx = centers[i]
                    val left = (cx - w / 2f).coerceIn(chart.left, chart.right - w)
                    drawText(layout, topLeft = Offset(left, chart.bottom))
                }
            }

            is BarStyle.XAxis.LabelsAdaptive -> {
                val minGapPx = xCfg.minGapDp.toPx()
                val centers = data.items.indices.map { i ->
                    val left = startX + i * (bw + sp)
                    left + bw / 2f
                }
                val layouts = xAxisSpecs

                data class L(
                    val left: Float,
                    val right: Float,
                    val layout: TextLayoutResult
                )

                val bounds = mutableMapOf<Int, L>()
                for (i in data.items.indices) {
                    val lay = layouts[i]
                    val w = lay.size.width.toFloat()
                    val cx = centers[i]
                    val left = (cx - w / 2f).coerceIn(chart.left, chart.right - w)
                    bounds[i] = L(left = left, right = left + w, layout = lay)
                }
                val order = data.items.indices.toList()
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
                val lastIdx = order.last()
                val lastB = bounds[lastIdx]
                if (lastB != null) {
                    while (selected.isNotEmpty()) {
                        val tail = bounds[selected.last()] ?: break
                        if (lastB.left >= tail.right + minGapPx) break
                        if (selected.size == 1) break
                        selected.removeLast()
                    }
                    selected.add(lastIdx)
                }
                selected.sorted().forEach { i ->
                    val b = bounds[i] ?: return@forEach
                    drawText(b.layout, topLeft = Offset(b.left, chart.bottom))
                }
            }
        }

        // Draw deferred value labels last to ensure they never get covered by bars
        deferredValueLabels.forEach { item ->
            drawText(item.layout, topLeft = Offset(item.x, item.y))
        }
    }
}