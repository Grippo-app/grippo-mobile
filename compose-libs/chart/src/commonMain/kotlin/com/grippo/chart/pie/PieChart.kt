package com.grippo.chart.pie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.TextLayoutResult

@Composable
public fun PieChart(
    modifier: Modifier = Modifier,
    data: PieData,
    style: PieStyle,
) {
    val textMeasurer = rememberTextMeasurer()

    val slices = data.slices
    // Precompute totals and base sweeps (true proportions, no distortion)
    val (sweeps, percents) = remember(slices) {
        val total = slices.sumOf { it.value.toDouble() }.toFloat()
        if (total <= 0f || slices.isEmpty()) {
            FloatArray(0) to IntArray(0)
        } else {
            val base = FloatArray(slices.size) { i ->
                360f * (slices[i].value / total)
            }
            val pc = IntArray(slices.size) { i ->
                ((slices[i].value / total) * 100f).toInt()
            }
            base to pc
        }
    }

    // Build label layouts via formatter
    val labelLayouts = remember(textMeasurer, style.labels, slices, percents) {
        if (sweeps.isEmpty()) emptyList()
        else slices.indices.map { i ->
            val (textStyle, formatter) = when (val labels = style.labels) {
                is PieStyle.Labels.Adaptive -> labels.textStyle to labels.formatter
                is PieStyle.Labels.Inside -> labels.textStyle to labels.formatter
                is PieStyle.Labels.Outside -> labels.textStyle to labels.formatter
            }
            val label = formatter(slices[i], percents.getOrElse(i) { 0 })
            textMeasurer.measure(AnnotatedString(label), textStyle)
        }
    }

    Canvas(modifier = modifier) {
        if (sweeps.isEmpty()) return@Canvas

        // Layout rect and radii
        val pad = style.layout.padding.toPx()
        val chart = Rect(pad, pad, size.width - pad, size.height - pad)
        if (chart.width <= 0f || chart.height <= 0f) return@Canvas

        val widthPx = style.arc.width.toPx()
        val cornerRadiusPx = style.arc.cornerRadius.toPx()
        val connectorWidthPx = style.leaders.lineWidth.toPx()
        val labelRadiusExtra = style.leaders.offset.toPx()
        val labelPaddingH = when (val labels = style.labels) {
            is PieStyle.Labels.Adaptive -> labels.labelPadding.toPx()
            is PieStyle.Labels.Inside -> labels.labelPadding.toPx()
            is PieStyle.Labels.Outside -> labels.labelPadding.toPx()
        }
        val epsDeg = 0.1f

        val center = Offset(chart.left + chart.width / 2f, chart.top + chart.height / 2f)
        val outerRadius = min(chart.width, chart.height) / 2f
        val innerRadius = outerRadius - widthPx
        val centerRadius = (outerRadius + innerRadius) / 2f

        // Collect outside-label candidates; inside labels will be drawn last on top
        data class OutsideLabel(
            val index: Int,
            val angleDeg: Float, // mid-angle (deg)
            var y: Float,        // desired y, will be adjusted
            val width: Int,
            val height: Int,
            val rightSide: Boolean
        )
        val outside = mutableListOf<OutsideLabel>()
        val insideLabels = mutableListOf<Pair<TextLayoutResult, Offset>>() // top-left positions

        // First pass: draw arcs and collect label positions (do not draw text yet)
        var acc = style.layout.startAngleDeg
        sweeps.forEachIndexed { i, baseSweep ->
            // Local padding limited by slice size to avoid eating tiny slices
            val localPad = style.arc.paddingAngleDeg
                .coerceAtMost(baseSweep / 2f - epsDeg)
                .coerceAtLeast(0f)

            val start = acc + localPad
            val drawSweep = (baseSweep - 2f * localPad).coerceAtLeast(0f)
            val mid = start + drawSweep / 2f

            // Draw rounded arc if visible
            if (drawSweep >= epsDeg) {
                drawRoundedArc(
                    color = slices[i].color,
                    startAngle = start,
                    sweepAngle = drawSweep,
                    width = widthPx,
                    cornerRadius = cornerRadiusPx,
                    center = center,
                    outerRadius = outerRadius
                )
            }

            // Prepare label placement only (defer drawing until after arcs)
            when (val labels = style.labels) {
                is PieStyle.Labels.Inside -> {
                    val layout = labelLayouts[i]
                    val midRad = mid * PI.toFloat() / 180f
                    val x = center.x + centerRadius * cos(midRad)
                    val y = center.y + centerRadius * sin(midRad)
                    insideLabels += layout to Offset(
                        x - layout.size.width / 2f,
                        y - layout.size.height / 2f
                    )
                }

                is PieStyle.Labels.Outside -> {
                    val layout = labelLayouts[i]
                    val midRad = mid * PI.toFloat() / 180f
                    val anchorY = center.y + (outerRadius + labelRadiusExtra) * sin(midRad)
                    val right = cos(midRad) >= 0f
                    outside += OutsideLabel(
                        index = i,
                        angleDeg = mid,
                        y = anchorY,
                        width = layout.size.width,
                        height = layout.size.height,
                        rightSide = right
                    )
                }

                is PieStyle.Labels.Adaptive -> {
                    if (baseSweep >= labels.insideMinAngleDeg) {
                        val layout = labelLayouts[i]
                        val midRad = mid * PI.toFloat() / 180f
                        val x = center.x + centerRadius * cos(midRad)
                        val y = center.y + centerRadius * sin(midRad)
                        insideLabels += layout to Offset(
                            x - layout.size.width / 2f,
                            y - layout.size.height / 2f
                        )
                    } else if (baseSweep >= labels.outsideMinAngleDeg && style.leaders.show) {
                        val layout = labelLayouts[i]
                        val midRad = mid * PI.toFloat() / 180f
                        val anchorY = center.y + (outerRadius + labelRadiusExtra) * sin(midRad)
                        val right = cos(midRad) >= 0f
                        outside += OutsideLabel(
                            index = i,
                            angleDeg = mid,
                            y = anchorY,
                            width = layout.size.width,
                            height = layout.size.height,
                            rightSide = right
                        )
                    }
                }
            }

            acc += baseSweep
        }

        // Helper to layout a side of outside labels and draw leaders + text
        fun layoutAndDrawOutsideSide(rightSide: Boolean) {
            val side = outside.filter { it.rightSide == rightSide }
                .sortedBy { it.y }
                .toMutableList()
            if (side.isEmpty()) return

            // Greedy vertical spacing (ladder)
            val minGap = 4.dp.toPx()
            for (k in 1 until side.size) {
                val prev = side[k - 1]
                val cur = side[k]
                val prevBottom = prev.y + prev.height / 2f
                val curTop = cur.y - cur.height / 2f
                if (curTop - prevBottom < minGap) {
                    val shift = (minGap - (curTop - prevBottom))
                    side[k] = cur.copy(y = cur.y + shift)
                }
            }

            // Draw leaders and labels (text last so it is above the lines)
            side.forEach { item ->
                val i = item.index
                val layout = labelLayouts[i]

                // Geometry points: arc point → elbow → label baseline
                val midRad = (item.angleDeg * PI.toFloat() / 180f)
                val arcPt = Offset(
                    x = center.x + outerRadius * cos(midRad),
                    y = center.y + outerRadius * sin(midRad)
                )
                val elbow = Offset(
                    x = center.x + (outerRadius + labelRadiusExtra) * cos(midRad),
                    y = center.y + (outerRadius + labelRadiusExtra) * sin(midRad)
                )

                val labelX = if (item.rightSide) {
                    elbow.x + labelPaddingH
                } else {
                    elbow.x - labelPaddingH - layout.size.width
                }
                val labelYTop = item.y - layout.size.height / 2f

                // Leader lines in slice color
                drawLine(
                    color = slices[i].color,
                    start = arcPt,
                    end = elbow,
                    strokeWidth = connectorWidthPx
                )
                drawLine(
                    color = slices[i].color,
                    start = elbow,
                    end = Offset(
                        x = if (item.rightSide) labelX else labelX + layout.size.width,
                        y = item.y
                    ),
                    strokeWidth = connectorWidthPx
                )

                // Text above the leader line
                drawText(layout, topLeft = Offset(labelX, labelYTop))
            }
        }

        // Second pass: resolve outside overlaps and draw leaders + outside labels
        if (outside.isNotEmpty() && style.leaders.show) {
            layoutAndDrawOutsideSide(rightSide = true)
            layoutAndDrawOutsideSide(rightSide = false)
        }

        // Final pass: draw inside/adaptive-inside labels on top of EVERYTHING
        insideLabels.forEach { (layout, topLeft) ->
            drawText(layout, topLeft = topLeft)
        }
    }
}

