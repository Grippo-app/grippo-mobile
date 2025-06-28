package com.grippo.chart.pie

import androidx.annotation.FloatRange
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
public fun ProgressChart(
    modifier: Modifier = Modifier,
    @FloatRange(from = 0.0, to = 1.0) progress: Float,
    startAngle: Float = -90f,
    chartBarWidth: Dp = 3.dp,
    progressColor: Color,
    backgroundColor: Color,
) {
    Canvas(modifier = modifier) {
        drawArc(
            color = backgroundColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(chartBarWidth.toPx(), cap = StrokeCap.Round),
        )

        drawArc(
            color = progressColor,
            startAngle = startAngle,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(chartBarWidth.toPx(), cap = StrokeCap.Round),
        )
    }
}
