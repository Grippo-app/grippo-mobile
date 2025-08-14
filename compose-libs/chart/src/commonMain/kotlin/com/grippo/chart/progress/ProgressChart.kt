package com.grippo.chart.progress

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
import com.grippo.chart.utils.chooseContrastingText
import kotlin.math.max

@Composable
public fun ProgressChart(
    modifier: Modifier = Modifier,
    data: ProgressData,
    style: ProgressStyle,
) {
    val measurer = rememberTextMeasurer()

    androidx.compose.foundation.Canvas(modifier) {
        if (data.items.isEmpty()) return@Canvas

        // ----- Domain -----
        val autoMax = if (style.domain.normalized) 1f else max(1f, data.items.maxOf { it.value })
        val maxV = style.domain.maxValue?.takeIf { it > 0f } ?: autoMax

        // ----- Gutters -----
        val pad = style.layout.padding.toPx()
        val labelPad = style.layout.labelPadding.toPx()

        // left gutter for labels
        val maxLabelW = data.items.maxOf { e ->
            measurer.measure(AnnotatedString(e.label), style.labels.textStyle).size.width
        }
        val leftGutter = pad + maxLabelW + labelPad

        // right gutter only if values are outside
        val sampleValues = if (style.values.show && !style.values.placeInside) {
            data.items.map { e ->
                val disp =
                    if (style.domain.normalized && style.values.preferNormalizedLabels) e.value.coerceIn(
                        0f,
                        1f
                    ) else e.value
                style.values.formatter(disp, data)
            }
        } else emptyList()
        val maxValueW = sampleValues.maxOfOrNull { t ->
            measurer.measure(AnnotatedString(t), style.values.textStyle).size.width
        } ?: 0
        val rightGutter =
            pad + if (style.values.show && !style.values.placeInside) maxValueW + labelPad else 0f

        val chart = Rect(leftGutter, pad, size.width - rightGutter, size.height - pad)

        // bail out if not enough room
        val chartW = chart.width.coerceAtLeast(0f)
        val chartH = chart.height.coerceAtLeast(0f)
        if (chartW <= 0f || chartH <= 0f) return@Canvas

        // geometry
        val h = style.layout.barHeight.toPx()
        val gap = style.layout.spacing.toPx()
        val rx = style.layout.corner.toPx()
        val strokeW = style.bars.strokeWidth.toPx()

        val totalH = data.items.size * h + (data.items.size - 1) * gap
        val startY = chart.top + (chartH - totalH).coerceAtLeast(0f) / 2f

        fun mapX(v: Float): Float {
            val vv = if (style.domain.normalized) v.coerceIn(0f, 1f) else v
            val cw = chartW.coerceAtLeast(1e-3f)
            val domain = maxV.coerceAtLeast(1e-3f)
            return chart.left + (vv / domain) * cw
        }

        // target marker
        style.target?.let { t ->
            val x = mapX(if (style.domain.normalized) t.value.coerceIn(0f, 1f) else t.value)
            drawLine(
                color = t.color,
                start = Offset(x, chart.top),
                end = Offset(x, chart.bottom),
                strokeWidth = t.width.toPx()
            )
        }

        // bars
        data.items.forEachIndexed { i, e ->
            val y = startY + i * (h + gap)

            // track
            style.bars.trackColor?.let { tc ->
                drawRoundRect(
                    color = tc,
                    topLeft = Offset(chart.left, y),
                    size = Size(chartW, h),
                    cornerRadius = CornerRadius(rx, rx)
                )
            }

            // foreground bar
            val w = (mapX(e.value) - chart.left).coerceIn(0f, chartW)
            val barRect = Rect(chart.left, y, chart.left + w, y + h)
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

            if (strokeW > 0f && w > 0f) {
                drawRoundRect(
                    color = style.bars.strokeColor,
                    topLeft = Offset(barRect.left, barRect.top),
                    size = Size(barRect.width, barRect.height),
                    cornerRadius = CornerRadius(rx, rx),
                    style = Stroke(width = strokeW)
                )
            }

            // left label
            val labelLayout = measurer.measure(AnnotatedString(e.label), style.labels.textStyle)
            val labelX = chart.left - labelPad - labelLayout.size.width
            val labelY = y + (h - labelLayout.size.height) / 2f
            drawText(labelLayout, topLeft = Offset(labelX, labelY))

            // value label
            if (style.values.show) {
                val display =
                    if (style.domain.normalized && style.values.preferNormalizedLabels) e.value.coerceIn(
                        0f,
                        1f
                    ) else e.value
                val txt = style.values.formatter(display, data)
                val valueLayout = measurer.measure(AnnotatedString(txt), style.values.textStyle)
                val insideFits =
                    style.values.placeInside && w >= valueLayout.size.width + 2f * style.values.minInnerPadding.toPx()
                if (insideFits) {
                    val vx =
                        barRect.right - style.values.minInnerPadding.toPx() - valueLayout.size.width
                    val vy = y + (h - valueLayout.size.height) / 2f
                    val insideColor = style.values.insideColor ?: chooseContrastingText(
                        e.color, style.values.textStyle.color
                    )
                    val insideLayout = measurer.measure(
                        AnnotatedString(txt),
                        style.values.textStyle.copy(color = insideColor)
                    )
                    drawText(insideLayout, topLeft = Offset(vx, vy))
                } else {
                    val vx = chart.left + w + style.layout.labelPadding.toPx()
                    val vy = y + (h - valueLayout.size.height) / 2f
                    drawText(valueLayout, topLeft = Offset(vx, vy))
                }
            }
        }
    }
}