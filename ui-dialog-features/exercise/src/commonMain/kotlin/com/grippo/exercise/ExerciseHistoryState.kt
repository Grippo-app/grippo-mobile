package com.grippo.exercise

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import com.grippo.core.state.trainings.ExerciseState as TrainingExerciseState

@Immutable
public sealed interface ExerciseHistoryState {
    @Immutable
    public data object Loading : ExerciseHistoryState

    @Immutable
    public data class Loaded(val records: ImmutableList<TrainingExerciseState>) : ExerciseHistoryState
}
