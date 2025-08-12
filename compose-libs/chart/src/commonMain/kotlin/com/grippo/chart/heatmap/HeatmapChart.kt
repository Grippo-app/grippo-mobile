package com.grippo.chart.heatmap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.grippo.chart.utils.chooseContrastingText

@Immutable
public data class HeatmapCell(val row: Int, val col: Int, val value01: Float)

@Composable
public fun HeatmapChart(
    modifier: Modifier = Modifier,
    cells: List<HeatmapCell>,
    style: HeatmapStyle,
) {
    val measurer = rememberTextMeasurer()

    androidx.compose.foundation.Canvas(modifier) {
        if (style.rows <= 0 || style.cols <= 0) return@Canvas

        // ----- Normalize values (optional) -----
        var minVal = 0f
        var maxVal = 1f
        if (style.autoNormalize && cells.isNotEmpty()) {
            minVal = cells.minOf { it.value01 }
            maxVal = cells.maxOf { it.value01 }
            if (maxVal == minVal) maxVal = minVal + 1f
        }
        fun norm(v: Float): Float =
            if (!style.autoNormalize) v else (v - minVal) / (maxVal - minVal)

        // ----- Gutters for labels/legend -----
        val pad = style.padding.toPx()
        val labelPadPx = style.labelPadding.toPx()

        var leftGutter = pad
        if (style.rowLabels.isNotEmpty()) {
            val maxW = (0 until style.rows).maxOf { r ->
                val text = style.rowLabels.getOrNull(r) ?: ""
                measurer.measure(AnnotatedString(text), style.labelStyle).size.width
            }
            leftGutter += maxW + labelPadPx
        }

        var bottomGutter = pad
        if (style.colLabels.isNotEmpty()) {
            val maxH = (0 until style.cols).maxOf { c ->
                val text = style.colLabels.getOrNull(c) ?: ""
                measurer.measure(AnnotatedString(text), style.labelStyle).size.height
            }
            bottomGutter += maxH + labelPadPx
        }
        if (style.showLegend) {
            bottomGutter += style.legendHeight.toPx() + labelPadPx
        }

        val chart = Rect(leftGutter, pad, size.width - pad, size.height - bottomGutter)

        // ----- Cell geometry -----
        val gap = style.gap.toPx()
        val cw = if (style.cols > 0) (chart.width - gap * (style.cols - 1)) / style.cols else 0f
        val ch = if (style.rows > 0) (chart.height - gap * (style.rows - 1)) / style.rows else 0f
        val rx = style.corner.toPx()

        // Pre-index cells for quick lookup
        val valueMap = HashMap<Long, Float>(cells.size)
        fun key(r: Int, c: Int) = (r.toLong() shl 32) or (c.toLong() and 0xFFFFFFFF)
        cells.forEach {
            if (it.row in 0 until style.rows && it.col in 0 until style.cols) valueMap[key(
                it.row,
                it.col
            )] = it.value01
        }

        // Background for missing cells
        if (style.missingCellColor != null) {
            for (r in 0 until style.rows) {
                for (c in 0 until style.cols) {
                    val x = chart.left + (cw + gap) * c
                    val y = chart.top + (ch + gap) * r
                    drawRoundRect(
                        color = style.missingCellColor,
                        topLeft = Offset(x, y),
                        size = Size(cw, ch),
                        cornerRadius = CornerRadius(rx, rx)
                    )
                }
            }
        }

        // Cells with data
        for (r in 0 until style.rows) {
            for (c in 0 until style.cols) {
                val v = valueMap[key(r, c)] ?: continue
                val t = norm(v).coerceIn(0f, 1f)
                val x = chart.left + (cw + gap) * c
                val y = chart.top + (ch + gap) * r
                val color = style.colorScale(t)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(cw, ch),
                    cornerRadius = CornerRadius(rx, rx)
                )

                if (style.showCellBorders && style.borderWidth.value > 0f) {
                    drawRoundRect(
                        color = style.borderColor,
                        topLeft = Offset(x, y),
                        size = Size(cw, ch),
                        cornerRadius = CornerRadius(rx, rx),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = style.borderWidth.toPx())
                    )
                }

                if (style.showValues) {
                    val text = style.valueFormatter(t)
                    val textStyle = style.valueTextStyle
                    val textColor = chooseContrastingText(color, textStyle.color)
                    val styledLayout = measurer.measure(
                        AnnotatedString(text),
                        textStyle.copy(color = textColor)
                    )
                    val cx = x + (cw - styledLayout.size.width) / 2f
                    val cy = y + (ch - styledLayout.size.height) / 2f
                    drawText(styledLayout, topLeft = Offset(cx, cy))
                }
            }
        }

        // Row labels
        if (style.rowLabels.isNotEmpty()) {
            for (r in 0 until style.rows) {
                val text = style.rowLabels.getOrNull(r) ?: ""
                val layout = measurer.measure(AnnotatedString(text), style.labelStyle)
                val y = chart.top + r * (ch + gap) + (ch - layout.size.height) / 2f
                val x = chart.left - labelPadPx - layout.size.width
                drawText(layout, topLeft = Offset(x, y))
            }
        }

        // Column labels
        if (style.colLabels.isNotEmpty()) {
            for (c in 0 until style.cols) {
                val text = style.colLabels.getOrNull(c) ?: ""
                val layout = measurer.measure(AnnotatedString(text), style.labelStyle)
                val x = chart.left + c * (cw + gap) + (cw - layout.size.width) / 2f
                val y = chart.bottom + labelPadPx / 2f
                drawText(layout, topLeft = Offset(x, y))
            }
        }

        // Legend
        if (style.showLegend) {
            val legendTop = size.height - pad - style.legendHeight.toPx()
            val legendRect =
                Rect(chart.left, legendTop, chart.right, legendTop + style.legendHeight.toPx())
            val brush = legendBrush(style.legendStops)
            drawRect(
                brush = brush,
                topLeft = Offset(legendRect.left, legendRect.top),
                size = Size(legendRect.width, legendRect.height)
            )

            val minText = (style.legendMinText?.let { it(minVal) }) ?: "0%"
            val maxText = (style.legendMaxText?.let { it(maxVal) }) ?: "100%"
            val minLayout = measurer.measure(AnnotatedString(minText), style.legendLabelStyle)
            val maxLayout = measurer.measure(AnnotatedString(maxText), style.legendLabelStyle)
            drawText(minLayout, topLeft = Offset(legendRect.left, legendRect.bottom + 2.dp.toPx()))
            drawText(
                maxLayout,
                topLeft = Offset(
                    legendRect.right - maxLayout.size.width,
                    legendRect.bottom + 2.dp.toPx()
                )
            )
        }
    }
}

private fun legendBrush(stops: List<Pair<Float, Color>>?): Brush {
    val s = stops ?: listOf(
        0f to Color(0xFF3A86FF),
        0.5f to Color(0xFFB049F8),
        1f to Color(0xFFFF7A33)
    )
    return Brush.horizontalGradient(colorStops = s.toTypedArray())
}