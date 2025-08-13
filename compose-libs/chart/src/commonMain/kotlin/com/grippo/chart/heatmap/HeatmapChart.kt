package com.grippo.chart.heatmap

import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.grippo.chart.utils.chooseContrastingText

@Composable
public fun HeatmapChart(
    modifier: Modifier = Modifier,
    data: HeatmapData,
    style: HeatmapStyle = HeatmapStyle(),
) {
    val measurer = rememberTextMeasurer()

    androidx.compose.foundation.Canvas(modifier) {
        if (data.rows <= 0 || data.cols <= 0) return@Canvas

        // ----- Normalize domain (optional) -----
        val values = data.cells.map { it.value01 }
        var minVal = 0f
        var maxVal = 1f
        if (style.palette.autoNormalize && values.isNotEmpty()) {
            minVal = values.min()
            maxVal = values.max()
            if (maxVal == minVal) maxVal += 1f
        }
        fun norm(v: Float): Float =
            if (!style.palette.autoNormalize) v else (v - minVal) / (maxVal - minVal)

        // ----- Gutters for labels/legend -----
        val pad = style.layout.padding.toPx()
        val labelPadPx = style.layout.labelPadding.toPx()

        var leftGutter = pad
        if (style.labels.showRowLabels && data.rowLabels.isNotEmpty()) {
            val maxW = (0 until data.rows).maxOf { r ->
                val text = data.rowLabels.getOrNull(r) ?: ""
                measurer.measure(AnnotatedString(text), style.labels.textStyle).size.width
            }
            leftGutter += maxW + labelPadPx
        }

        var bottomGutter = pad
        if (style.labels.showColLabels && data.colLabels.isNotEmpty()) {
            val maxH = (0 until data.cols).maxOf { c ->
                val text = data.colLabels.getOrNull(c) ?: ""
                measurer.measure(AnnotatedString(text), style.labels.textStyle).size.height
            }
            bottomGutter += maxH + labelPadPx
        }
        if (style.legend.show) {
            bottomGutter += style.legend.height.toPx() + labelPadPx
        }

        val chart = Rect(leftGutter, pad, size.width - pad, size.height - bottomGutter)
        val chartW = chart.width.coerceAtLeast(0f)
        val chartH = chart.height.coerceAtLeast(0f)
        if (chartW <= 0f || chartH <= 0f) return@Canvas

        // ----- Cell geometry -----
        val gap = style.layout.gap.toPx()
        val cw = if (data.cols > 0) (chartW - gap * (data.cols - 1)) / data.cols else 0f
        val ch = if (data.rows > 0) (chartH - gap * (data.rows - 1)) / data.rows else 0f
        val rx = style.layout.corner.toPx()

        // Index cells for O(1) lookup
        val valueMap = HashMap<Long, Float>(data.cells.size)
        fun key(r: Int, c: Int) = (r.toLong() shl 32) or (c.toLong() and 0xFFFFFFFF)
        data.cells.forEach {
            if (it.row in 0 until data.rows && it.col in 0 until data.cols) {
                valueMap[key(it.row, it.col)] = it.value01
            }
        }

        // Background for missing cells
        style.palette.missingCellColor?.let { bg ->
            for (r in 0 until data.rows) {
                for (c in 0 until data.cols) {
                    val x = chart.left + (cw + gap) * c
                    val y = chart.top + (ch + gap) * r
                    drawRoundRect(
                        color = bg,
                        topLeft = Offset(x, y),
                        size = Size(cw, ch),
                        cornerRadius = CornerRadius(rx, rx)
                    )
                }
            }
        }

        // Cells with data
        for (r in 0 until data.rows) {
            for (c in 0 until data.cols) {
                val v = valueMap[key(r, c)] ?: continue
                val t = norm(v).coerceIn(0f, 1f)
                val x = chart.left + (cw + gap) * c
                val y = chart.top + (ch + gap) * r
                val color = style.palette.colorScale(t)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(cw, ch),
                    cornerRadius = CornerRadius(rx, rx)
                )

                if (style.cells.showBorders && style.cells.borderWidth.value > 0f) {
                    drawRoundRect(
                        color = style.cells.borderColor,
                        topLeft = Offset(x, y),
                        size = Size(cw, ch),
                        cornerRadius = CornerRadius(rx, rx),
                        style = Stroke(width = style.cells.borderWidth.toPx())
                    )
                }

                if (style.values.show) {
                    val pTxt = style.values.formatter(t, data)
                    val txtColor = chooseContrastingText(color, style.values.textStyle.color)
                    val layout = measurer.measure(
                        AnnotatedString(pTxt),
                        style.values.textStyle.copy(color = txtColor)
                    )
                    val cx = x + (cw - layout.size.width) / 2f
                    val cy = y + (ch - layout.size.height) / 2f
                    drawText(layout, topLeft = Offset(cx, cy))
                }
            }
        }

        // Row labels
        if (style.labels.showRowLabels && data.rowLabels.isNotEmpty()) {
            for (r in 0 until data.rows) {
                val text = data.rowLabels.getOrNull(r) ?: ""
                val layout = measurer.measure(AnnotatedString(text), style.labels.textStyle)
                val y = chart.top + r * (ch + gap) + (ch - layout.size.height) / 2f
                val x = chart.left - labelPadPx - layout.size.width
                drawText(layout, topLeft = Offset(x, y))
            }
        }

        // Column labels
        if (style.labels.showColLabels && data.colLabels.isNotEmpty()) {
            for (c in 0 until data.cols) {
                val text = data.colLabels.getOrNull(c) ?: ""
                val layout = measurer.measure(AnnotatedString(text), style.labels.textStyle)
                val x = chart.left + c * (cw + gap) + (cw - layout.size.width) / 2f
                val y = chart.bottom + labelPadPx / 2f
                drawText(layout, topLeft = Offset(x, y))
            }
        }

        // Legend
        if (style.legend.show) {
            val legendTop = size.height - pad - style.legend.height.toPx()
            val legendRect =
                Rect(chart.left, legendTop, chart.right, legendTop + style.legend.height.toPx())
            val brush = legendBrush(style.legend.stops)
            drawRect(
                brush = brush,
                topLeft = Offset(legendRect.left, legendRect.top),
                size = Size(legendRect.width, legendRect.height)
            )

            val minText = (style.legend.minText?.let { it(minVal) }) ?: "0%"
            val maxText = (style.legend.maxText?.let { it(maxVal) }) ?: "100%"
            val minLayout = measurer.measure(AnnotatedString(minText), style.legend.labelStyle)
            val maxLayout = measurer.measure(AnnotatedString(maxText), style.legend.labelStyle)
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

// Default cool→magenta→warm palette
public fun defaultCoolWarm(vIn: Float): Color {
    val v = vIn.coerceIn(0f, 1f)
    val c1 = Color(0xFF3A86FF)
    val c2 = Color(0xFFB049F8)
    val c3 = Color(0xFFFF7A33)
    return if (v < 0.5f) lerpColor(c1, c2, v * 2f) else lerpColor(c2, c3, (v - 0.5f) * 2f)
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val k = t.coerceIn(0f, 1f)
    return Color(
        red = (a.red + (b.red - a.red) * k),
        green = (a.green + (b.green - a.green) * k),
        blue = (a.blue + (b.blue - a.blue) * k),
        alpha = (a.alpha + (b.alpha - a.alpha) * k)
    )
}