package com.grippo.chart.pie

import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
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
import kotlin.math.sin

@Immutable
public data class PieChartData(
    val text: String,
    val color: Color,
    val value: Long,
)

@Composable
public fun PieChart(
    modifier: Modifier = Modifier,
    data: List<PieChartData>,
    style: PieStyle,
) {
    val textMeasurer = rememberTextMeasurer()

    // Precompute totals and base sweeps (true proportions, no distortion)
    val (sweeps, percents) = remember(data) {
        val total = data.sumOf { it.value }.toFloat()
        if (total <= 0f || data.isEmpty()) {
            FloatArray(0) to IntArray(0)
        } else {
            val base = FloatArray(data.size) { i ->
                360f * (data[i].value / total)
            }
            val pc = IntArray(data.size) { i ->
                // Round to int for clean UI
                ((data[i].value / total) * 100f).toInt()
            }
            base to pc
        }
    }

    // Pre-build labels (name + percent)
    val labelLayouts = remember(textMeasurer, style.pieText?.textStyle, data, percents) {
        if (style.pieText == null || sweeps.isEmpty()) emptyList()
        else data.indices.map { i ->
            val label = data[i].text
            textMeasurer.measure(
                text = AnnotatedString(label),
                style = style.pieText.textStyle
            )
        }
    }

    Canvas(modifier = modifier) {
        if (sweeps.isEmpty()) return@Canvas

        // Convert dp → px once per frame
        val widthPx = style.chartBarWidth.toPx()
        val cornerRadiusPx = style.cornerRadius.toPx()
        val connectorWidthPx = 1.dp.toPx()
        val labelRadiusExtra = 8.dp.toPx()      // how far labels sit outside the ring
        val labelPaddingH = 6.dp.toPx()         // horizontal padding from leader end to text
        val minOutsideAngle = 5f                // below this angle we skip outside labels
        val epsDeg = 0.1f                       // geometric epsilon

        val outerRadius = size.minDimension / 2f
        val innerRadius = outerRadius - widthPx
        val centerRadius = (outerRadius + innerRadius) / 2f

        var acc = 0f

        // Collect candidates for outside labels to resolve overlaps per hemisphere
        data class OutsideLabel(
            val index: Int,
            val angleDeg: Float, // mid-angle (deg)
            var y: Float,        // desired y, will be adjusted
            val width: Int,
            val height: Int,
            val rightSide: Boolean
        )

        val outside = mutableListOf<OutsideLabel>()

        // First pass: draw arcs and prepare label positions
        sweeps.forEachIndexed { i, baseSweep ->
            // Use local padding limited by slice size to avoid "eating" tiny slices
            val localPad = style.paddingAngle
                .coerceAtMost(baseSweep / 2f - epsDeg)
                .coerceAtLeast(0f)

            val start = acc + localPad
            val drawSweep = (baseSweep - 2f * localPad).coerceAtLeast(0f)
            val mid = start + drawSweep / 2f

            // Draw rounded arc if there is something visible
            if (drawSweep >= epsDeg) {
                drawRoundedArc(
                    color = data[i].color,
                    startAngle = start,
                    sweepAngle = drawSweep,
                    width = widthPx,
                    cornerRadius = cornerRadiusPx,
                    center = center,
                    outerRadius = outerRadius
                )
            }

            // Labels: inside for big slices, outside for small slices
            style.pieText?.let {
                if (baseSweep >= style.minVisibleAngle && labelLayouts.isNotEmpty()) {
                    // Inside label
                    val layout = labelLayouts[i]
                    val midRad = mid * PI.toFloat() / 180f
                    val x = center.x + centerRadius * cos(midRad)
                    val y = center.y + centerRadius * sin(midRad)
                    drawText(
                        layout,
                        topLeft = Offset(
                            x - layout.size.width / 2f,
                            y - layout.size.height / 2f
                        )
                    )
                } else if (baseSweep >= minOutsideAngle && labelLayouts.isNotEmpty()) {
                    // Prepare outside label (position resolved in second pass)
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

            acc += baseSweep
        }

        // Second pass: resolve outside label overlaps per hemisphere and draw leaders + text
        if (outside.isNotEmpty() && style.pieText != null) {
            fun layoutSide(rightSide: Boolean) {
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

                // Draw leaders and labels
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
                        // place text to the right
                        elbow.x + labelPaddingH
                    } else {
                        // place text to the left (right-aligned)
                        elbow.x - labelPaddingH - layout.size.width
                    }
                    val labelYTop = item.y - layout.size.height / 2f

                    // Leader lines
                    drawLine(
                        color = data[i].color, // use slice color
                        start = arcPt,
                        end = elbow,
                        strokeWidth = connectorWidthPx
                    )
                    drawLine(
                        color = data[i].color, // use slice color
                        start = elbow,
                        end = Offset(
                            x = if (item.rightSide) labelX else labelX + layout.size.width,
                            y = item.y
                        ),
                        strokeWidth = connectorWidthPx
                    )

                    // Text
                    drawText(
                        layout,
                        topLeft = Offset(labelX, labelYTop),
                    )
                }
            }

            layoutSide(rightSide = true)
            layoutSide(rightSide = false)
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
    @FloatRange(from = 0.0, to = 1.0)
    alpha: Float = 1.0f,
    style: DrawStyle = Fill,
    center: Offset = this.center,
    outerRadius: Float = size.minDimension / 2f,
    colorFilter: ColorFilter? = null,
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
        arcTo(
            rect = Rect(center = center, radius = outerRadius),
            startAngleDegrees = startAngle + cornerOuterSweep,
            sweepAngleDegrees = sweepAngle - 2 * cornerOuterSweep,
            forceMoveTo = false
        )
        quadraticTo(topRight.x, topRight.y, topRightEnd.x, topRightEnd.y)
        lineTo(bottomRightStart.x, bottomRightStart.y)
        quadraticTo(bottomRight.x, bottomRight.y, bottomRightEnd.x, bottomRightEnd.y)
        arcTo(
            rect = Rect(center = center, radius = innerRadius),
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