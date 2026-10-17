package com.grippo.exercise

import androidx.compose.runtime.Immutable
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
        val history: ExerciseHistoryState = ExerciseHistoryState.Loading,
    ) : ExerciseState
}
