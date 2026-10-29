package com.grippo.exercise

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.grippo.core.state.trainings.ExerciseState as TrainingExerciseState

@Immutable
public data class ExerciseState(
    val exercise: TrainingExerciseState? = null,
    val history: ImmutableList<TrainingExerciseState> = persistentListOf(),
)
