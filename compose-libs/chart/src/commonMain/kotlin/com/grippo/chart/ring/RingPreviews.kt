package com.grippo.chart.ring

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun RingChartPreview() {
    val style = RingStyle(
        strokeWidth = 12.dp,
        trackColor = Color(0x1A000000),
        indicatorColor = Color(0xFF6AA9FF),
        startAngle = -90f,
        sweepAngle = 360f
    )

    RingChart(
        modifier = Modifier.size(120.dp),
        data = RingData(value = 62f, min = 0f, max = 100f),
        style = style
    )
}
