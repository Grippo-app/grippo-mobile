package com.grippo.home.home

import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.DigestState
import com.grippo.core.state.metrics.ExerciseSpotlightState
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.PerformanceMetricState
import com.grippo.core.state.metrics.TrainingLoadProfileState
import com.grippo.core.state.metrics.TrainingStreakState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.toolkit.date.utils.DateRange
import kotlin.time.Duration

@Immutable
internal data class HomeState(
    val range: DateRange.Range = DateRange.Range.Monthly(),
    val lastTraining: TrainingState? = null,
    val digest: DigestState? = null,
    val totalDuration: Duration? = null,
    val muscleLoad: MuscleLoadSummaryState? = null,
    val streak: TrainingStreakState? = null,
    val performance: List<PerformanceMetricState> = emptyList(),
    val profile: TrainingLoadProfileState? = null,
    val consistent: ExerciseSpotlightState.MostConsistentState? = null,
    val missing: ExerciseSpotlightState.ComebackMissingState? = null,
    val best: ExerciseSpotlightState.BestProgressState? = null,
)
