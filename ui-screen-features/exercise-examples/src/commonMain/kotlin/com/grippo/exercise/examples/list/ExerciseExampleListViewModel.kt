package com.grippo.exercise.examples.list

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.state.exercise.examples.CategoryEnumState
import com.grippo.state.exercise.examples.ForceTypeEnumState
import com.grippo.state.exercise.examples.WeightTypeEnumState
import com.grippo.state.filters.FilterValue
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
        val categories = FilterValue.Category(value = CategoryEnumState.COMPOUND)
        val weightType = FilterValue.WeightType(value = WeightTypeEnumState.FREE)
        val forceType = FilterValue.ForceType(value = ForceTypeEnumState.PUSH)

        val dialog = DialogConfig.FilterPicker(
            initial = persistentListOf(
                categories,
                weightType,
                forceType
            ),
            onResult = { value ->
            }
        )

        dialogController.show(dialog)
    }
}
