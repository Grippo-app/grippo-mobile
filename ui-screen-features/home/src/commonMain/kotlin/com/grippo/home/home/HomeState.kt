package com.grippo.home.home

import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.ExerciseSpotlightState
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.PerformanceMetricState
import com.grippo.core.state.metrics.TrainingLoadProfileState
import com.grippo.core.state.metrics.TrainingStreakState
import com.grippo.core.state.trainings.TrainingState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration

@Immutable
internal data class HomeState(
    val range: DateRange.Range = DateRange.Range.Last30Days(),
    val hasDraftTraining: Boolean = false,
    val lastTraining: TrainingState? = null,
    val totalDuration: Duration? = null,
    val muscleLoad: MuscleLoadSummaryState? = null,
    val streak: TrainingStreakState? = null,
    val performance: ImmutableList<PerformanceMetricState> = persistentListOf(),
    val profile: TrainingLoadProfileState? = null,
    val spotlights: ImmutableList<ExerciseSpotlightState> = persistentListOf(),
)
