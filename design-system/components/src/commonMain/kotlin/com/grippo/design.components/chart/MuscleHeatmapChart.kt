package com.grippo.design.components.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.toolkit.calculation.models.MuscleLoadMatrix
import com.grippo.chart.heatmap.HeatmapData
import com.grippo.chart.heatmap.Matrix01
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

private fun MuscleLoadMatrix.toHeatmapData(): HeatmapData {
    val m = Matrix01.fromFlat(
        rows = rows,
        cols = cols,
        values01 = values01
    )
    return HeatmapData(
        matrix = m,
        rowLabels = rowLabels,
        colLabels = colLabels,
    )
}