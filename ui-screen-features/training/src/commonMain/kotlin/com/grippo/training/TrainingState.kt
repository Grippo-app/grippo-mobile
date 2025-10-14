package com.grippo.training

import androidx.compose.runtime.Immutable
import com.grippo.core.state.trainings.ExerciseState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class TrainingState(
    val exercises: ImmutableList<ExerciseState> = persistentListOf()
)