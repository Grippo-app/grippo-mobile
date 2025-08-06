package com.grippo.design.components.chart

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.grippo.chart.pie.PieChart
import com.grippo.chart.pie.PieChartData
import com.grippo.chart.pie.PieStyle
import com.grippo.chart.pie.PieText
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun PieChart(
    modifier: Modifier = Modifier,
    data: List<PieChartData>,
    textStyle: TextStyle
) {
    PieChart(
        modifier = modifier,
        data = data,
        style = PieStyle(
            pieText = PieText(textStyle = textStyle),
            chartBarWidth = AppTokens.dp.chart.pie.width
        )
    )
}

@AppPreview
@Composable
private fun PieChartPreview() {
    PreviewContainer {
        PieChart(
            data = listOf(
                PieChartData("Green", Color(0xFF4CAF50), 40L),
                PieChartData("Amber", Color(0xFFFFC107), 30L),
                PieChartData("Red", Color(0xFFF44336), 20L),
                PieChartData("Blue", Color(0xFF2196F3), 10L),
            ),
            modifier = Modifier.size(120.dp),
            textStyle = AppTokens.typography.b11Bold().copy(
                color = AppTokens.colors.muscle.text
            )
        )
    }
}