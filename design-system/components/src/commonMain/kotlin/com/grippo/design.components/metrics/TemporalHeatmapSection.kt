package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.MuscleLoadTimelineState
import com.grippo.core.state.metrics.stubMuscleLoadTimeline
import com.grippo.design.components.chart.MuscleHeatmapChart
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun TemporalHeatmapSection(
    state: MuscleLoadTimelineState,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    MetricSectionPanel(modifier = modifier) {
        title?.let {
            Text(
                text = it,
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
                maxLines = 1
            )
        }

        MuscleHeatmapChart(
            modifier = Modifier
                .fillMaxWidth(),
            state = state,
        )
    }
}

@AppPreview
@Composable
private fun TemporalHeatmapSectionPreview() {
    PreviewContainer {
        TemporalHeatmapSection(
            state = stubMuscleLoadTimeline(),
            modifier = Modifier.fillMaxWidth(),
            title = "Heatmap"
        )
    }
}
