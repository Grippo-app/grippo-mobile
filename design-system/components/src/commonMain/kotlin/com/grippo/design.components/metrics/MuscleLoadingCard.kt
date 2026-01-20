package com.grippo.design.components.metrics

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.highlight_muscle_focus

@Composable
public fun MuscleLoadingCard(
    summary: MuscleLoadSummaryState,
    modifier: Modifier = Modifier,
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.highlight_muscle_focus),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val clippedData = remember(summary) {
            summary.copy(
                perMuscle = summary.perMuscle.copy(entries = summary.perMuscle.entries.take(4)),
                perGroup = summary.perGroup.copy(entries = summary.perGroup.entries.take(4)),
            )
        }

        MuscleLoading(
            summary = clippedData,
            mode = MuscleLoadingMode.PerGroup,
            style = MuscleLoadingStyle.Collapsed
        )
    }
}

@AppPreview
@Composable
private fun MuscleLoadingCardPreview() {
    PreviewContainer {
        MuscleLoadingCard(
            summary = stubMuscleLoadSummary(),
        )
    }
}