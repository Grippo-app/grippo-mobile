package com.grippo.design.components.metrics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.TrainingStreakState
import com.grippo.core.state.metrics.stubTrainingStreaks
import com.grippo.design.components.chart.internal.RadarChart
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun TrainingProfileCard(
    modifier: Modifier = Modifier,
    value: TrainingStreakState,
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
        RadarChart(

        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardPreviewCardPreview() {
    PreviewContainer {
        TrainingStreakCard(
            value = stubTrainingStreaks().random(),
        )
    }
}
