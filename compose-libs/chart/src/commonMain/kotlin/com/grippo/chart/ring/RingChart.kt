package com.grippo.chart.ring

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
public fun RingChart(
    modifier: Modifier = Modifier,
    data: RingData,
    style: RingStyle,
) {
    Canvas(modifier = modifier) {
        val domain = (data.max - data.min).takeIf { it > 0f } ?: 1f
        val progress = ((data.value - data.min) / domain).coerceIn(0f, 1f)

        val stroke = style.strokeWidth.toPx().coerceAtLeast(1f)
        val diameter = size.minDimension - stroke
        if (diameter <= 0f) return@Canvas

        val offset = (size.minDimension - diameter) / 2f
        val topLeft = Offset(offset, offset)
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = style.trackColor,
            startAngle = style.startAngle,
            sweepAngle = style.sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = style.cap)
        )

        if (progress > 0f) {
            drawArc(
                color = style.indicatorColor,
                startAngle = style.startAngle,
                sweepAngle = style.sweepAngle * progress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = style.cap)
            )
        }
    }
}
