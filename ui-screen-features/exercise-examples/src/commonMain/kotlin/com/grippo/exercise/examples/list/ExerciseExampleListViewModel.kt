package com.grippo.exercise.examples.list

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature

internal class ExerciseExampleListViewModel(
    private val exerciseExampleFeature: ExerciseExampleFeature
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
    }
}
