package com.grippo.design.components.chart

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.grippo.chart.pie.PieChart
import com.grippo.design.core.AppTokens

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