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
    style: HeatmapStyle,
) {
    val measurer = rememberTextMeasurer()

    androidx.compose.foundation.Canvas(modifier) {
        if (data.matrix.rows <= 0 || data.matrix.cols <= 0) return@Canvas

        // ----- Normalize domain (optional) -----
        val values = data.matrix.values
        var minVal = 0f
        var maxVal = 1f
        if (style.palette.autoNormalize && values.isNotEmpty()) {
            minVal = values.min()
            maxVal = values.max()
            if (maxVal == minVal) maxVal += 1f
        }
        fun norm(v: Float): Float =
            if (!style.palette.autoNormalize) v else (v - minVal) / (maxVal - minVal)

        // ----- Gutters for labels/legend (no external padding) -----
        val labelPadPx = style.layout.labelPadding.toPx()

        var leftGutter = 0f
        when (val rLbl = style.rowLabels) {
            is HeatmapStyle.AxisLabels.ShowAll -> {
                if (data.rowLabels.isNotEmpty()) {
                    val maxW = (0 until data.matrix.rows).maxOf { r ->
                        val text = data.rowLabels.getOrNull(r) ?: ""
                        measurer.measure(AnnotatedString(text), rLbl.textStyle).size.width
                    }
                    leftGutter += maxW + labelPadPx
                }
            }

            is HeatmapStyle.AxisLabels.Adaptive -> {
                if (data.rowLabels.isNotEmpty()) {
                    val maxW = (0 until data.matrix.rows).maxOf { r ->
                        val text = data.rowLabels.getOrNull(r) ?: ""
                        measurer.measure(AnnotatedString(text), rLbl.textStyle).size.width
                    }
                    leftGutter += maxW + labelPadPx
                }
            }

            is HeatmapStyle.AxisLabels.None -> Unit
        }

        var bottomGutter = 0f
        when (val cLbl = style.colLabels) {
            is HeatmapStyle.AxisLabels.ShowAll -> {
                if (data.colLabels.isNotEmpty()) {
                    val maxH = (0 until data.matrix.cols).maxOf { c ->
                        val text = data.colLabels.getOrNull(c) ?: ""
                        measurer.measure(AnnotatedString(text), cLbl.textStyle).size.height
                    }
                    bottomGutter += maxH + labelPadPx
                }
            }

            is HeatmapStyle.AxisLabels.Adaptive -> {
                if (data.colLabels.isNotEmpty()) {
                    val maxH = (0 until data.matrix.cols).maxOf { c ->
                        val text = data.colLabels.getOrNull(c) ?: ""
                        measurer.measure(AnnotatedString(text), cLbl.textStyle).size.height
                    }
                    bottomGutter += maxH + labelPadPx
                }
            }

            is HeatmapStyle.AxisLabels.None -> Unit
        }

        val legendH = when (val lg = style.legend) {
            is HeatmapStyle.Legend.Visible -> lg.height.toPx()
            is HeatmapStyle.Legend.None -> 0f
        }
        if (legendH > 0f) bottomGutter += legendH + labelPadPx

        val chart = Rect(leftGutter, 0f, size.width, size.height - bottomGutter)
        val chartW = chart.width.coerceAtLeast(0f)
        val chartH = chart.height.coerceAtLeast(0f)
        if (chartW <= 0f || chartH <= 0f) return@Canvas

        // ----- Cell geometry -----
        val gap = style.layout.gap.toPx()
        val cw =
            if (data.matrix.cols > 0) (chartW - gap * (data.matrix.cols - 1)) / data.matrix.cols else 0f
        val ch =
            if (data.matrix.rows > 0) (chartH - gap * (data.matrix.rows - 1)) / data.matrix.rows else 0f
        val rx = style.layout.corner.toPx()

        // Direct matrix access

        // Background for missing cells
        style.palette.missingCellColor?.let { bg ->
            for (r in 0 until data.matrix.rows) {
                for (c in 0 until data.matrix.cols) {
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
        for (r in 0 until data.matrix.rows) {
            for (c in 0 until data.matrix.cols) {
                val v = data.matrix[r, c]
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

                when (val b = style.borders) {
                    is HeatmapStyle.Borders.Visible -> if (b.borderWidth.value > 0f) {
                        drawRoundRect(
                            color = b.borderColor,
                            topLeft = Offset(x, y),
                            size = Size(cw, ch),
                            cornerRadius = CornerRadius(rx, rx),
                            style = Stroke(width = b.borderWidth.toPx())
                        )
                    }

                    is HeatmapStyle.Borders.None -> Unit
                }

                when (val v = style.values) {
                    is HeatmapStyle.Values.Visible -> {
                        val pTxt = v.formatter(t, data)
                        val txtColor = chooseContrastingText(color, v.textStyle.color)
                        val layout = measurer.measure(
                            AnnotatedString(pTxt),
                            v.textStyle.copy(color = txtColor)
                        )
                        val cx = x + (cw - layout.size.width) / 2f
                        val cy = y + (ch - layout.size.height) / 2f
                        drawText(layout, topLeft = Offset(cx, cy))
                    }

                    is HeatmapStyle.Values.None -> Unit
                }
            }
        }

        // Row labels
        when (val rLbl = style.rowLabels) {
            is HeatmapStyle.AxisLabels.ShowAll -> if (data.rowLabels.isNotEmpty()) {
                for (r in 0 until data.matrix.rows) {
                    val text = data.rowLabels.getOrNull(r) ?: ""
                    val layout = measurer.measure(AnnotatedString(text), rLbl.textStyle)
                    val y = chart.top + r * (ch + gap) + (ch - layout.size.height) / 2f
                    val x = chart.left - labelPadPx - layout.size.width
                    drawText(layout, topLeft = Offset(x, y))
                }
            }

            is HeatmapStyle.AxisLabels.Adaptive -> if (data.rowLabels.isNotEmpty()) {
                val minGapPx = rLbl.minGapDp.toPx()
                // try to thin rows by step so that label boxes (height) have gap
                val layouts = (0 until data.matrix.rows).map { r ->
                    val text = data.rowLabels.getOrNull(r) ?: ""
                    measurer.measure(AnnotatedString(text), rLbl.textStyle)
                }
                var step = 1
                while (true) {
                    var ok = true
                    var prevBottom = Float.NEGATIVE_INFINITY
                    var r = 0
                    while (r < data.matrix.rows) {
                        val l = layouts[r]
                        val y = chart.top + r * (ch + gap) + (ch - l.size.height) / 2f
                        val top = y
                        if (top < prevBottom + minGapPx) {
                            ok = false; break
                        }
                        prevBottom = top + l.size.height
                        r += step
                    }
                    if (ok) break
                    step++
                    if (step > data.matrix.rows) break
                }
                var r = 0
                while (r < data.matrix.rows) {
                    val l = layouts[r]
                    val y = chart.top + r * (ch + gap) + (ch - l.size.height) / 2f
                    val x = chart.left - labelPadPx - l.size.width
                    drawText(l, topLeft = Offset(x, y))
                    r += step
                }
            }

            is HeatmapStyle.AxisLabels.None -> Unit
        }

        // Column labels
        when (val cLbl = style.colLabels) {
            is HeatmapStyle.AxisLabels.ShowAll -> if (data.colLabels.isNotEmpty()) {
                for (c in 0 until data.matrix.cols) {
                    val text = data.colLabels.getOrNull(c) ?: ""
                    val layout = measurer.measure(AnnotatedString(text), cLbl.textStyle)
                    val x = chart.left + c * (cw + gap) + (cw - layout.size.width) / 2f
                    val y = chart.bottom + labelPadPx / 2f
                    drawText(layout, topLeft = Offset(x, y))
                }
            }

            is HeatmapStyle.AxisLabels.Adaptive -> if (data.colLabels.isNotEmpty()) {
                val minGapPx = cLbl.minGapDp.toPx()
                val centers = (0 until data.matrix.cols).map { c ->
                    chart.left + c * (cw + gap) + cw / 2f
                }
                val layouts = (0 until data.matrix.cols).map { c ->
                    val text = data.colLabels.getOrNull(c) ?: ""
                    measurer.measure(AnnotatedString(text), cLbl.textStyle)
                }

                data class L(
                    val left: Float,
                    val right: Float,
                    val layout: androidx.compose.ui.text.TextLayoutResult
                )

                val boxes = (0 until data.matrix.cols).map { c ->
                    val layout = layouts[c]
                    val w = layout.size.width.toFloat()
                    val cx = centers[c]
                    val left = (cx - w / 2f).coerceIn(chart.left, chart.right - w)
                    L(left, left + w, layout)
                }
                val selected = mutableListOf<Int>()
                var lastRight = Float.NEGATIVE_INFINITY
                for (i in 0 until data.matrix.cols) {
                    val b = boxes[i]
                    if (b.left >= lastRight + minGapPx) {
                        selected.add(i)
                        lastRight = b.right
                    }
                }
                // Ensure last visible if possible
                if (selected.isNotEmpty() && selected.last() != data.matrix.cols - 1) {
                    val lastB = boxes.last()
                    while (selected.isNotEmpty()) {
                        val tail = boxes[selected.last()]
                        if (lastB.left >= tail.right + minGapPx) break
                        selected.removeLast()
                    }
                    selected.add(data.matrix.cols - 1)
                }
                selected.forEach { i ->
                    val b = boxes[i]
                    val y = chart.bottom + labelPadPx / 2f
                    drawText(b.layout, topLeft = Offset(b.left, y))
                }
            }

            is HeatmapStyle.AxisLabels.None -> Unit
        }

        // Legend
        when (val lg = style.legend) {
            is HeatmapStyle.Legend.None -> Unit
            is HeatmapStyle.Legend.Visible -> {
                val legendTop = size.height - lg.height.toPx()
                val legendRect =
                    Rect(chart.left, legendTop, chart.right, legendTop + lg.height.toPx())
                val brush = legendBrush(lg.stops)
                drawRect(
                    brush = brush,
                    topLeft = Offset(legendRect.left, legendRect.top),
                    size = Size(legendRect.width, legendRect.height)
                )
                val minText = (lg.minText?.let { it(minVal) }) ?: "0%"
                val maxText = (lg.maxText?.let { it(maxVal) }) ?: "100%"
                val minLayout = measurer.measure(AnnotatedString(minText), lg.labelStyle)
                val maxLayout = measurer.measure(AnnotatedString(maxText), lg.labelStyle)
                drawText(
                    minLayout,
                    topLeft = Offset(legendRect.left, legendRect.bottom + 2.dp.toPx())
                )
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