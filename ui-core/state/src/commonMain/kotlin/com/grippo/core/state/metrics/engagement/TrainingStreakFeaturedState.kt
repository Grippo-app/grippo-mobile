package com.grippo.core.state.metrics.engagement

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.highlight_streak
import com.grippo.design.resources.provider.highlight_streak_cycles
import com.grippo.design.resources.provider.highlight_streak_weekly_headline_primary
import com.grippo.design.resources.provider.metric_overview_streak_cycle_days
import com.grippo.design.resources.provider.metric_overview_streak_daily_pattern
import com.grippo.design.resources.provider.metric_overview_streak_pattern_cadence
import com.grippo.design.resources.provider.metric_overview_streak_rest_days
import com.grippo.design.resources.provider.metric_overview_streak_rhythm_cadence
import com.grippo.design.resources.provider.metric_overview_streak_training_days
import com.grippo.design.resources.provider.metric_overview_streak_weekly_days
import com.grippo.design.resources.provider.training_streak_details_cycle_method
import com.grippo.design.resources.provider.training_streak_details_cycle_value
import com.grippo.design.resources.provider.training_streak_details_cycle_value_none
import com.grippo.design.resources.provider.training_streak_details_daily_method
import com.grippo.design.resources.provider.training_streak_details_recent_cycles
import com.grippo.design.resources.provider.training_streak_details_recent_days
import com.grippo.design.resources.provider.training_streak_details_recent_weeks
import com.grippo.design.resources.provider.training_streak_details_weekly_method
import com.grippo.design.resources.provider.training_streak_none
import com.grippo.design.resources.provider.training_streak_period_days
import com.grippo.design.resources.provider.training_streak_period_days_none
import com.grippo.design.resources.provider.training_streak_today_done
import com.grippo.design.resources.provider.training_streak_today_pending
import com.grippo.toolkit.date.utils.DateTimeUtils
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
    public fun headline(): String {
        if (length == 0) {
            return AppTokens.strings.res(Res.string.training_streak_none)
        }
        return when (this) {
            is Daily -> AppTokens.strings.res(Res.plurals.highlight_streak, length, length)
            is Weekly -> AppTokens.strings.res(Res.plurals.highlight_streak_weekly_headline_primary, length, length)
            is Rhythm, is Pattern -> AppTokens.strings.res(Res.plurals.highlight_streak_cycles, length, length)
        }
    }

    @Composable
    public fun cadence(): String = when (this) {
        is Daily -> AppTokens.strings.res(Res.string.metric_overview_streak_daily_pattern)
        is Weekly -> AppTokens.strings.res(
            Res.plurals.metric_overview_streak_weekly_days,
            targetTrainingDaysPerPeriod,
            targetTrainingDaysPerPeriod,
        )

        is Rhythm -> AppTokens.strings.res(
            Res.string.metric_overview_streak_rhythm_cadence,
            AppTokens.strings.res(Res.plurals.metric_overview_streak_training_days, workDays, workDays),
            AppTokens.strings.res(Res.plurals.metric_overview_streak_rest_days, restDays, restDays),
        )

        is Pattern -> AppTokens.strings.res(
            Res.string.metric_overview_streak_pattern_cadence,
            AppTokens.strings.res(
                Res.plurals.metric_overview_streak_training_days,
                targetTrainingDaysPerPeriod,
                targetTrainingDaysPerPeriod,
            ),
            AppTokens.strings.res(Res.plurals.metric_overview_streak_cycle_days, periodLengthDays, periodLengthDays),
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
    public fun periodValue(entry: TrainingStreakProgressState): String {
        val achieved = entry.achievedTrainingDays
        val target = entry.targetTrainingDays
        return when (this) {
            is Daily, is Weekly -> if (achieved == 0) {
                AppTokens.strings.res(Res.string.training_streak_period_days_none, target)
            } else {
                AppTokens.strings.res(
                    Res.string.training_streak_period_days,
                    AppTokens.strings.res(Res.plurals.metric_overview_streak_training_days, achieved, achieved),
                    target,
                )
            }

            is Rhythm, is Pattern -> if (achieved == 0) {
                AppTokens.strings.res(Res.string.training_streak_details_cycle_value_none)
            } else {
                AppTokens.strings.res(Res.plurals.training_streak_details_cycle_value, target, achieved, target)
            }
        }
    }

    @Composable
    public fun currentPeriodLine(entry: TrainingStreakProgressState): String? {
        if (this !is Daily) return periodValue(entry)
        if (entry.range.value?.to?.date != DateTimeUtils.today()) return null
        val resource = if (entry.achievedTrainingDays > 0) {
            Res.string.training_streak_today_done
        } else {
            Res.string.training_streak_today_pending
        }
        return AppTokens.strings.res(resource)
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
