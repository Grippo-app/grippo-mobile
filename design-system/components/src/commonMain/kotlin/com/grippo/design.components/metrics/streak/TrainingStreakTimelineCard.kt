package com.grippo.design.components.metrics.streak

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.TrainingStreakProgressState
import com.grippo.core.state.metrics.stubTrainingStreaks
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_streak_period_value
import com.grippo.design.resources.provider.training_streak_timeline_title

@Composable
public fun TrainingStreakTimelineCard(
    modifier: Modifier = Modifier,
    entries: List<TrainingStreakProgressState>,
) {
    if (entries.isEmpty()) return

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.training_streak_timeline_title),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary
        )

        entries.asReversed().forEach { entry ->
            TrainingStreakTimelineRow(
                entry = entry
            )
        }
    }
}

@Composable
private fun TrainingStreakTimelineRow(
    entry: TrainingStreakProgressState,
) {
    val progressColors = when {
        entry.progressPercent >= 80 -> AppTokens.colors.charts.indicator.success
        entry.progressPercent >= 40 -> AppTokens.colors.charts.indicator.info
        else -> AppTokens.colors.charts.indicator.warning
    }

    LineIndicator(
        modifier = Modifier.fillMaxWidth(),
        progress = (entry.progressPercent.coerceIn(0, 100)) / 100f,
        colors = progressColors,
        labelSpacing = AppTokens.dp.contentPadding.text,
        startLabel = {
            Text(
                text = entry.range.display,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
            )
        },
        endLabel = {
            Text(
                text = AppTokens.strings.res(
                    Res.string.training_streak_period_value,
                    entry.achievedSessions,
                    entry.targetSessions
                ),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
                maxLines = 1,
            )
        },
    )
}

@AppPreview
@Composable
private fun TrainingStreakTimelineCardPreview() {
    PreviewContainer {
        TrainingStreakTimelineCard(
            entries = stubTrainingStreaks().random().timeline
        )
        TrainingStreakTimelineCard(
            entries = stubTrainingStreaks().random().timeline
        )
        TrainingStreakTimelineCard(
            entries = stubTrainingStreaks().random().timeline
        )
        TrainingStreakTimelineCard(
            entries = stubTrainingStreaks().random().timeline
        )
    }
}
