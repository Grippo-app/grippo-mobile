package com.grippo.chart.bar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.grippo.chart.utils.chooseContrastingText
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
    style: BarStyle = BarStyle(),
) {
    val measurer = rememberTextMeasurer()

    androidx.compose.foundation.Canvas(modifier) {
        if (data.items.isEmpty()) return@Canvas

        // ----- Domain (include zero, nice ticks) -----
        val minVal = data.items.minOf { it.value }
        val maxVal = data.items.maxOf { it.value }
        val yMinTarget = min(0f, minVal)
        val yMaxTarget = max(0f, maxVal)

        val ticksY = max(1, style.yAxis.ticks)
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
        val minY = floor(yMinTarget / yStep) * yStep
        val maxY = ceil(yMaxTarget / yStep) * yStep
        val spanY = (maxY - minY).coerceAtLeast(1e-6f)

        // ----- Gutters -----
        val pad = style.layout.padding.toPx()
        val labelPad = style.layout.labelPadding.toPx()

        var leftGutter = pad
        if (style.yAxis.show) {
            val maxLabelW = (0..ticksY).maxOf { i ->
                val t = style.yAxis.formatter(minY + i * yStep, data)
                val layout = measurer.measure(AnnotatedString(t), style.yAxis.textStyle)
                layout.size.width
            }
            leftGutter += maxLabelW + labelPad
        }

        var bottomGutter = pad
        if (style.xAxis.show) {
            val maxH = data.items.maxOf { e ->
                measurer.measure(AnnotatedString(e.label), style.xAxis.textStyle).size.height
            }
            bottomGutter += maxH + labelPad
        }

        var topGutter = pad
        if (style.values.show && style.values.placement == BarStyle.ValuePlacement.Above) {
            val maxH = data.items.maxOf { e ->
                val t = style.values.formatter(e.value, data)
                measurer.measure(AnnotatedString(t), style.values.textStyle).size.height
            }
            topGutter += maxH + labelPad
        }

        var rightGutter = pad
        if (style.values.show && style.values.placement == BarStyle.ValuePlacement.Outside) {
            val maxW = data.items.maxOf { e ->
                val t = style.values.formatter(e.value, data)
                measurer.measure(AnnotatedString(t), style.values.textStyle).size.width
            }
            rightGutter += maxW + labelPad
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
            for (i in 0..ticksY) {
                val yVal = minY + i * yStep
                val y = mapY(yVal)
                drawLine(style.grid.color, Offset(chart.left, y), Offset(chart.right, y), gw)
            }
        }
        if (style.yAxis.show) {
            for (i in 0..ticksY) {
                val yVal = minY + i * yStep
                val y = mapY(yVal)
                val text = style.yAxis.formatter(yVal, data)
                val layout = measurer.measure(AnnotatedString(text), style.yAxis.textStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        x = chart.left - labelPad - layout.size.width,
                        y = y - layout.size.height / 2f
                    )
                )
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
        if (style.xAxis.show && style.xAxis.showBaseline) {
            val baseY = mapY(0f)
            drawLine(
                color = style.xAxis.baselineColor,
                start = Offset(chart.left, baseY),
                end = Offset(chart.right, baseY),
                strokeWidth = style.xAxis.baselineWidth.toPx()
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

        // ----- Bars -----
        val bw = style.bars.width.toPx()
        val sp = style.bars.spacing.toPx()
        val totalW = data.items.size * bw + (data.items.size - 1) * sp
        val startX = chart.left + (chartW - totalW).coerceAtLeast(0f) / 2f
        val rx = style.bars.corner.toPx()
        val strokeW = style.bars.strokeWidth.toPx()
        val baseY = mapY(0f)

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

            // Value label
            if (style.values.show) {
                val txt = style.values.formatter(e.value, data)
                val layout = measurer.measure(AnnotatedString(txt), style.values.textStyle)
                when (style.values.placement) {
                    BarStyle.ValuePlacement.Above -> {
                        val x = left + (bw - layout.size.width) / 2f
                        val y =
                            (top - style.layout.labelPadding.toPx() - layout.size.height).coerceAtLeast(
                                chart.top
                            )
                        drawText(layout, topLeft = Offset(x, y))
                    }

                    BarStyle.ValuePlacement.Outside -> {
                        val x = barRect.right + style.layout.labelPadding.toPx()
                        val centerY = (top + bottom) / 2f
                        val y = centerY - layout.size.height / 2f
                        drawText(layout, topLeft = Offset(x, y))
                    }

                    BarStyle.ValuePlacement.Inside -> {
                        val innerPad = style.values.minInnerPadding.toPx()
                        val fits = (bw - 2f * innerPad) >= layout.size.width
                        if (fits) {
                            val x = left + bw - innerPad - layout.size.width
                            val centerY = (top + bottom) / 2f
                            val y = centerY - layout.size.height / 2f
                            val insideColor = style.values.insideColor ?: chooseContrastingText(
                                e.color, style.values.textStyle.color
                            )
                            val insideLayout = measurer.measure(
                                AnnotatedString(txt),
                                style.values.textStyle.copy(color = insideColor)
                            )
                            drawText(insideLayout, topLeft = Offset(x, y))
                        } else {
                            val x = barRect.right + style.layout.labelPadding.toPx()
                            val centerY = (top + bottom) / 2f
                            val y = centerY - layout.size.height / 2f
                            drawText(layout, topLeft = Offset(x, y))
                        }
                    }
                }
            }

            // X label
            if (style.xAxis.show) {
                val layout = measurer.measure(AnnotatedString(e.label), style.xAxis.textStyle)
                val x = left + (bw - layout.size.width) / 2f
                val y = chart.bottom + style.layout.labelPadding.toPx() / 2f
                drawText(layout, topLeft = Offset(x, y))
            }
        }
    }
}