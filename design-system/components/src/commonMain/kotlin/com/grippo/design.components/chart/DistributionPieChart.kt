package com.grippo.design.components.chart

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.chart.pie.PieData
import com.grippo.chart.pie.PieSlice
import com.grippo.design.components.chart.internal.PieChart
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.calculation.models.DistributionBreakdown
import com.grippo.toolkit.calculation.models.DistributionSlice

@Composable
public fun DistributionPieChart(
    modifier: Modifier = Modifier,
    value: DistributionBreakdown,
) {
    val data = remember(value) {
        value.toPieData()
    }

    PieChart(
        modifier = modifier,
        data = data,
    )
}

private fun DistributionBreakdown.toPieData(): PieData = PieData(
    slices = slices.map { it.toPieSlice() }
)

private fun DistributionSlice.toPieSlice(): PieSlice = PieSlice(
    id = id,
    label = label,
    value = value,
    color = color,
)

@AppPreview
@Composable
private fun DistributionPieChartPreview() {
    PreviewContainer {
        DistributionPieChart(
            modifier = Modifier.size(200.dp),
            value = DistributionBreakdown(
                slices = listOf(
                    DistributionSlice(
                        id = "1",
                        label = "Chest",
                        value = 30f,
                        color = AppTokens.colors.semantic.success
                    ),
                    DistributionSlice(
                        id = "2",
                        label = "Back",
                        value = 25f,
                        color = AppTokens.colors.semantic.info
                    ),
                    DistributionSlice(
                        id = "3",
                        label = "Legs",
                        value = 20f,
                        color = AppTokens.colors.semantic.warning
                    ),
                    DistributionSlice(
                        id = "4",
                        label = "Arms",
                        value = 15f,
                        color = AppTokens.colors.semantic.error
                    ),
                    DistributionSlice(
                        id = "5",
                        label = "Shoulders",
                        value = 10f,
                        color = AppTokens.colors.brand.color3
                    )
                )
            )
        )
    }
}