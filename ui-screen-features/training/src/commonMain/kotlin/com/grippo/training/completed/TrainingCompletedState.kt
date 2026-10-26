package com.grippo.training.completed

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.engagement.TrainingStreakState
import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.core.state.metrics.performance.TrainingRecordState
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.volume.WeeklyVolumeState
import com.grippo.core.state.trainings.TrainingState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Immutable
@Serializable
internal data class TrainingCompletedState(
    val savedTrainingId: String? = null,
    val reference: PerformanceReferenceState = PerformanceReferenceState.Average,
    @Transient
    val training: TrainingState? = null,
    @Transient
    val muscleLoad: MuscleLoadSummaryState? = null,
    @Transient
    val range: DateRangeFormatState = DateRangeFormatState.Empty(),
    @Transient
    val performance: PerformanceComparisonState? = null,
    @Transient
    val records: ImmutableList<TrainingRecordState> = persistentListOf(),
    @Transient
    val weeklyVolume: WeeklyVolumeState? = null,
    @Transient
    val streak: TrainingStreakState? = null,
    @Transient
    val previousStreak: TrainingStreakState? = null,
    @Transient
    val goalProgress: GoalProgressState? = null,
    @Transient
    val previousGoalProgress: GoalProgressState? = null,
    @Transient
    val recordCelebration: RecordCelebrationState = RecordCelebrationState.Pending,
)
