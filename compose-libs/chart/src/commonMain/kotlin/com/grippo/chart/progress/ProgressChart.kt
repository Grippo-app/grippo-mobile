package com.grippo.chart.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.times
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

@Composable
public fun ProgressChart(
    modifier: Modifier = Modifier,
    data: ProgressData,
    style: ProgressStyle,
) {
    val measurer = rememberTextMeasurer()

    val h = style.layout.barHeight
    val gap = style.layout.spacing
    val totalHeight = data.items.size * h + (data.items.size - 1) * gap

    Canvas(modifier.height(totalHeight)) {
        if (data.items.isEmpty()) return@Canvas

        // ----- Domain -----
        val isNormalized = style.domain is ProgressStyle.Domain.Normalized
        val autoMax = if (isNormalized) 1f else max(1f, data.items.maxOf { it.value })
        val maxV = when (val d = style.domain) {
            is ProgressStyle.Domain.Normalized -> 1f
            is ProgressStyle.Domain.Absolute -> d.maxValue?.takeIf { it > 0f } ?: autoMax
        }

        // ----- Gutters -----
        val labelPad = style.layout.labelPadding.toPx()

        val maxLabelW = data.items.maxOf { e ->
            measurer.measure(AnnotatedString(e.label), style.labels.textStyle).size.width
        }
        val leftGutter = maxLabelW + labelPad

        val rightGutter = when (val vCfg = style.values) {
            is ProgressStyle.Values.Outside -> {
                val sampleValues = data.items.map { e ->
                    val disp = if (isNormalized && vCfg.preferNormalizedLabels)
                        e.value.coerceIn(0f, 1f) else e.value
                    vCfg.formatter(disp, data)
                }
                val maxValueW = sampleValues.maxOfOrNull { t ->
                    measurer.measure(AnnotatedString(t), vCfg.textStyle).size.width
                } ?: 0
                (maxValueW + labelPad)
            }

            else -> 0f
        }

        val chart = Rect(leftGutter, 0f, size.width - rightGutter, size.height)
        val chartW = chart.width.coerceAtLeast(0f)
        val chartH = chart.height.coerceAtLeast(0f)
        if (chartW <= 0f || chartH <= 0f) return@Canvas

        // geometry
        val hPx = style.layout.barHeight.toPx()
        val gapPx = style.layout.spacing.toPx()
        val rx = style.layout.corner.toPx()
        val strokeW = style.bars.strokeWidth.toPx()

        val totalH = data.items.size * hPx + (data.items.size - 1) * gapPx
        val scale = if (totalH > chartH) chartH / totalH else 1f
        val hEff = hPx * scale
        val gapEff = gapPx * scale
        val totalHEff = data.items.size * hEff + (data.items.size - 1) * gapEff
        val startY = chart.top + (chartH - totalHEff).coerceAtLeast(0f) / 2f

        val domain = maxV.coerceAtLeast(1e-6f)

        // ----- Scaling function -----
        fun scaledWidth(value: Float): Float {
            val v = if (isNormalized) value.coerceIn(0f, 1f) else value
            val ratio = (v / domain).coerceIn(0f, 1f)
            return when (val prog = style.progression) {
                is ProgressStyle.Progression.Linear -> ratio * chartW
                is ProgressStyle.Progression.Power -> ratio.pow(prog.alpha) * chartW
                is ProgressStyle.Progression.Logarithmic -> {
                    val safeV = max(v, 0f)
                    (ln(safeV + 1.0) / ln(domain + 1.0)).toFloat() * chartW
                }
            }
        }

        // 1. Base widths
        val baseWidths = data.items.map { e -> scaledWidth(e.value) }

        // 2. Guarantee for text (Inside)
        val finalWidths = when (val vCfg = style.values) {
            is ProgressStyle.Values.Inside -> {
                data.items.mapIndexed { i, e ->
                    val disp = if (isNormalized && vCfg.preferNormalizedLabels)
                        e.value.coerceIn(0f, 1f) else e.value
                    val txt = vCfg.formatter(disp, data)
                    val textLayout = measurer.measure(AnnotatedString(txt), vCfg.textStyle)
                    val needed = textLayout.size.width + 2 * vCfg.minInnerPadding.toPx()
                    max(baseWidths[i], needed)
                }
            }

            else -> baseWidths
        }

        // target marker
        style.target?.let { t ->
            val v = if (isNormalized) t.value.coerceIn(0f, 1f) else t.value
            val x = chart.left + scaledWidth(v)
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
            val w = finalWidths[i].coerceIn(0f, chartW)
            val barRect = Rect(chart.left, y, chart.left + w, y + hEff)

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
                    val disp = if (isNormalized && vCfg.preferNormalizedLabels)
                        e.value.coerceIn(0f, 1f) else e.value
                    val txt = vCfg.formatter(disp, data)
                    val textLayout = measurer.measure(
                        AnnotatedString(txt),
                        vCfg.textStyle.copy(color = vCfg.insideColor ?: vCfg.textStyle.color)
                    )
                    val vx = barRect.right - vCfg.minInnerPadding.toPx() - textLayout.size.width
                    val vy = y + (hEff - textLayout.size.height) / 2f
                    drawText(textLayout, topLeft = Offset(vx, vy))
                }

                is ProgressStyle.Values.Outside -> {
                    val disp = if (isNormalized && vCfg.preferNormalizedLabels)
                        e.value.coerceIn(0f, 1f) else e.value
                    val txt = vCfg.formatter(disp, data)
                    val textLayout = measurer.measure(AnnotatedString(txt), vCfg.textStyle)
                    val vx = barRect.right + labelPad
                    val vy = y + (hEff - textLayout.size.height) / 2f
                    drawText(textLayout, topLeft = Offset(vx, vy))
                }
            }
        }
    }
}