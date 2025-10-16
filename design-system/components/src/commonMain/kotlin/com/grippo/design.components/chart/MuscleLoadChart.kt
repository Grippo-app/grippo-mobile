package com.grippo.design.components.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.chart.progress.ProgressChartData
import com.grippo.chart.progress.ProgressData
import com.grippo.design.components.chart.internal.ProgressChart
import com.grippo.toolkit.calculation.models.MuscleLoadSummary

@Composable
public fun MuscleLoadChart(
    modifier: Modifier = Modifier,
    value: MuscleLoadSummary,
) {
    val chartData = remember(value) {
        value.toProgressData()
    }

    ProgressChart(
        modifier = modifier,
        data = chartData,
    )
}

private fun MuscleLoadSummary.toProgressData(): ProgressData = ProgressData(
    items = perGroup.entries.map { entry ->
        ProgressChartData(
            label = entry.label,
            value = entry.value,
            color = entry.color,
        )
    }
)
