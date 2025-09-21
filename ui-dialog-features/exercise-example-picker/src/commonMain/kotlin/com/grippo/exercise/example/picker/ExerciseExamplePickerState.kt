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
    val query: String = "",
    val muscleGroups: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>> = persistentListOf(),
    val selectedMuscleGroupId: String?,
    val filters: ImmutableList<FilterValue> = ExerciseExampleState.filters
)

internal class SearchKey(
    val query: String,
    val weightType: WeightTypeEnum?,
    val forceType: ForceTypeEnum?,
    val category: CategoryEnum?,
    val muscleGroupId: String?,
    val sortBy: ExampleSortingEnum
)