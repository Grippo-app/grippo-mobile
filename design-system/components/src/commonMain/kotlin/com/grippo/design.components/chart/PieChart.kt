package com.grippo.design.components.chart

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.chart.pie.PieChart
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun PieChart(
    modifier: Modifier = Modifier,
    data: List<Pair<Color, Long>>,
) {
    PieChart(
        modifier = modifier,
        data = data,
        chartBarWidth = AppTokens.dp.chart.pie.width,
        paddingAngle = 2f,
    )
}

@AppPreview
@Composable
private fun PieChartPreview() {
    PreviewContainer {
        PieChart(
            data = listOf(
                Color(0xFF4CAF50) to 40L, // Green
                Color(0xFFFFC107) to 30L, // Amber
                Color(0xFFF44336) to 20L, // Red
                Color(0xFF2196F3) to 10L  // Blue
            ),
            modifier = Modifier
                .size(120.dp)
        )
    }
}