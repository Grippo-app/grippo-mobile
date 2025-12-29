package com.grippo.design.components.chart

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.chart.bar.BarData
import com.grippo.chart.bar.BarEntry
import com.grippo.design.components.chart.internal.BarChart
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.calculation.models.MetricPoint
import com.grippo.toolkit.calculation.models.MetricSeries

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

private fun MetricSeries.toBarData(): BarData = BarData(
    items = points.map { it.toBarItem() }
)

private fun MetricPoint.toBarItem(): BarEntry = BarEntry(
    label = label,
    value = value,
    color = color,
)

@AppPreview
@Composable
private fun MetricBarChartPreview() {
    PreviewContainer {
        MetricBarChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            value = MetricSeries(
                points = listOf(
                    MetricPoint(
                        label = "Mon",
                        value = 120f,
                        color = AppTokens.colors.brand.color1
                    ),
                    MetricPoint(
                        label = "Tue",
                        value = 90f,
                        color = AppTokens.colors.brand.color2
                    ),
                    MetricPoint(
                        label = "Wed",
                        value = 150f,
                        color = AppTokens.colors.brand.color3
                    ),
                    MetricPoint(
                        label = "Thu",
                        value = 110f,
                        color = AppTokens.colors.brand.color4
                    ),
                    MetricPoint(
                        label = "Fri",
                        value = 180f,
                        color = AppTokens.colors.brand.color5
                    ),
                    MetricPoint(
                        label = "Sat",
                        value = 200f,
                        color = AppTokens.colors.brand.color6
                    ),
                    MetricPoint(
                        label = "Sun",
                        value = 95f,
                        color = AppTokens.colors.brand.color1
                    )
                )
            )
        )
    }
}