package com.grippo.exercises

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.ExerciseExampleState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class ExercisesState(
    val items: ImmutableList<ExerciseExampleState> = persistentListOf(),
)
