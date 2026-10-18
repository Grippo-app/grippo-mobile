package com.grippo.design.components.metrics.engagement.streak

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.metrics.engagement.TrainingStreakProgressState
import com.grippo.core.state.metrics.engagement.stubTrainingStreaks
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_streak_period_value
import com.grippo.design.resources.provider.training_streak_partial_period
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun TrainingStreakTimelineCard(
    modifier: Modifier = Modifier,
    entries: ImmutableList<TrainingStreakProgressState>,
) {
    if (entries.isEmpty()) return

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
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
        entry.progressPercent >= 100 -> AppTokens.colors.charts.indicator.success
        entry.isPartial -> AppTokens.colors.charts.indicator.info
        else -> AppTokens.colors.charts.indicator.warning
    }

    LineIndicator(
        modifier = Modifier.fillMaxWidth(),
        progress = (entry.progressPercent.coerceIn(0, 100)) / 100f,
        colors = progressColors,
        labelSpacing = AppTokens.dp.contentPadding.text,
        startLabel = {
            Text(
                text = entry.range.short(),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.primary,
            )
        },
        endLabel = {
            Text(
                text = AppTokens.strings.res(
                    Res.string.training_streak_period_value,
                    entry.achievedTrainingDays,
                    entry.targetTrainingDays
                ),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.End,
            )
        },
    )

    if (entry.isPartial) {
        Text(
            text = AppTokens.strings.res(Res.string.training_streak_partial_period),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakTimelineCardPreview() {
    PreviewContainer {
        stubTrainingStreaks().forEach { value ->
            TrainingStreakTimelineCard(
                entries = value.timeline,
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingStreakTimelineCardEmptyPreview() {
    PreviewContainer {
        TrainingStreakTimelineCard(entries = persistentListOf())
    }
}
