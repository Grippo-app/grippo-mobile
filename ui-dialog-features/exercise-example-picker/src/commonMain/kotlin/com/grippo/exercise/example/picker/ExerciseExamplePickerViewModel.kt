package com.grippo.exercise.example.picker

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.state.datetime.PeriodState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

public class ExerciseExamplePickerViewModel(
    exerciseExampleFeature: ExerciseExampleFeature,
    private val dialogController: DialogController,
) : BaseViewModel<ExerciseExamplePickerState, ExerciseExamplePickerDirection, ExerciseExamplePickerLoader>(
    ExerciseExamplePickerState()
), ExerciseExamplePickerContract {

    init {
        exerciseExampleFeature
            .observeExerciseExamples()
            .onEach(::provideExerciseExamples)
            .safeLaunch()
    }

    private fun provideExerciseExamples(value: List<ExerciseExample>) {
        val list = value.toState()
        update { it.copy(exerciseExamples = list.toPersistentList()) }
    }

    override fun onExerciseExampleDetailsClick(id: String) {
        val dialog = DialogConfig.ExerciseExample(
            id = id,
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

    override fun onExerciseExampleSelectClick(value: PeriodState) {
    }

    override fun onDismiss() {
        navigateTo(ExerciseExamplePickerDirection.Back)
    }
}