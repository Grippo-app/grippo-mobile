package com.grippo.design.components.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.calculation.models.MuscleLoadMatrix
import com.grippo.design.components.chart.internal.DSHeatmapData
import com.grippo.design.components.chart.internal.HeatmapChart

@Composable
public fun MuscleHeatmapChart(
    modifier: Modifier = Modifier,
    value: MuscleLoadMatrix,
) {
    val data = remember(value) {
        value.toHeatmapData()
    }

    HeatmapChart(
        modifier = modifier,
        data = data,
    )
}

private fun MuscleLoadMatrix.toHeatmapData(): DSHeatmapData = DSHeatmapData(
    rows = rows,
    cols = cols,
    values01 = values01,
    rowLabels = rowLabels,
    colLabels = colLabels,
)