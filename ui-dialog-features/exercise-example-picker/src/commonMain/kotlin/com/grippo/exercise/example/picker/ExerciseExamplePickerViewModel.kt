package com.grippo.exercise.example.picker

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.state.domain.example.toDomain
import com.grippo.state.domain.user.toDomain
import com.grippo.state.exercise.examples.ExampleSortingEnumState
import com.grippo.state.exercise.examples.ExerciseExampleDialogView
import com.grippo.state.filters.FilterValue
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

public class ExerciseExamplePickerViewModel(
    exerciseExampleFeature: ExerciseExampleFeature,
    private val dialogController: DialogController,
) : BaseViewModel<ExerciseExamplePickerState, ExerciseExamplePickerDirection, ExerciseExamplePickerLoader>(
    ExerciseExamplePickerState()
), ExerciseExamplePickerContract {

    init {
        state.map { it.query to it.filters }
            .distinctUntilChanged()
            .flatMapLatest {
                val name = it.first
                val weightType = it.second.filterIsInstance<FilterValue.WeightType>().firstOrNull()
                    ?.value?.toDomain()
                val forceType = it.second.filterIsInstance<FilterValue.ForceType>().firstOrNull()
                    ?.value?.toDomain()
                val category = it.second.filterIsInstance<FilterValue.Category>().firstOrNull()
                    ?.value?.toDomain()
                val experience = it.second.filterIsInstance<FilterValue.Experience>().firstOrNull()
                    ?.value?.toDomain()

                exerciseExampleFeature.observeExerciseExamples(
                    name = name,
                    weightType = weightType,
                    forceType = forceType,
                    category = category,
                    experience = experience,
                )
            }
            .onEach(::provideExerciseExamples)
            .safeLaunch()
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
                navigateTo(ExerciseExamplePickerDirection.BackWithResult(example))
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

    override fun onSortByClick(value: ExampleSortingEnumState) {
        update { it.copy(sortBy = value) }
    }

    override fun onDismiss() {
        navigateTo(ExerciseExamplePickerDirection.Back)
    }
}