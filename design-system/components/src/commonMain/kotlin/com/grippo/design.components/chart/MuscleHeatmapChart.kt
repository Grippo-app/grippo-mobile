package com.grippo.design.components.chart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.chart.heatmap.HeatmapData
import com.grippo.chart.heatmap.Matrix01
import com.grippo.core.state.metrics.MuscleLoadTimelineState
import com.grippo.core.state.metrics.stubMuscleLoadTimeline
import com.grippo.design.components.chart.internal.HeatmapChart
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun MuscleHeatmapChart(
    modifier: Modifier = Modifier,
    state: MuscleLoadTimelineState,
) {
    val data = remember(state) {
        state.toHeatmapData()
    }

    HeatmapChart(
        modifier = modifier,
        data = data,
    )
}

private fun MuscleLoadTimelineState.toHeatmapData(): HeatmapData {
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
            state = stubMuscleLoadTimeline()
        )
    }
}
