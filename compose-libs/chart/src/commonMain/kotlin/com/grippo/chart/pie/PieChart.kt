package com.grippo.chart.pie

import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

//  Based on design assessment by human
private const val EMPIRICAL_VISIBLE_MIN_ANGLE = 4

// https://medium.com/@developerchunk/create-custom-pie-chart-with-animations-in-jetpack-compose-android-studio-kotlin-49cf95ef321e
@Composable
public fun PieChart(
    modifier: Modifier = Modifier,
    data: List<Pair<Color, Long>>,
    chartBarWidth: Dp = 22.dp,
    paddingAngle: Float = 2f,
) {
    var lastValue = 0f

    Canvas(modifier = modifier) {
        val minAngle = (2 * paddingAngle + EMPIRICAL_VISIBLE_MIN_ANGLE)

        val totalSum = data.sumOf { it.second }
        val floatValue = mutableListOf<Float>()
        var difference = 0f

        data.forEachIndexed { index, values ->
            val sectionAngle = 360 * values.second.toFloat() / totalSum.toFloat()
            val safeSectionAngle = sectionAngle.coerceAtLeast(minAngle)
            if (safeSectionAngle > sectionAngle) {
                difference += safeSectionAngle - sectionAngle
            }
            floatValue.add(index, safeSectionAngle)
        }

        val remainingAngleCoefficient = (360 - difference) / 360
        floatValue.forEachIndexed { index, value ->
            if (value > minAngle) {
                floatValue[index] = value * remainingAngleCoefficient
            }
        }

        floatValue.forEachIndexed { index, value ->
            drawRoundedArc(
                color = data.getOrNull(index)?.first ?: Color.Transparent,
                startAngle = lastValue + paddingAngle,
                sweepAngle = value - paddingAngle * 2,
                width = chartBarWidth.toPx(),
                cornerRadius = 6.dp.toPx(),
            )
            lastValue += value
        }
    }
}

/**
 * Draws a segment of a pie chart with smooth rounded outer and inner corners.
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
    outerRadius: Float = size.minDimension / 2,
    colorFilter: ColorFilter? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
    cornerRadius: Float,
) {

    fun Float.toRadians(): Float = this * PI.toFloat() / 180f

    fun Double.toDegrees(): Float = (this * 180.0 / PI).toFloat()

    fun getPoint(angle: Float, radius: Float) = Offset(
        x = radius * cos(angle.toRadians()) + center.x,
        y = radius * sin(angle.toRadians()) + center.y
    )

    val endAngle = startAngle + sweepAngle
    val innerRadius = outerRadius - width

    fun coerceCorner(arcRadius: Float, cornerRadius: Float): Pair<Float, Float> {
        val safeRadius = cornerRadius.coerceAtMost(width / 2)
        val cornerTravelAngle = 2 * asin((safeRadius / (2 * arcRadius)).toDouble()).toDegrees()
        val maxSweep = sweepAngle / 2
        return if (cornerTravelAngle > maxSweep) {
            Pair(arcRadius * sin(maxSweep.toRadians()), maxSweep)
        } else {
            Pair(safeRadius, cornerTravelAngle)
        }
    }

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