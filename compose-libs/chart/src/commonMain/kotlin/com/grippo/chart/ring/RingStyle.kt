package com.grippo.chart.ring

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
public data class RingStyle(
    val strokeWidth: Dp = 12.dp,
    val trackColor: Color,
    val indicatorColor: Color,
    val startAngle: Float = -90f,
    val sweepAngle: Float = 360f,
    val cap: StrokeCap = StrokeCap.Round,
)
