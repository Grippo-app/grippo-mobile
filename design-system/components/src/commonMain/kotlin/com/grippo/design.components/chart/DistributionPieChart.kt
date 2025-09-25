package com.grippo.design.components.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.calculation.models.DistributionBreakdown
import com.grippo.calculation.models.DistributionSlice
import com.grippo.design.components.chart.internal.DSPieData
import com.grippo.design.components.chart.internal.DSPieSlice
import com.grippo.design.components.chart.internal.PieChart

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

private fun DistributionBreakdown.toPieData(): DSPieData = DSPieData(
    slices = slices.map { it.toPieSlice() }
)

private fun DistributionSlice.toPieSlice(): DSPieSlice = DSPieSlice(
    id = id,
    label = label,
    value = value,
    color = color,
)