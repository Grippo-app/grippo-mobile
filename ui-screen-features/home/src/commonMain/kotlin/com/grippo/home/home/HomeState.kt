package com.grippo.home.home

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.PeriodFormatState
import com.grippo.core.state.metrics.ExerciseSpotlightState
import com.grippo.core.state.metrics.MonthlyDigestState
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.PerformanceMetricState
import com.grippo.core.state.metrics.TrainingStreakState
import com.grippo.core.state.metrics.WeeklyDigestState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@Immutable
internal data class HomeState(
    val period: PeriodFormatState = PeriodFormatState.of(
        DateRange(
            from = DateTimeUtils.minus(DateTimeUtils.now(), 90.days),
            to = DateTimeUtils.plus(DateTimeUtils.now(), 1.days)
        ),
    ),
    val lastTraining: TrainingState? = null,
    val weeklyDigest: WeeklyDigestState? = null,
    val monthlyDigest: MonthlyDigestState? = null,
    val totalDuration: Duration? = null,
    val spotlight: ExerciseSpotlightState? = null,
    val muscleLoad: MuscleLoadSummaryState? = null,
    val streak: TrainingStreakState? = null,
    val performance: List<PerformanceMetricState> = emptyList(),
)
