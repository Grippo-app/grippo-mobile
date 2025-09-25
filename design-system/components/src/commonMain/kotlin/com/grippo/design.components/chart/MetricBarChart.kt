package com.grippo.design.components.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.calculation.models.MetricPoint
import com.grippo.calculation.models.MetricSeries
import com.grippo.design.components.chart.internal.BarChart
import com.grippo.design.components.chart.internal.DSBarData
import com.grippo.design.components.chart.internal.DSBarItem

@Composable
public fun MetricBarChart(
    modifier: Modifier = Modifier,
    value: MetricSeries,
) {
    val data = remember(value) {
        value.toBarData()
    }

    BarChart(
        modifier = modifier,
        data = data,
    )
}

private fun MetricSeries.toBarData(): DSBarData = DSBarData(
    items = points.map { it.toBarItem() }
)

private fun MetricPoint.toBarItem(): DSBarItem = DSBarItem(
    label = label,
    value = value,
    color = color,
)