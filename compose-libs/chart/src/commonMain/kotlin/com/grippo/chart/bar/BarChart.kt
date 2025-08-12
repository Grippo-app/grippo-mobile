package com.grippo.chart.bar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow

@Immutable
public data class BarEntry(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
public fun BarChart(
    modifier: Modifier = Modifier,
    data: List<BarEntry>,
    style: BarStyle = BarStyle(),
) {
    val measurer = rememberTextMeasurer()

    androidx.compose.foundation.Canvas(modifier) {
        if (data.isEmpty()) return@Canvas

        // ----- Y scale (nice 1/2/5 × 10^k) -----
        val rawMaxY = max(1f, data.maxOf { it.value })
        val ticksY = max(1, style.yAxisTicks)
        fun niceStep(maxVal: Float, ticks: Int): Float {
            val raw = maxVal / ticks
            val exp = floor(log10(max(raw.toDouble(), 1.0))).toInt()
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

        val yStep = niceStep(rawMaxY, ticksY)
        val maxY = ceil(rawMaxY / yStep) * yStep
        val minY = 0f

        // ----- Layout gutters (reserve space for labels) -----
        val pad = style.padding.toPx()
        val labelPadPx = style.labelPadding.toPx()

        var leftGutter = pad
        if (style.showYAxis) {
            val maxLabelWidth = (0..ticksY).maxOf { i ->
                val t = style.yValueFormatter(i * yStep)
                val layout = measurer.measure(AnnotatedString(t), style.yAxisTextStyle)
                layout.size.width
            }
            leftGutter += maxLabelWidth + labelPadPx
        }

        var bottomGutter = pad
        if (style.showXLabels) {
            val maxH = data.maxOf { e ->
                measurer.measure(AnnotatedString(e.label), style.xAxisTextStyle).size.height
            }
            bottomGutter += maxH + labelPadPx
        }

        var topGutter = pad
        if (style.showValueLabels) {
            val maxH = data.maxOf { e ->
                val t = style.valueFormatter(e.value)
                measurer.measure(AnnotatedString(t), style.valueLabelTextStyle).size.height
            }
            topGutter += maxH + labelPadPx
        }

        val chart = Rect(leftGutter, topGutter, size.width - pad, size.height - bottomGutter)

        fun mapY(y: Float): Float =
            chart.bottom - (y - minY) / (maxY - minY).coerceAtLeast(1e-3f) * chart.height

        // ----- Grid (horizontal lines) & Y axis -----
        if (style.showGrid) {
            val gw = style.gridStrokeWidth.toPx()
            for (i in 0..ticksY) {
                val yVal = i * yStep
                val y = mapY(yVal)
                drawLine(style.gridColor, Offset(chart.left, y), Offset(chart.right, y), gw)
            }
        }
        if (style.showYAxis) {
            for (i in 0..ticksY) {
                val yVal = i * yStep
                val y = mapY(yVal)
                val text = style.yValueFormatter(yVal)
                val layout = measurer.measure(AnnotatedString(text), style.yAxisTextStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        x = chart.left - labelPadPx - layout.size.width,
                        y = y - layout.size.height / 2f
                    )
                )
                // tick
                drawLine(
                    color = style.axisLineColor,
                    start = Offset(chart.left - 4.dp.toPx(), y),
                    end = Offset(chart.left, y),
                    strokeWidth = style.axisLineWidth.toPx()
                )
            }
            if (style.showYAxisLine) {
                drawLine(
                    color = style.axisLineColor,
                    start = Offset(chart.left, chart.top),
                    end = Offset(chart.left, chart.bottom),
                    strokeWidth = style.axisLineWidth.toPx()
                )
            }
        }

        // ----- Bars -----
        val bw = style.barWidth.toPx()
        val sp = style.spacing.toPx()
        val totalW = data.size * bw + (data.size - 1) * sp
        val startX = chart.left + (chart.width - totalW).coerceAtLeast(0f) / 2f
        val rx = style.corner.toPx()
        val barStrokeW = style.barStrokeWidth.toPx()

        data.forEachIndexed { i, e ->
            chart.height * (e.value / maxY)
            val left = startX + i * (bw + sp)
            val top = mapY(e.value)
            val barRect = Rect(left, top, left + bw, chart.bottom)

            val brush = style.barBrush?.invoke(e, size, barRect)
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

            if (barStrokeW > 0f) {
                drawRoundRect(
                    color = style.barStrokeColor,
                    topLeft = Offset(barRect.left, barRect.top),
                    size = Size(barRect.width, barRect.height),
                    cornerRadius = CornerRadius(rx, rx),
                    style = Stroke(width = barStrokeW)
                )
            }

            // Value label above bar
            if (style.showValueLabels) {
                val valueText = style.valueFormatter(e.value)
                val layout = measurer.measure(AnnotatedString(valueText), style.valueLabelTextStyle)
                val x = left + (bw - layout.size.width) / 2f
                val y = (barRect.top - labelPadPx - layout.size.height).coerceAtLeast(chart.top)
                drawText(layout, topLeft = Offset(x, y))
            }

            // X label under bar
            if (style.showXLabels) {
                val layout = measurer.measure(AnnotatedString(e.label), style.xAxisTextStyle)
                val x = left + (bw - layout.size.width) / 2f
                val y = chart.bottom + labelPadPx / 2f
                drawText(layout, topLeft = Offset(x, y))
            }
        }
    }
}