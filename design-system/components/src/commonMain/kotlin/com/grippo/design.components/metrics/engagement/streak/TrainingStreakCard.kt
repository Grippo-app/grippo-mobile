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
import com.grippo.core.state.metrics.engagement.TrainingStreakMood
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
import com.grippo.design.resources.provider.highlight_active_days
import com.grippo.design.resources.provider.highlight_streak
import com.grippo.design.resources.provider.highlight_streak_daily_target
import com.grippo.design.resources.provider.highlight_streak_mood_crushing
import com.grippo.design.resources.provider.highlight_streak_mood_on_track
import com.grippo.design.resources.provider.highlight_streak_mood_paused
import com.grippo.design.resources.provider.highlight_streak_mood_restart
import com.grippo.design.resources.provider.highlight_streak_pattern_headline_primary
import com.grippo.design.resources.provider.highlight_streak_pattern_target
import com.grippo.design.resources.provider.highlight_streak_rhythm
import com.grippo.design.resources.provider.highlight_streak_rhythm_target
import com.grippo.design.resources.provider.highlight_streak_weekly_headline_primary
import com.grippo.design.resources.provider.highlight_streak_weekly_target
import com.grippo.design.resources.provider.highlight_streaks
import com.grippo.design.resources.provider.icons.Flame
import com.grippo.design.resources.provider.metric_overview_streak_active_days
import com.grippo.design.resources.provider.metric_overview_streak_daily_pattern
import com.grippo.design.resources.provider.metric_overview_streak_pattern_pattern
import com.grippo.design.resources.provider.metric_overview_streak_rhythm_pattern
import com.grippo.design.resources.provider.metric_overview_streak_title
import com.grippo.design.resources.provider.metric_overview_streak_weekly_pattern
import com.grippo.design.resources.provider.training_streak_current_progress
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun TrainingStreakCard(
    modifier: Modifier = Modifier,
    value: TrainingStreakState,
    presentation: MetricCardPresentation = MetricCardPresentation.Detailed,
) {
    val title = AppTokens.strings.res(
        if (presentation == MetricCardPresentation.Overview) {
            Res.string.metric_overview_streak_title
        } else {
            Res.string.highlight_streaks
        },
    )

    val headlinePrimary = when (val featured = value.featured) {
        is TrainingStreakFeaturedState.Daily -> AppTokens.strings.res(
            Res.string.highlight_streak,
            featured.length
        )

        is TrainingStreakFeaturedState.Weekly -> AppTokens.strings.res(
            Res.string.highlight_streak_weekly_headline_primary,
            featured.length
        )

        is TrainingStreakFeaturedState.Rhythm -> AppTokens.strings.res(
            Res.string.highlight_streak_rhythm,
            featured.length,
        )

        is TrainingStreakFeaturedState.Pattern -> AppTokens.strings.res(
            Res.string.highlight_streak_pattern_headline_primary,
            featured.length
        )
    }
    val cadenceLabel = if (presentation == MetricCardPresentation.Overview) {
        when (val featured = value.featured) {
            is TrainingStreakFeaturedState.Daily -> AppTokens.strings.res(
                Res.string.metric_overview_streak_daily_pattern,
            )

            is TrainingStreakFeaturedState.Weekly -> AppTokens.strings.res(
                Res.string.metric_overview_streak_weekly_pattern,
                featured.targetTrainingDaysPerPeriod,
            )

            is TrainingStreakFeaturedState.Rhythm -> AppTokens.strings.res(
                Res.string.metric_overview_streak_rhythm_pattern,
                featured.workDays,
                featured.restDays,
            )

            is TrainingStreakFeaturedState.Pattern -> AppTokens.strings.res(
                Res.string.metric_overview_streak_pattern_pattern,
                featured.targetTrainingDaysPerPeriod,
                featured.periodLengthDays,
            )
        }
    } else {
        when (val featured = value.featured) {
            is TrainingStreakFeaturedState.Daily ->
                AppTokens.strings.res(Res.string.highlight_streak_daily_target)

            is TrainingStreakFeaturedState.Weekly -> AppTokens.strings.res(
                Res.string.highlight_streak_weekly_target,
                featured.targetTrainingDaysPerPeriod
            )

            is TrainingStreakFeaturedState.Rhythm -> AppTokens.strings.res(
                Res.string.highlight_streak_rhythm_target,
                featured.workDays,
                featured.restDays
            )

            is TrainingStreakFeaturedState.Pattern -> AppTokens.strings.res(
                Res.string.highlight_streak_pattern_target,
                featured.targetTrainingDaysPerPeriod,
                featured.periodLengthDays,
            )
        }
    }

    val progressValue =
        (value.featured.progressPercent.coerceIn(0, 100)) / 100f

    val progressColors = when (value.featured.mood) {
        TrainingStreakMood.CrushingIt -> AppTokens.colors.charts.indicator.success
        TrainingStreakMood.OnTrack -> AppTokens.colors.charts.indicator.info
        TrainingStreakMood.Paused -> AppTokens.colors.charts.indicator.muted
        TrainingStreakMood.Restart -> AppTokens.colors.charts.indicator.warning
    }

    MetricSectionPanel(
        modifier = modifier,
        style = if (presentation == MetricCardPresentation.Overview) {
            MetricSectionPanelStyle.Large
        } else {
            MetricSectionPanelStyle.Small
        },
    ) {
        if (presentation == MetricCardPresentation.Overview) {
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
        } else {
            Text(
                text = title,
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )

            Text(
                text = headlinePrimary,
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary,
            )

            Text(
                text = cadenceLabel,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
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

        if (presentation == MetricCardPresentation.Detailed) {
            val moodLabel = when (value.featured.mood) {
                TrainingStreakMood.CrushingIt -> AppTokens.strings.res(Res.string.highlight_streak_mood_crushing)
                TrainingStreakMood.OnTrack -> AppTokens.strings.res(Res.string.highlight_streak_mood_on_track)
                TrainingStreakMood.Paused -> AppTokens.strings.res(Res.string.highlight_streak_mood_paused)
                TrainingStreakMood.Restart -> AppTokens.strings.res(Res.string.highlight_streak_mood_restart)
            }

            Text(
                text = AppTokens.strings.res(Res.string.highlight_active_days, value.totalActiveDays),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
            Text(
                text = moodLabel,
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.tertiary,
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
            value = trainingStreakCardPreviewValues()[0],
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
            value = trainingStreakCardPreviewValues()[1],
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
            value = trainingStreakCardPreviewValues()[2],
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
            value = trainingStreakCardPreviewValues()[3],
            presentation = MetricCardPresentation.Detailed,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakCardDetailedRestartPreview() {
    PreviewContainer {
        TrainingStreakCard(
            modifier = Modifier.fillMaxWidth(),
            value = trainingStreakCardPreviewValues()[4],
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
            value = trainingStreakCardPreviewValues()[0],
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
            value = trainingStreakCardPreviewValues()[1],
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
            value = trainingStreakCardPreviewValues()[2],
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
            value = trainingStreakCardPreviewValues()[3],
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
            value = trainingStreakCardPreviewValues()[4],
            presentation = MetricCardPresentation.Overview,
        )
    }
}

private fun trainingStreakCardPreviewValues(): List<TrainingStreakState> {
    val values = stubTrainingStreaks()
    val pattern = TrainingStreakFeaturedState.Pattern(
        length = 12,
        mask = persistentListOf(true, false, true, false, true, false, false, false),
        mood = TrainingStreakMood.OnTrack,
        progressPercent = 67,
        confidence = 0.8f,
    )
    val patternValue = values.first().copy(
        totalActiveDays = 128,
        featured = pattern,
        historyDays = 365,
        timeline = persistentListOf(),
    )
    return values + patternValue + patternValue.copy(
        totalActiveDays = 0,
        featured = pattern.copy(
            length = 0,
            mood = TrainingStreakMood.Restart,
            progressPercent = 0,
            confidence = 0.5f,
        ),
        lastTrainingGapDays = 7,
    )
}
