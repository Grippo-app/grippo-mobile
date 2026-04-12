package com.grippo.training.exercise

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.trainings.ExerciseState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Immutable
internal data class ExerciseState(
    val exercise: ExerciseState,
    val exerciseExample: ExerciseExampleState? = null,
    val volumeArtifactIds: ImmutableSet<String> = persistentSetOf(),
    val repetitionArtifactIds: ImmutableSet<String> = persistentSetOf(),
)
