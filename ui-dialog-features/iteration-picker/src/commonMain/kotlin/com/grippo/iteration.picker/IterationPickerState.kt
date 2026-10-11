package com.grippo.iteration.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.IterationState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class IterationPickerState(
    val example: ExerciseExampleState,
    val number: Int,
    val value: IterationState,
    val iterationSuggestions: ImmutableList<IterationState>,
    val focus: IterationFocusState,
    val repetitionSuggestions: ImmutableList<Int> = persistentListOf(),
)
