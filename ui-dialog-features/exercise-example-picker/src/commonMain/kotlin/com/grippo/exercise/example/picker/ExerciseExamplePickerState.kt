package com.grippo.exercise.example.picker

import androidx.compose.runtime.Immutable
import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.filters.FilterValue
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class ExerciseExamplePickerState(
    val exerciseExamples: ImmutableList<ExerciseExampleState> = persistentListOf(),
    val muscleGroups: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>> = persistentListOf(),
    val query: String = "",
    val selectedMuscleGroupId: String?,
    val filters: ImmutableList<FilterValue> = ExerciseExampleState.filters,
    val suggestion: ExerciseExampleSuggestion? = null
)

@Immutable
public data class ExerciseExampleSuggestion(
    val id: String,
    val name: String,
    val reason: String,
    val warning: String?,
)

internal data class SearchKey(
    val query: String,
    val weightType: WeightTypeEnum?,
    val forceType: ForceTypeEnum?,
    val category: CategoryEnum?,
    val muscleGroupId: String?,
    val sortBy: ExampleSortingEnum
)
