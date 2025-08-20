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
        val isNormalized = when (style.domain) {
            is ProgressStyle.Domain.Normalized -> true
            is ProgressStyle.Domain.Absolute -> false
        }
        val autoMax = if (isNormalized) 1f else max(1f, data.items.maxOf { it.value })
        val maxV = when (val d = style.domain) {
            is ProgressStyle.Domain.Normalized -> 1f
            is ProgressStyle.Domain.Absolute -> d.maxValue?.takeIf { it > 0f } ?: autoMax
        }

        // ----- Gutters (inside-only; no external paddings) -----
        val labelPad = style.layout.labelPadding.toPx()

        // left gutter for labels
        val maxLabelW = data.items.maxOf { e ->
            measurer.measure(AnnotatedString(e.label), style.labels.textStyle).size.width
        }
        val leftGutter = maxLabelW + labelPad

        // right gutter only if values are outside
        val rightGutter = when (val vCfg = style.values) {
            is ProgressStyle.Values.Outside -> {
                val sampleValues = data.items.map { e ->
                    val disp = if (isNormalized && vCfg.preferNormalizedLabels) e.value.coerceIn(
                        0f,
                        1f
                    ) else e.value
                    vCfg.formatter(disp, data)
                }
                val maxValueW = sampleValues.maxOfOrNull { t ->
                    measurer.measure(AnnotatedString(t), vCfg.textStyle).size.width
                } ?: 0
                (maxValueW + labelPad)
            }

            is ProgressStyle.Values.None, is ProgressStyle.Values.Inside -> 0f
        }

        val chart = Rect(leftGutter, 0f, size.width - rightGutter, size.height)

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
        val scale = if (totalH > chartH) chartH / totalH else 1f
        val hEff = h * scale
        val gapEff = gap * scale
        val totalHEff = data.items.size * hEff + (data.items.size - 1) * gapEff
        val startY = chart.top + (chartH - totalHEff).coerceAtLeast(0f) / 2f

        fun mapX(v: Float): Float {
            val vv = if (isNormalized) v.coerceIn(0f, 1f) else v
            val cw = chartW.coerceAtLeast(1e-3f)
            val domain = maxV.coerceAtLeast(1e-3f)
            return chart.left + (vv / domain) * cw
        }

        // target marker
        style.target?.let { t ->
            val x = mapX(if (isNormalized) t.value.coerceIn(0f, 1f) else t.value)
            drawLine(
                color = t.color,
                start = Offset(x, chart.top),
                end = Offset(x, chart.bottom),
                strokeWidth = t.width.toPx()
            )
        }

        // bars
        data.items.forEachIndexed { i, e ->
            val y = startY + i * (hEff + gapEff)

            // track
            style.bars.trackColor?.let { tc ->
                drawRoundRect(
                    color = tc,
                    topLeft = Offset(chart.left, y),
                    size = Size(chartW, hEff),
                    cornerRadius = CornerRadius(rx, rx)
                )
            }

            // foreground bar
            val w = (mapX(e.value) - chart.left).coerceIn(0f, chartW)
            val barRect = Rect(chart.left, y, chart.left + w, y + hEff)
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
            val labelY = y + (hEff - labelLayout.size.height) / 2f
            drawText(labelLayout, topLeft = Offset(labelX, labelY))

            // value label
            when (val vCfg = style.values) {
                is ProgressStyle.Values.None -> Unit
                is ProgressStyle.Values.Inside -> {
                    val display = if (isNormalized && vCfg.preferNormalizedLabels) e.value.coerceIn(
                        0f,
                        1f
                    ) else e.value
                    val txt = vCfg.formatter(display, data)
                    val baseLayout = measurer.measure(AnnotatedString(txt), vCfg.textStyle)
                    val insideFits = w >= baseLayout.size.width + 2f * vCfg.minInnerPadding.toPx()
                    if (insideFits) {
                        val vx = barRect.right - vCfg.minInnerPadding.toPx() - baseLayout.size.width
                        val vy = y + (hEff - baseLayout.size.height) / 2f
                        val insideColor = vCfg.insideColor ?: chooseContrastingText(
                            e.color, vCfg.textStyle.color
                        )
                        val insideLayout = measurer.measure(
                            AnnotatedString(txt),
                            vCfg.textStyle.copy(color = insideColor)
                        )
                        drawText(insideLayout, topLeft = Offset(vx, vy))
                    } else {
                        // fallback: draw outside but clamp inside chart
                        val valueLayout = measurer.measure(AnnotatedString(txt), vCfg.textStyle)
                        val unclampedX = barRect.right + style.layout.labelPadding.toPx()
                        val vx = unclampedX.coerceIn(
                            chart.left,
                            chart.right - valueLayout.size.width.toFloat()
                        )
                        val vy = y + (hEff - valueLayout.size.height) / 2f
                        drawText(valueLayout, topLeft = Offset(vx, vy))
                    }
                }

                is ProgressStyle.Values.Outside -> {
                    val display = if (isNormalized && vCfg.preferNormalizedLabels) e.value.coerceIn(
                        0f,
                        1f
                    ) else e.value
                    val txt = vCfg.formatter(display, data)
                    val valueLayout = measurer.measure(AnnotatedString(txt), vCfg.textStyle)
                    val unclampedX = barRect.right + style.layout.labelPadding.toPx()
                    val vx = unclampedX.coerceIn(
                        chart.left,
                        chart.right - valueLayout.size.width.toFloat()
                    )
                    val vy = y + (hEff - valueLayout.size.height) / 2f
                    drawText(valueLayout, topLeft = Offset(vx, vy))
                }
            }
        }
    }
}