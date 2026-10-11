package com.grippo.training.exercise

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.data.features.api.training.models.ExerciseValidationContext
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.LocalDateTime

@Immutable
internal data class TrainingExerciseState(
    val exercise: ExerciseState,
    val exerciseExample: ExerciseExampleState? = null,
    val volumeArtifactIds: ImmutableSet<String> = persistentSetOf(),
    val repetitionArtifactIds: ImmutableSet<String> = persistentSetOf(),
)

internal data class TrainingExerciseValidationTarget(
    val exampleId: String,
    val performedAt: LocalDateTime?,
)

internal data class TrainingExerciseValidationContext(
    val target: TrainingExerciseValidationTarget,
    val value: ExerciseValidationContext,
)

/** Retains the source snapshot so delayed flow emissions cannot highlight a different edit. */
internal data class TrainingExerciseValidation(
    val source: ExerciseState,
    val volumeIds: ImmutableSet<String>,
    val repetitionIds: ImmutableSet<String>,
)
