package com.grippo.design.components.training

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.trainings.highlight.Highlight
import com.grippo.core.state.trainings.highlight.TrainingStreakMood
import com.grippo.core.state.trainings.highlight.TrainingStreakType
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.sections.SectionPanel
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.highlight_active_days
import com.grippo.design.resources.provider.highlight_streak
import com.grippo.design.resources.provider.highlight_streak_daily_target
import com.grippo.design.resources.provider.highlight_streak_mood_crushing
import com.grippo.design.resources.provider.highlight_streak_mood_on_track
import com.grippo.design.resources.provider.highlight_streak_mood_restart
import com.grippo.design.resources.provider.highlight_streak_rhythm
import com.grippo.design.resources.provider.highlight_streak_rhythm_target
import com.grippo.design.resources.provider.highlight_streak_weekly_headline_primary
import com.grippo.design.resources.provider.highlight_streak_weekly_headline_secondary
import com.grippo.design.resources.provider.highlight_streak_weekly_target
import com.grippo.design.resources.provider.highlight_streaks

@Composable
public fun TrainingStreakSection(
    value: Highlight,
    modifier: Modifier = Modifier,
) {
    SectionPanel(modifier = modifier) {
        val title = AppTokens.strings.res(Res.string.highlight_streaks)
        val (headlinePrimary, headlineSecondary) = when (value.streak.featured.type) {
            TrainingStreakType.Daily -> AppTokens.strings.res(
                Res.string.highlight_streak,
                value.streak.featured.length
            ) to null

            TrainingStreakType.Weekly -> AppTokens.strings.res(
                Res.string.highlight_streak_weekly_headline_primary,
                value.streak.featured.length
            ) to AppTokens.strings.res(
                Res.string.highlight_streak_weekly_headline_secondary,
                value.streak.featured.targetSessionsPerPeriod
            )

            TrainingStreakType.Rhythm -> {
                val rhythm = value.streak.featured.rhythm ?: return@SectionPanel
                AppTokens.strings.res(
                    Res.string.highlight_streak_rhythm,
                    rhythm.workDays,
                    rhythm.restDays
                ) to null
            }
        }
        val cadenceLabel = when (value.streak.featured.type) {
            TrainingStreakType.Daily -> AppTokens.strings.res(Res.string.highlight_streak_daily_target)
            TrainingStreakType.Weekly -> AppTokens.strings.res(
                Res.string.highlight_streak_weekly_target,
                value.streak.featured.targetSessionsPerPeriod
            )

            TrainingStreakType.Rhythm -> {
                val rhythm = value.streak.featured.rhythm ?: return@SectionPanel
                AppTokens.strings.res(
                    Res.string.highlight_streak_rhythm_target,
                    rhythm.workDays,
                    rhythm.restDays
                )
            }
        }

        val progressValue =
            (value.streak.featured.progressPercent.coerceIn(0, 100)) / 100f

        val progressColors = when (value.streak.featured.mood) {
            TrainingStreakMood.CrushingIt -> AppTokens.colors.lineIndicator.success
            TrainingStreakMood.OnTrack -> AppTokens.colors.lineIndicator.info
            TrainingStreakMood.Restart -> AppTokens.colors.lineIndicator.warning
        }

        val moodLabel = when (value.streak.featured.mood) {
            TrainingStreakMood.CrushingIt -> AppTokens.strings.res(Res.string.highlight_streak_mood_crushing)
            TrainingStreakMood.OnTrack -> AppTokens.strings.res(Res.string.highlight_streak_mood_on_track)
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
                value.streak.totalActiveDays
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
