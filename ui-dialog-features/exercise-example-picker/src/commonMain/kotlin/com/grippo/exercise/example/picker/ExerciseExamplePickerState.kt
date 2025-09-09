package com.grippo.exercise.example.picker

import androidx.compose.runtime.Immutable
import com.grippo.state.exercise.examples.ExampleSortingEnumState
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.filters.FilterValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class ExerciseExamplePickerState(
    val exerciseExamples: ImmutableList<ExerciseExampleState> = persistentListOf(),
    val query: String = "",
    val sortingSuggestions: ImmutableList<ExampleSortingEnumState> = ExampleSortingEnumState.entries.toPersistentList(),
    val sortBy: ExampleSortingEnumState = ExampleSortingEnumState.MostlyUsed,
    val filters: ImmutableList<FilterValue> = ExerciseExampleState.filters
)