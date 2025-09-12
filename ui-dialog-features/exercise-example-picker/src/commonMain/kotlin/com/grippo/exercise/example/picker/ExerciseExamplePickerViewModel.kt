package com.grippo.exercise.example.picker

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleQueries
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.dialog.api.DIALOG_EXIT_ANIMATION_DURATION
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.muscles.toState
import com.grippo.state.domain.example.toDomain
import com.grippo.state.domain.user.toDomain
import com.grippo.state.exercise.examples.ExerciseExampleDialogView
import com.grippo.state.filters.FilterValue
import com.grippo.state.sorting.SortingEnumState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

public class ExerciseExamplePickerViewModel(
    exerciseExampleFeature: ExerciseExampleFeature,
    muscleFeature: MuscleFeature,
    private val dialogController: DialogController,
) : BaseViewModel<ExerciseExamplePickerState, ExerciseExamplePickerDirection, ExerciseExamplePickerLoader>(
    ExerciseExamplePickerState()
), ExerciseExamplePickerContract {

    init {
        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map { s ->
                val weightType = s.filters.filterIsInstance<FilterValue.WeightType>().firstOrNull()
                    ?.value?.toDomain()
                val forceType = s.filters.filterIsInstance<FilterValue.ForceType>().firstOrNull()
                    ?.value?.toDomain()
                val category = s.filters.filterIsInstance<FilterValue.Category>().firstOrNull()
                    ?.value?.toDomain()
                val experience = s.filters.filterIsInstance<FilterValue.Experience>().firstOrNull()
                    ?.value?.toDomain()
                val sortBy = SortingEnumState.RecentlyUsed
                    .toDomain()

                SearchKey(
                    query = s.query.trim(),
                    weightType = weightType,
                    forceType = forceType,
                    category = category,
                    experience = experience,
                    muscleGroupId = s.selectedMuscleGroupId,
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
                    experience = key.experience,
                    muscleGroupId = key.muscleGroupId
                )
                val page = ExamplePage.First15

                exerciseExampleFeature.observeExerciseExamples(queries, key.sortBy, page)
            }
            .onEach(::provideExerciseExamples)
            .safeLaunch()

        safeLaunch {
            exerciseExampleFeature.getExerciseExamples()
        }
    }

    private fun provideMuscles(list: List<MuscleGroup>) {
        val suggestions = list.toState()
        update { it.copy(muscleGroups = suggestions) }
    }

    private fun provideExerciseExamples(value: List<ExerciseExample>) {
        val list = value.toState()
        update { it.copy(exerciseExamples = list) }
    }

    override fun onExerciseExampleDetailsClick(id: String) {
        val dialog = DialogConfig.ExerciseExample(
            id = id,
            view = ExerciseExampleDialogView.PICK,
            onResult = { example ->
                safeLaunch {
                    delay(DIALOG_EXIT_ANIMATION_DURATION)
                    navigateTo(ExerciseExamplePickerDirection.BackWithResult(example))
                }
            }
        )

        dialogController.show(dialog)
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
        update { it.copy(selectedMuscleGroupId = id) }
    }

    override fun onDismiss() {
        navigateTo(ExerciseExamplePickerDirection.Back)
    }
}