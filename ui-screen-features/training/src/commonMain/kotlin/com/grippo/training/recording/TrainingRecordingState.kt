package com.grippo.training.recording

import androidx.compose.runtime.Immutable
import com.grippo.calculation.models.DistributionBreakdown
import com.grippo.calculation.models.MetricSeries
import com.grippo.calculation.models.MuscleLoadSummary
import com.grippo.date.utils.DateTimeUtils
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingMetrics
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime

@Immutable
internal data class TrainingRecordingState(
    val tab: RecordingTab = RecordingTab.Exercises,

    // === Main data ===
    val exercises: ImmutableList<ExerciseState> = persistentListOf(),
    val startAt: LocalDateTime = DateTimeUtils.now(),

    // === Filters / Sorting ===
    val examples: ImmutableList<ExerciseExampleState> = persistentListOf(),
    val muscles: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>> = persistentListOf(),

    // === Basic metrics chips ===
    val totalMetrics: TrainingMetrics? = null,

    // === Exercise volume (bar) ===
    val exerciseVolume: MetricSeries? = null,

    // === Muscle analysis (progress/heatmap) ===
    val muscleLoad: MuscleLoadSummary? = null,

    // === Exercise example distributions (pie) ===
    val categoryDistribution: DistributionBreakdown? = null,
    val weightTypeDistribution: DistributionBreakdown? = null,
    val forceTypeDistribution: DistributionBreakdown? = null
)

@Immutable
internal enum class RecordingTab {
    Exercises,
    Stats
}
