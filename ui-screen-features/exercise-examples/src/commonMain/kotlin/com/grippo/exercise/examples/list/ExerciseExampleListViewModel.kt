package com.grippo.exercise.examples.list

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import kotlinx.collections.immutable.persistentListOf

internal class ExerciseExampleListViewModel(
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val dialogController: DialogController,
) : BaseViewModel<ExerciseExampleListState, ExerciseExampleListDirection, ExerciseExampleListLoader>(
    ExerciseExampleListState()
), ExerciseExampleListContract {

    override fun onBack() {
        navigateTo(ExerciseExampleListDirection.Back)
    }

    override fun onExerciseExampleClick(id: String) {
    }

    override fun onQueryChange(value: String) {
        update { it.copy(query = value) }
    }

    override fun onFiltersClick() {
        val dialog = DialogConfig.FilterPicker(
            initial = persistentListOf(),
            onResult = { value ->
            }
        )

        dialogController.show(dialog)
    }
}
