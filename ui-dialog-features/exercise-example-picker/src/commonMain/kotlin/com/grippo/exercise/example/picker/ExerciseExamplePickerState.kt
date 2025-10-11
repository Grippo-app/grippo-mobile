package com.grippo.exercise.example.picker

import androidx.compose.runtime.Immutable
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.filters.FilterValue
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class ExerciseExamplePickerState(
    val exerciseExamples: ImmutableList<ExerciseExampleState> = persistentListOf(),

    // Keep both sub-states in one state holder
    val manual: ManualQueries = ManualQueries(),
    val suggestion: AiSuggestionQueries? = null,
)

@Immutable
public data class ManualQueries(
    val name: String = "",
    val selectedMuscleGroupId: String? = null,
    val filters: ImmutableList<FilterValue> = ExerciseExampleState.filters,
    val muscleGroups: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>> = persistentListOf(),
)

@Immutable
public data class AiSuggestionQueries(
    val id: String,
    val name: String,
    val reason: String,
)
