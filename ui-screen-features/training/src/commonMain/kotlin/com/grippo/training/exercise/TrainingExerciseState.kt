package com.grippo.training.exercise

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.IterationState
import com.grippo.data.features.api.training.models.ExerciseValidationContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Immutable
@Serializable
internal data class TrainingExerciseState(
    val exercise: ExerciseState,
    @Transient
    val exerciseExample: ExerciseExampleState? = null,
    @Transient
    val previousIterations: ImmutableList<IterationState> = persistentListOf(),
    @Transient
    val volumeArtifactIds: ImmutableSet<String> = persistentSetOf(),
    @Transient
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

// Carries its source so a late emission for an older edit is dropped, not shown on the current one.
internal data class TrainingExerciseValidation(
    val source: ExerciseState,
    val volumeIds: ImmutableSet<String> = persistentSetOf(),
    val repetitionIds: ImmutableSet<String> = persistentSetOf(),
    val previousIterations: ImmutableList<IterationState> = persistentListOf(),
)
