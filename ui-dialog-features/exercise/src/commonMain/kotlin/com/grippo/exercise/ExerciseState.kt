package com.grippo.exercise

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.grippo.core.state.trainings.ExerciseState as TrainingExerciseState

@Immutable
public sealed interface ExerciseState {
    @Immutable
    public data object Loading : ExerciseState

    @Immutable
    public data object Unavailable : ExerciseState

    @Immutable
    public data class Available(
        val exercise: TrainingExerciseState,
        val history: ImmutableList<TrainingExerciseState> = persistentListOf(),
    ) : ExerciseState
}
