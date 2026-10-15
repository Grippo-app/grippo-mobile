package com.grippo.design.components.metrics.engagement.streak

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.formatters.DateRangeFormatState
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

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
                textAlign = TextAlign.End,
            )
        },
    )
}

@AppPreview
@Composable
private fun TrainingStreakTimelineCardPreview() {
    PreviewContainer {
        stubTrainingStreaks().forEach { value ->
            TrainingStreakTimelineCard(
                entries = value.timeline.map { entry ->
                    entry.copy(
                        range = DateRangeFormatState.of(
                            from = LocalDateTime(2026, 10, 5, 0, 0),
                            to = LocalDateTime(2026, 10, 11, 23, 59),
                        ),
                    )
                }.toPersistentList(),
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
