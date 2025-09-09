package com.grippo.exercise.examples.list

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExampleQueries
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

internal class ExerciseExampleListViewModel(
    exerciseExampleFeature: ExerciseExampleFeature,
    private val dialogController: DialogController,
) : BaseViewModel<ExerciseExampleListState, ExerciseExampleListDirection, ExerciseExampleListLoader>(
    ExerciseExampleListState()
), ExerciseExampleListContract {

    init {
        state.map { Triple(it.query, it.filters, ExampleSortingEnumState.MostlyUsed) }
            .distinctUntilChanged()
            .flatMapLatest { (query, filters, sortBy) ->
                val name = query
                val weightType = filters.filterIsInstance<FilterValue.WeightType>().firstOrNull()
                    ?.value?.toDomain()
                val forceType = filters.filterIsInstance<FilterValue.ForceType>().firstOrNull()
                    ?.value?.toDomain()
                val category = filters.filterIsInstance<FilterValue.Category>().firstOrNull()
                    ?.value?.toDomain()
                val experience = filters.filterIsInstance<FilterValue.Experience>().firstOrNull()
                    ?.value?.toDomain()

                val queries = ExampleQueries(
                    name = name,
                    weightType = weightType,
                    forceType = forceType,
                    category = category,
                    experience = experience,
                )

                val sorting = sortBy.toDomain()

                exerciseExampleFeature.observeExerciseExamples(queries, sorting = sorting)
            }
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
