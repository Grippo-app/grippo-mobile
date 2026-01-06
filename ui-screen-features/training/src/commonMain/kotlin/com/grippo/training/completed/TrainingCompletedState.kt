package com.grippo.training.completed

import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.TrainingTotalState
import com.grippo.core.state.trainings.TimelineState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class TrainingCompletedState(
    val timeline: ImmutableList<TimelineState> = persistentListOf(),
    val total: TrainingTotalState? = null,
    val summary: MuscleLoadSummaryState? = null,
)
