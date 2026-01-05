package com.grippo.design.components.metrics

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.muscle.MuscleLoading
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.highlight_muscle_focus

@Composable
public fun MuscleLoadSection(
    summary: MuscleLoadSummaryState,
    modifier: Modifier = Modifier,
) {
    MetricSectionPanel(modifier = modifier) {
        Text(
            text = AppTokens.strings.res(Res.string.highlight_muscle_focus),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        MuscleLoading(
            summary = summary,
            maxVisibleEntries = 4
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadSectionPreview() {
    PreviewContainer {
        MuscleLoadSection(summary = stubMuscleLoadSummary())
    }
}

@AppPreview
@Composable
private fun MuscleLoadingPreview() {
    PreviewContainer {
        MuscleLoading(summary = stubMuscleLoadSummary())
    }
}
