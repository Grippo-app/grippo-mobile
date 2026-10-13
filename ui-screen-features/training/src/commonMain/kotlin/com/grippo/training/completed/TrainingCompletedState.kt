package com.grippo.training.completed

import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.trainings.TimelineState
import com.grippo.core.state.trainings.TrainingState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Immutable
@Serializable
internal data class TrainingCompletedState(
    val savedTrainingId: String? = null,
    @Transient
    val timeline: ImmutableList<TimelineState> = persistentListOf(),
    @Transient
    val training: TrainingState? = null,
    @Transient
    val muscleLoad: MuscleLoadSummaryState? = null,
)
