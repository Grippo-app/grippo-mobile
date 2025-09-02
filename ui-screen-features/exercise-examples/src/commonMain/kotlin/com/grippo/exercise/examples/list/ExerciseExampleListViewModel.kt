package com.grippo.exercise.examples.list

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.state.exercise.examples.ExerciseExampleDialogView
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.onEach

internal class ExerciseExampleListViewModel(
    exerciseExampleFeature: ExerciseExampleFeature,
    private val dialogController: DialogController,
) : BaseViewModel<ExerciseExampleListState, ExerciseExampleListDirection, ExerciseExampleListLoader>(
    ExerciseExampleListState()
), ExerciseExampleListContract {

    init {
        exerciseExampleFeature
            .observeExerciseExamples()
            .onEach(::provideExerciseExamples)
            .safeLaunch()
    }

    private fun provideExerciseExamples(value: List<ExerciseExample>) {
        val list = value.toState()
        update { it.copy(exerciseExamples = list) }
    }

    override fun onBack() {
        navigateTo(ExerciseExampleListDirection.Back)
    }

    override fun onExerciseExampleClick(id: String) {
        val dialog = DialogConfig.ExerciseExample(
            id = id,
            view = ExerciseExampleDialogView.READ
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
}
