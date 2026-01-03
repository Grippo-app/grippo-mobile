package com.grippo.home.home

import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.ExerciseSpotlightState
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.PerformanceMetricState
import com.grippo.core.state.metrics.TrainingStreakState
import com.grippo.core.state.metrics.digest.MonthlyDigestState
import com.grippo.core.state.metrics.digest.WeeklyDigestState
import com.grippo.core.state.trainings.TrainingState
import kotlin.time.Duration

@Immutable
internal data class HomeState(
    val lastTraining: TrainingState? = null,
    val weeklyDigest: WeeklyDigestState? = null,
    val monthlyDigest: MonthlyDigestState? = null,
    val totalDuration: Duration? = null,
    val spotlight: ExerciseSpotlightState? = null,
    val muscleLoad: MuscleLoadSummaryState? = null,
    val streak: TrainingStreakState? = null,
    val performance: List<PerformanceMetricState> = emptyList(),
)
