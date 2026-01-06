package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.TrainingTotalState
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.core.state.metrics.stubTotal
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.muscle.MuscleLoading
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun TrainingSummaryCard(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
    totalState: TrainingTotalState
) {
    MetricSectionPanel(modifier = modifier) {
        Text(
            text = totalState.volume.short(),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary
        )

        MuscleLoading(
            modifier = Modifier.fillMaxWidth(),
            summary = summary,
        )
    }
}

@AppPreview
@Composable
private fun TrainingSummaryCardPreview() {
    PreviewContainer {
        TrainingSummaryCard(
            summary = stubMuscleLoadSummary(),
            totalState = stubTotal()
        )
    }
}
