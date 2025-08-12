package com.grippo.chart.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.grippo.chart.utils.chooseContrastingText
import kotlin.math.max

@Immutable
public data class ProgressChartData(val label: String, val value: Float, val color: Color)

@Composable
public fun ProgressChart(
    modifier: Modifier = Modifier,
    data: List<ProgressChartData>,
    style: ProgressStyle = ProgressStyle(),
) {
    val measurer = rememberTextMeasurer()

    androidx.compose.foundation.Canvas(modifier) {
        if (data.isEmpty()) return@Canvas

        // ----- Domain -----
        val autoMax = if (style.normalized) 1f else max(1f, data.maxOf { it.value })
        val maxV = style.maxValue?.takeIf { it > 0f } ?: autoMax

        // ----- Gutters -----
        val pad = style.padding.toPx()
        val labelPad = style.labelPadding.toPx()

        // Left gutter for labels
        val maxLabelW = data.maxOf { e ->
            measurer.measure(AnnotatedString(e.label), style.labelTextStyle).size.width
        }
        val leftGutter = pad + maxLabelW + labelPad

        // Reserve right gutter only if values are outside
        val sampleValues = if (style.showValue && !style.placeValueInside) {
            data.map { e ->
                style.valueFormatter(if (style.normalized) e.value.coerceIn(0f, 1f) else e.value)
            }
        } else emptyList()
        val maxValueW = sampleValues.maxOfOrNull { t ->
            measurer.measure(AnnotatedString(t), style.valueTextStyle).size.width
        } ?: 0
        val rightGutter =
            pad + if (style.showValue && !style.placeValueInside) maxValueW + labelPad else 0f

        // Chart rect
        val chart = Rect(leftGutter, pad, size.width - rightGutter, size.height - pad)

        // Bail out if not enough room
        val chartW = (chart.width).coerceAtLeast(0f)
        val chartH = (chart.height).coerceAtLeast(0f)
        if (chartW <= 0f || chartH <= 0f) return@Canvas

        // Geometry
        val h = style.barHeight.toPx()
        val gap = style.spacing.toPx()
        val rx = style.corner.toPx()
        val strokeW = style.barStrokeWidth.toPx()

        val totalH = data.size * h + (data.size - 1) * gap
        val startY = chart.top + (chartH - totalH).coerceAtLeast(0f) / 2f

        fun mapX(v: Float): Float {
            val vv = if (style.normalized) v.coerceIn(0f, 1f) else v
            val cw = chartW.coerceAtLeast(1e-3f)
            val domain = maxV.coerceAtLeast(1e-3f)
            return chart.left + (vv / domain) * cw
        }

        // Optional target marker
        style.targetValue?.let { tVal ->
            val x = mapX(if (style.normalized) tVal.coerceIn(0f, 1f) else tVal)
            drawLine(
                color = style.targetColor,
                start = Offset(x, chart.top),
                end = Offset(x, chart.bottom),
                strokeWidth = style.targetWidth.toPx()
            )
        }

        // Draw bars
        data.forEachIndexed { i, e ->
            val y = startY + i * (h + gap)

            // Track (full width)
            style.trackColor?.let { tc ->
                drawRoundRect(
                    color = tc,
                    topLeft = Offset(chart.left, y),
                    size = Size(chartW, h),
                    cornerRadius = CornerRadius(rx, rx)
                )
            }

            // Foreground bar
            val w = (mapX(e.value) - chart.left).coerceIn(0f, chartW)
            val barRect = Rect(chart.left, y, chart.left + w, y + h)

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

            if (strokeW > 0f && w > 0f) {
                drawRoundRect(
                    color = style.barStrokeColor,
                    topLeft = Offset(barRect.left, barRect.top),
                    size = Size(barRect.width, barRect.height),
                    cornerRadius = CornerRadius(rx, rx),
                    style = Stroke(width = strokeW)
                )
            }

            // Left label (vertically centered)
            val labelLayout = measurer.measure(AnnotatedString(e.label), style.labelTextStyle)
            val labelX = chart.left - labelPad - labelLayout.size.width
            val labelY = y + (h - labelLayout.size.height) / 2f
            drawText(labelLayout, topLeft = Offset(labelX, labelY))

            // Value label
            if (style.showValue) {
                val txt = style.valueFormatter(
                    if (style.normalized) e.value.coerceIn(0f, 1f) else e.value
                )
                val valueLayout = measurer.measure(AnnotatedString(txt), style.valueTextStyle)
                val insideFits =
                    style.placeValueInside && w >= valueLayout.size.width + 2f * style.minInnerPadding.toPx()
                if (insideFits) {
                    // Inside: right-aligned with padding and auto-contrast
                    val vx = barRect.right - style.minInnerPadding.toPx() - valueLayout.size.width
                    val vy = y + (h - valueLayout.size.height) / 2f
                    val insideColor = style.valueInsideColor ?: chooseContrastingText(
                        sampleBarColor(e, brush), style.valueTextStyle.color
                    )
                    val insideLayout = measurer.measure(
                        AnnotatedString(txt),
                        style.valueTextStyle.copy(color = insideColor)
                    )
                    drawText(insideLayout, topLeft = Offset(vx, vy))
                } else {
                    // Outside: to the right of the bar
                    val vx = chart.left + w + style.labelPadding.toPx()
                    val vy = y + (h - valueLayout.size.height) / 2f
                    drawText(valueLayout, topLeft = Offset(vx, vy))
                }
            }
        }
    }
}

private fun sampleBarColor(e: ProgressChartData, brush: Brush?): Color {
    // Simple sample: if brush provided, approximate by midpoint sampling is not trivial here; fallback to entry color
    return e.color
}