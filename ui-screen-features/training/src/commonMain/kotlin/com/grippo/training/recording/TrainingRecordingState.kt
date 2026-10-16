package com.grippo.training.recording

import androidx.compose.runtime.Immutable
import com.grippo.core.state.ImmutableListSerializer
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.core.state.stage.StageState
import com.grippo.core.state.trainings.ExerciseState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Immutable
@Serializable
internal data class TrainingRecordingState(
    val stage: StageState,
    val startAt: LocalDateTime,
    @Serializable(with = ImmutableListSerializer::class)
    val exercises: ImmutableList<ExerciseState>? = null,
    @Transient
    val examples: ImmutableList<ExerciseExampleState> = persistentListOf(),
    @Transient
    val muscles: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>> = persistentListOf(),
)
