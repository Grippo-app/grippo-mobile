package com.grippo.core.state.metrics.engagement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.highlight_streak
import com.grippo.design.resources.provider.highlight_streak_pattern_headline_primary
import com.grippo.design.resources.provider.highlight_streak_rhythm
import com.grippo.design.resources.provider.highlight_streak_weekly_headline_primary
import com.grippo.design.resources.provider.metric_overview_streak_daily_pattern
import com.grippo.design.resources.provider.metric_overview_streak_pattern_pattern
import com.grippo.design.resources.provider.metric_overview_streak_rhythm_pattern
import com.grippo.design.resources.provider.metric_overview_streak_weekly_pattern
import com.grippo.design.resources.provider.training_streak_details_cycle_headline
import com.grippo.design.resources.provider.training_streak_details_cycle_legend
import com.grippo.design.resources.provider.training_streak_details_cycle_method
import com.grippo.design.resources.provider.training_streak_details_cycle_value
import com.grippo.design.resources.provider.training_streak_details_daily_cadence
import com.grippo.design.resources.provider.training_streak_details_daily_headline
import com.grippo.design.resources.provider.training_streak_details_daily_legend
import com.grippo.design.resources.provider.training_streak_details_daily_method
import com.grippo.design.resources.provider.training_streak_details_no_sequence
import com.grippo.design.resources.provider.training_streak_details_pattern_cadence
import com.grippo.design.resources.provider.training_streak_details_recent_cycles
import com.grippo.design.resources.provider.training_streak_details_recent_days
import com.grippo.design.resources.provider.training_streak_details_recent_weeks
import com.grippo.design.resources.provider.training_streak_details_rhythm_cadence
import com.grippo.design.resources.provider.training_streak_details_weekly_cadence
import com.grippo.design.resources.provider.training_streak_details_weekly_headline
import com.grippo.design.resources.provider.training_streak_details_weekly_legend
import com.grippo.design.resources.provider.training_streak_details_weekly_method
import com.grippo.design.resources.provider.training_streak_details_weekly_value
import kotlinx.collections.immutable.ImmutableList

@Immutable
public sealed interface TrainingStreakFeaturedState {
    public val length: Int
    public val targetTrainingDaysPerPeriod: Int
    public val periodLengthDays: Int
    public val mood: TrainingStreakMood
    public val progressPercent: Int
    public val confidence: Float

    @Composable
    public fun overviewHeadline(): String {
        val resource = when (this) {
            is Daily -> Res.string.highlight_streak
            is Weekly -> Res.string.highlight_streak_weekly_headline_primary
            is Rhythm -> Res.string.highlight_streak_rhythm
            is Pattern -> Res.string.highlight_streak_pattern_headline_primary
        }
        return AppTokens.strings.res(resource, length)
    }

    @Composable
    public fun overviewCadence(): String = when (this) {
        is Daily -> AppTokens.strings.res(Res.string.metric_overview_streak_daily_pattern)
        is Weekly -> AppTokens.strings.res(
            Res.string.metric_overview_streak_weekly_pattern,
            targetTrainingDaysPerPeriod,
        )

        is Rhythm -> AppTokens.strings.res(
            Res.string.metric_overview_streak_rhythm_pattern,
            workDays,
            restDays,
        )

        is Pattern -> AppTokens.strings.res(
            Res.string.metric_overview_streak_pattern_pattern,
            targetTrainingDaysPerPeriod,
            periodLengthDays,
        )
    }

    @Composable
    public fun detailsHeadline(): String {
        if (length == 0) {
            return AppTokens.strings.res(Res.string.training_streak_details_no_sequence)
        }
        val resource = when (this) {
            is Daily -> Res.string.training_streak_details_daily_headline
            is Weekly -> Res.string.training_streak_details_weekly_headline
            is Rhythm, is Pattern -> Res.string.training_streak_details_cycle_headline
        }
        return AppTokens.strings.res(resource, length)
    }

    @Composable
    public fun cadenceDescription(): String = when (this) {
        is Daily -> AppTokens.strings.res(Res.string.training_streak_details_daily_cadence)
        is Weekly -> AppTokens.strings.res(
            Res.string.training_streak_details_weekly_cadence,
            targetTrainingDaysPerPeriod,
        )

        is Rhythm -> AppTokens.strings.res(
            Res.string.training_streak_details_rhythm_cadence,
            workDays,
            restDays,
        )

        is Pattern -> AppTokens.strings.res(
            Res.string.training_streak_details_pattern_cadence,
            targetTrainingDaysPerPeriod,
            periodLengthDays,
        )
    }

    @Composable
    public fun calculationDescription(): String = AppTokens.strings.res(
        when (this) {
            is Daily -> Res.string.training_streak_details_daily_method
            is Weekly -> Res.string.training_streak_details_weekly_method
            is Rhythm, is Pattern -> Res.string.training_streak_details_cycle_method
        },
    )

    @Composable
    public fun timelineTitle(): String = AppTokens.strings.res(
        when (this) {
            is Daily -> Res.string.training_streak_details_recent_days
            is Weekly -> Res.string.training_streak_details_recent_weeks
            is Rhythm, is Pattern -> Res.string.training_streak_details_recent_cycles
        },
    )

    @Composable
    public fun timelineDescription(): String = AppTokens.strings.res(
        when (this) {
            is Daily -> Res.string.training_streak_details_daily_legend
            is Weekly -> Res.string.training_streak_details_weekly_legend
            is Rhythm, is Pattern -> Res.string.training_streak_details_cycle_legend
        },
    )

    @Composable
    public fun timelineValue(entry: TrainingStreakProgressState): String {
        val resource = when (this) {
            is Daily, is Weekly -> Res.string.training_streak_details_weekly_value
            is Rhythm, is Pattern -> Res.string.training_streak_details_cycle_value
        }
        return AppTokens.strings.res(resource, entry.achievedTrainingDays, entry.targetTrainingDays)
    }

    @Immutable
    public data class Daily(
        override val length: Int,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeaturedState {
        override val targetTrainingDaysPerPeriod: Int = 1
        override val periodLengthDays: Int = 1
    }

    @Immutable
    public data class Weekly(
        override val length: Int,
        override val targetTrainingDaysPerPeriod: Int,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeaturedState {
        override val periodLengthDays: Int = 7
    }

    @Immutable
    public data class Rhythm(
        override val length: Int,
        val workDays: Int,
        val restDays: Int,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeaturedState {
        override val targetTrainingDaysPerPeriod: Int
            get() = workDays
        override val periodLengthDays: Int
            get() = workDays + restDays
    }

    @Immutable
    public data class Pattern(
        override val length: Int,
        val mask: ImmutableList<Boolean>,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeaturedState {
        override val targetTrainingDaysPerPeriod: Int
            get() = mask.count { it }

        override val periodLengthDays: Int
            get() = mask.size
    }
}