/**
 * Draws a segment of a pie chart with smooth rounded outer and inner corners.
 * Assumes donut style: [outerRadius] and [width] define inner radius.
 */
private fun DrawScope.drawRoundedArc(
    color: Color,
    startAngle: Float,
    sweepAngle: Float,
    width: Float,
    @androidx.annotation.FloatRange(from = 0.0, to = 1.0)
    alpha: Float = 1.0f,
    style: DrawStyle = Fill,
    center: Offset = this.center,
    outerRadius: Float = size.minDimension / 2f,
    colorFilter: androidx.compose.ui.graphics.ColorFilter? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
    cornerRadius: Float,
) {
    fun Float.toRad(): Float = this * PI.toFloat() / 180f
    fun Double.toDeg(): Float = (this * 180.0 / PI).toFloat()

    fun getPoint(angle: Float, radius: Float) = Offset(
        x = radius * cos(angle.toRad()) + center.x,
        y = radius * sin(angle.toRad()) + center.y
    )

    val endAngle = startAngle + sweepAngle
    val innerRadius = outerRadius - width

    fun coerceCorner(arcRadius: Float, cornerRadius: Float): Pair<Float, Float> {
        val safeRadius = cornerRadius.coerceAtMost(width / 2f)
        val cornerTravelAngle = 2 * asin((safeRadius / (2 * arcRadius)).toDouble()).toDeg()
        val maxSweep = sweepAngle / 2f
        return if (cornerTravelAngle > maxSweep) {
            Pair(arcRadius * sin(maxSweep.toRad()), maxSweep)
        } else {
            Pair(safeRadius, cornerTravelAngle)
        }
    }

    if (sweepAngle <= 0f) return

    val (cornerOuterRadius, cornerOuterSweep) = coerceCorner(outerRadius, cornerRadius)
    val (cornerInnerRadius, cornerInnerSweep) = coerceCorner(innerRadius, cornerRadius)

    val path = Path().apply {
        val topLeft = getPoint(startAngle, outerRadius)
        val topLeftStart = getPoint(startAngle, outerRadius - cornerOuterRadius)
        val topLeftEnd = getPoint(startAngle + cornerOuterSweep, outerRadius)

        val topRight = getPoint(endAngle, outerRadius)
        val topRightEnd = getPoint(endAngle, outerRadius - cornerOuterRadius)

        val bottomRight = getPoint(endAngle, innerRadius)
        val bottomRightStart = getPoint(endAngle, innerRadius + cornerInnerRadius)
        val bottomRightEnd = getPoint(endAngle - cornerInnerSweep, innerRadius)

        val bottomLeft = getPoint(startAngle, innerRadius)
        val bottomLeftEnd = getPoint(startAngle, innerRadius + cornerInnerRadius)

        moveTo(topLeftStart.x, topLeftStart.y)
        quadraticTo(topLeft.x, topLeft.y, topLeftEnd.x, topLeftEnd.y)

        // Outer arc
        val outerRect = Rect(
            center.x - outerRadius,
            center.y - outerRadius,
            center.x + outerRadius,
            center.y + outerRadius
        )
        arcTo(
            rect = outerRect,
            startAngleDegrees = startAngle + cornerOuterSweep,
            sweepAngleDegrees = sweepAngle - 2 * cornerOuterSweep,
            forceMoveTo = false
        )

        quadraticTo(topRight.x, topRight.y, topRightEnd.x, topRightEnd.y)
        lineTo(bottomRightStart.x, bottomRightStart.y)
        quadraticTo(bottomRight.x, bottomRight.y, bottomRightEnd.x, bottomRightEnd.y)

        // Inner arc (reverse)
        val innerRect = Rect(
            center.x - innerRadius,
            center.y - innerRadius,
            center.x + innerRadius,
            center.y + innerRadius
        )
        arcTo(
            rect = innerRect,
            startAngleDegrees = endAngle - cornerInnerSweep,
            sweepAngleDegrees = -(sweepAngle - 2 * cornerInnerSweep),
            forceMoveTo = false
        )

        quadraticTo(bottomLeft.x, bottomLeft.y, bottomLeftEnd.x, bottomLeftEnd.y)
        lineTo(topLeftStart.x, topLeftStart.y)
        close()
    }

    drawPath(
        path = path,
        color = color,
        style = style,
        alpha = alpha,
        colorFilter = colorFilter,
        blendMode = blendMode,
    )
}