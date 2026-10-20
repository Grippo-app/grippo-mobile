package com.grippo.design.components.metrics.engagement.streak

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.metrics.engagement.TrainingStreakFeaturedState
import com.grippo.core.state.metrics.engagement.TrainingStreakState
import com.grippo.core.state.metrics.engagement.stubTrainingStreaks
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.metrics.MetricCardPresentation
import com.grippo.design.components.metrics.internal.MetricOverviewHeader
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Flame
import com.grippo.design.resources.provider.metric_overview_streak_active_days
import com.grippo.design.resources.provider.metric_overview_streak_title
import com.grippo.design.resources.provider.training_streak_current_progress

@Composable
public fun TrainingStreakCard(
    modifier: Modifier = Modifier,
    value: TrainingStreakState,
    presentation: MetricCardPresentation = MetricCardPresentation.Detailed,
) {
    if (presentation == MetricCardPresentation.Detailed) {
        TrainingStreakDetailSummary(modifier = modifier, value = value)
        return
    }

    val title = AppTokens.strings.res(Res.string.metric_overview_streak_title)

    val headlinePrimary = value.featured.overviewHeadline()
    val cadenceLabel = value.featured.overviewCadence()

    val progressValue =
        (value.featured.progressPercent.coerceIn(0, 100)) / 100f

    val progressColors = value.featured.mood.indicatorColors()

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Large,
    ) {
        MetricOverviewHeader(title = title)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(2.5f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.size(AppTokens.dp.metrics.overview.rhythmIcon),
                        imageVector = AppTokens.icons.Flame,
                        tint = progressColors.dominant,
                        contentDescription = null,
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = headlinePrimary,
                        style = AppTokens.typography.h4(),
                        color = AppTokens.colors.text.primary,
                    )
                }
                Text(
                    text = cadenceLabel,
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    text = value.totalActiveDays.toString(),
                    style = AppTokens.typography.h3(),
                    color = AppTokens.colors.text.primary,
                    textAlign = TextAlign.End,
                )
                Text(
                    text = AppTokens.strings.res(Res.string.metric_overview_streak_active_days),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                    textAlign = TextAlign.End,
                )
            }
        }

        LineIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = progressValue,
            colors = progressColors,
        )

        value.timeline.lastOrNull()?.let { currentPeriod ->
            Text(
                text = AppTokens.strings.res(
                    Res.string.training_streak_current_progress,
                    currentPeriod.achievedTrainingDays,
                    currentPeriod.targetTrainingDays,
                ),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@Composable
private fun TrainingStreakDetailSummary(
    modifier: Modifier = Modifier,
    value: TrainingStreakState,
) {
    MetricSectionPanel(modifier = modifier, style = MetricSectionPanelStyle.Large) {
        Text(
            text = value.summaryHeadline(),
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
        )
        Text(
            text = value.summaryDescription(),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
        )
        if (value.featured.length > 0) {
            Text(
                text = value.activeDaysDescription(),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardDetailedDailyPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[0],
            presentation = MetricCardPresentation.Detailed,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardDetailedWeeklyPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[1],
            presentation = MetricCardPresentation.Detailed,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardDetailedRhythmPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[2],
            presentation = MetricCardPresentation.Detailed,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardDetailedPatternPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[3],
            presentation = MetricCardPresentation.Detailed,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardDetailedRestartPreview() {
    val streak = stubTrainingStreaks()[1]
    val featured = streak.featured as TrainingStreakFeaturedState.Weekly
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = streak.copy(featured = featured.copy(length = 0)),
            presentation = MetricCardPresentation.Detailed,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardOverviewDailyPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[0],
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardOverviewWeeklyPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[1],
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardOverviewRhythmPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[2],
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardOverviewPatternPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[3],
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardOverviewRestartPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubTrainingStreaks()[4],
            presentation = MetricCardPresentation.Overview,
        )
    }
}
