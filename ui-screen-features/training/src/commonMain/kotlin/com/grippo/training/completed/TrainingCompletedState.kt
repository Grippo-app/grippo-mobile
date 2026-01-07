package com.grippo.training.completed

import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.trainings.TimelineState
import com.grippo.core.state.trainings.TrainingState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class TrainingCompletedState(
    val timeline: ImmutableList<TimelineState> = persistentListOf(),
    val training: TrainingState? = null,
    val summary: MuscleLoadSummaryState? = null,
    val message: String? = null,
)
