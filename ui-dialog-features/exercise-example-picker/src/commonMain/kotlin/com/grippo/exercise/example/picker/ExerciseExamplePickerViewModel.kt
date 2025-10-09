package com.grippo.exercise.example.picker

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.UserExerciseExamplesUseCase
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleQueries
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.suggestion.SuggestionFeature
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.muscles.toState
import com.grippo.state.domain.example.toDomain
import com.grippo.state.filters.FilterValue
import com.grippo.state.sorting.SortingEnumState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

public class ExerciseExamplePickerViewModel(
    targetMuscleGroupId: String?,
    userExerciseExamplesUseCase: UserExerciseExamplesUseCase,
    muscleFeature: MuscleFeature,
    suggestionFeature: SuggestionFeature,
    private val dialogController: DialogController,
) : BaseViewModel<ExerciseExamplePickerState, ExerciseExamplePickerDirection, ExerciseExamplePickerLoader>(
    ExerciseExamplePickerState(
        selectedMuscleGroupId = targetMuscleGroupId
    )
), ExerciseExamplePickerContract {

    init {
        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map { s ->
                val muscleGroupId = s.selectedMuscleGroupId
                val sortBy = SortingEnumState.RecentlyUsed.toDomain()

                val weightType = s.filters.filterIsInstance<FilterValue.WeightType>().firstOrNull()
                    ?.value?.toDomain()
                val forceType = s.filters.filterIsInstance<FilterValue.ForceType>().firstOrNull()
                    ?.value?.toDomain()
                val category = s.filters.filterIsInstance<FilterValue.Category>().firstOrNull()
                    ?.value?.toDomain()

                SearchKey(
                    query = s.query.trim(),
                    weightType = weightType,
                    forceType = forceType,
                    category = category,
                    muscleGroupId = muscleGroupId,
                    sortBy = sortBy
                )
            }
            .distinctUntilChanged()
            .flatMapLatest { key ->
                val queries = ExampleQueries(
                    name = key.query,
                    weightType = key.weightType,
                    forceType = key.forceType,
                    category = key.category,
                    muscleGroupId = key.muscleGroupId
                )

                userExerciseExamplesUseCase.execute(
                    queries = queries,
                    sorting = key.sortBy,
                    page = ExamplePage.First15
                )
            }
            .onEach(::provideExerciseExamples)
            .safeLaunch()
    }

    private fun provideMuscles(list: List<MuscleGroup>) {
        val suggestions = list.toState()
        update { it.copy(muscleGroups = suggestions) }
    }

    private fun provideExerciseExamples(value: List<ExerciseExample>) {
        val list = value.toState()
        update { it.copy(exerciseExamples = list) }
    }

    override fun onQueryChange(value: String) {
        update { it.copy(query = value) }
    }

    override fun onFiltersClick() {
        val dialog = DialogConfig.FilterPicker(
            initial = state.value.filters,
            onResult = { value -> update { it.copy(filters = value.toPersistentList()) } }
        )

        dialogController.show(dialog)
    }

    override fun onExerciseExampleSelectClick(id: String) {
        val example = state.value.exerciseExamples.find { f -> f.value.id == id } ?: return
        navigateTo(ExerciseExamplePickerDirection.BackWithResult(example))
    }

    override fun onMuscleGroupClick(id: String) {
        update {
            val value = if (it.selectedMuscleGroupId == id) {
                null
            } else {
                id
            }

            it.copy(selectedMuscleGroupId = value)
        }
    }

    override fun onDismiss() {
        navigateTo(ExerciseExamplePickerDirection.Back)
    }
}
