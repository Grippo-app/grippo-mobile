package com.grippo.drart.training

import androidx.compose.runtime.Immutable
import com.grippo.state.trainings.ExerciseState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class DraftTrainingState(
    val exercises: ImmutableList<ExerciseState> = persistentListOf(),
)
