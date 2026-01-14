package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.core.state.metrics.TrainingStreakFeaturedState
import com.grippo.core.state.metrics.TrainingStreakMood
import com.grippo.core.state.metrics.TrainingStreakState
import com.grippo.core.state.metrics.stubTrainingStreaks
import com.grippo.design.components.indicators.LineIndicator
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
import com.grippo.design.resources.provider.highlight_streak_pattern_headline_secondary
import com.grippo.design.resources.provider.highlight_streak_pattern_target
import com.grippo.design.resources.provider.highlight_streak_rhythm
import com.grippo.design.resources.provider.highlight_streak_rhythm_target
import com.grippo.design.resources.provider.highlight_streak_weekly_headline_primary
import com.grippo.design.resources.provider.highlight_streak_weekly_headline_secondary
import com.grippo.design.resources.provider.highlight_streak_weekly_target
import com.grippo.design.resources.provider.highlight_streaks

@Composable
public fun TrainingStreakCard(
    modifier: Modifier = Modifier,
    value: TrainingStreakState,
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
        val title = AppTokens.strings.res(Res.string.highlight_streaks)

        val (headlinePrimary, headlineSecondary) = when (val featured = value.featured) {
            is TrainingStreakFeaturedState.Daily -> AppTokens.strings.res(
                Res.string.highlight_streak,
                featured.length
            ) to null

            is TrainingStreakFeaturedState.Weekly -> AppTokens.strings.res(
                Res.string.highlight_streak_weekly_headline_primary,
                featured.length
            ) to AppTokens.strings.res(
                Res.string.highlight_streak_weekly_headline_secondary,
                featured.targetSessionsPerPeriod
            )

            is TrainingStreakFeaturedState.Rhythm -> AppTokens.strings.res(
                Res.string.highlight_streak_rhythm,
                featured.workDays,
                featured.restDays
            ) to null

            is TrainingStreakFeaturedState.Pattern -> AppTokens.strings.res(
                Res.string.highlight_streak_pattern_headline_primary,
                featured.length
            ) to AppTokens.strings.res(
                Res.string.highlight_streak_pattern_headline_secondary,
                featured.targetSessionsPerPeriod,
                featured.periodLengthDays,
            )
        }
        val cadenceLabel = when (val featured = value.featured) {
            is TrainingStreakFeaturedState.Daily ->
                AppTokens.strings.res(Res.string.highlight_streak_daily_target)

            is TrainingStreakFeaturedState.Weekly -> AppTokens.strings.res(
                Res.string.highlight_streak_weekly_target,
                featured.targetSessionsPerPeriod
            )

            is TrainingStreakFeaturedState.Rhythm -> AppTokens.strings.res(
                Res.string.highlight_streak_rhythm_target,
                featured.workDays,
                featured.restDays
            )

            is TrainingStreakFeaturedState.Pattern -> AppTokens.strings.res(
                Res.string.highlight_streak_pattern_target,
                featured.targetSessionsPerPeriod,
                featured.periodLengthDays,
            )
        }

        val progressValue =
            (value.featured.progressPercent.coerceIn(0, 100)) / 100f

        val progressColors = when (value.featured.mood) {
            TrainingStreakMood.CrushingIt -> AppTokens.colors.lineIndicator.success
            TrainingStreakMood.OnTrack -> AppTokens.colors.lineIndicator.info
            TrainingStreakMood.Paused -> AppTokens.colors.lineIndicator.muted
            TrainingStreakMood.Restart -> AppTokens.colors.lineIndicator.warning
        }

        val moodLabel = when (value.featured.mood) {
            TrainingStreakMood.CrushingIt -> AppTokens.strings.res(Res.string.highlight_streak_mood_crushing)
            TrainingStreakMood.OnTrack -> AppTokens.strings.res(Res.string.highlight_streak_mood_on_track)
            TrainingStreakMood.Paused -> AppTokens.strings.res(Res.string.highlight_streak_mood_paused)
            TrainingStreakMood.Restart -> AppTokens.strings.res(Res.string.highlight_streak_mood_restart)
        }

        Text(
            text = title,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Column {
            Text(
                text = headlinePrimary,
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Hack to avoid missing IntrinsicSize in LazyGrid
            Spacer(Modifier.height(5.dp))

            headlineSecondary?.let { secondary ->
                Text(
                    text = secondary,
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = cadenceLabel,
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        LineIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = progressValue,
            colors = progressColors
        )

        Text(
            text = AppTokens.strings.res(
                Res.string.highlight_active_days,
                value.totalActiveDays
            ),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = moodLabel,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
