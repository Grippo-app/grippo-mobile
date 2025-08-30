package com.grippo.exercise.examples.list

import androidx.compose.runtime.Immutable
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.exercise.examples.ExerciseExampleValueState
import com.grippo.state.filters.FilterValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ExerciseExampleListState(
    val exerciseExamples: ImmutableList<ExerciseExampleState> = persistentListOf(),
    val query: String = "",
    val filters: ImmutableList<FilterValue> = ExerciseExampleValueState.filters
)
