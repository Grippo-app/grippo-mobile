package com.grippo.design.components.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.chart.heatmap.HeatmapData
import com.grippo.chart.heatmap.Matrix01
import com.grippo.design.components.chart.internal.HeatmapChart
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.calculation.models.MuscleLoadMatrix

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

@AppPreview
@Composable
private fun MuscleHeatmapChartPreview() {
    PreviewContainer {
        MuscleHeatmapChart(
            value = MuscleLoadMatrix(
                rows = 4,
                cols = 7,
                values01 = listOf(
                    0.8f, 0.2f, 0.5f, 0.9f, 0.3f, 0.7f, 0.1f,
                    0.4f, 0.6f, 0.9f, 0.2f, 0.8f, 0.3f, 0.5f,
                    0.7f, 0.1f, 0.4f, 0.6f, 0.9f, 0.2f, 0.8f,
                    0.3f, 0.5f, 0.7f, 0.1f, 0.4f, 0.6f, 0.9f
                ),
                rowLabels = listOf("Chest", "Back", "Legs", "Arms"),
                colLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            )
        )
    }
}