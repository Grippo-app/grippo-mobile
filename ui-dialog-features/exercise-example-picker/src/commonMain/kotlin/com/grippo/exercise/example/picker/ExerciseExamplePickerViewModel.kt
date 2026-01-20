package com.grippo.exercise.example.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.filters.FilterValueState
import com.grippo.data.features.api.exercise.example.UserExerciseExamplesUseCase
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleParams
import com.grippo.data.features.api.exercise.example.models.ExampleQueries
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.muscles.toState
import com.grippo.state.domain.example.toDomain
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

public class ExerciseExamplePickerViewModel(
    targetMuscleGroupId: String?,
    userExerciseExamplesUseCase: UserExerciseExamplesUseCase,
    muscleFeature: MuscleFeature,
    private val dialogController: DialogController,
) : BaseViewModel<ExerciseExamplePickerState, ExerciseExamplePickerDirection, ExerciseExamplePickerLoader>(
    ExerciseExamplePickerState(queries = Queries(selectedMuscleGroupId = targetMuscleGroupId))
), ExerciseExamplePickerContract {

    init {
        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map { current ->
                val manual = current.queries

                val manualFilters = manual.filters

                ExampleParams(
                    queries = ExampleQueries(
                        name = manual.name.trim(),
                        weightType = manualFilters
                            .filterIsInstance<FilterValueState.WeightType>()
                            .firstOrNull()
                            ?.value?.toDomain(),
                        forceType = manualFilters
                            .filterIsInstance<FilterValueState.ForceType>()
                            .firstOrNull()
                            ?.value?.toDomain(),
                        category = manualFilters
                            .filterIsInstance<FilterValueState.Category>()
                            .firstOrNull()
                            ?.value?.toDomain(),
                        muscleGroupId = manual.selectedMuscleGroupId
                    ),
                    page = ExamplePage(
                        limits = current.pagination.limit,
                        number = current.pagination.page
                    )
                )
            }
            .distinctUntilChanged()
            .flatMapLatest(userExerciseExamplesUseCase::execute)
            .onEach(::provideExerciseExamples)
            .safeLaunch()
    }

    private fun provideMuscles(list: List<MuscleGroup>) {
        val suggestions = list.toState()
        update { it.copy(queries = it.queries.copy(muscleGroups = suggestions)) }
    }

    private fun provideExerciseExamples(value: List<ExerciseExample>) {
        val incoming = value.toState()

        update { current ->
            val pagination = current.pagination
            val shouldReplace = pagination is PaginationState.Restartable ||
                    pagination.page == ExamplePage.Chunk.number
            val examples = if (shouldReplace) {
                incoming
            } else {
                (current.exerciseExamples + incoming)
                    .distinctBy { it.value.id }
                    .toPersistentList()
            }

            current.copy(
                exerciseExamples = examples,
                pagination = PaginationState.Next(
                    page = pagination.page,
                    limit = pagination.limit,
                    isLoadingNextPage = false,
                    isEndReached = value.size < pagination.limit
                )
            )
        }
    }

    override fun onQueryChange(value: String) {
        updateWithPaginationReset {
            it.copy(
                queries = it.queries.copy(name = value),
            )
        }
    }

    override fun onFiltersClick() {
        val dialog = DialogConfig.FilterPicker(
            initial = state.value.queries.filters,
            onResult = { value ->
                updateWithPaginationReset {
                    it.copy(
                        queries = it.queries.copy(filters = value.toPersistentList()),
                    )
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onMuscleGroupClick(id: String) {
        updateWithPaginationReset {
            val value = if (it.queries.selectedMuscleGroupId == id) null else id
            it.copy(
                queries = it.queries.copy(selectedMuscleGroupId = value),
            )
        }
    }

    override fun onLoadNextPage() {
        update { current ->
            val pagination = current.pagination
            when {
                pagination.isEndReached -> current
                pagination.isLoadingNextPage -> current
                else -> current.copy(
                    pagination = when (pagination) {
                        is PaginationState.Next -> pagination.copy(
                            page = pagination.page + 1,
                            isLoadingNextPage = true
                        )

                        is PaginationState.Restartable -> pagination.copy(
                            page = pagination.page + 1,
                            isLoadingNextPage = true
                        )
                    }
                )
            }
        }
    }

    override fun onExerciseExampleSelectClick(id: String) {
        val example = state.value.exerciseExamples.find { f -> f.value.id == id } ?: return
        navigateTo(ExerciseExamplePickerDirection.BackWithResult(example))
    }

    override fun onDismiss() {
        navigateTo(ExerciseExamplePickerDirection.Back)
    }

    private fun updateWithPaginationReset(
        transform: (ExerciseExamplePickerState) -> ExerciseExamplePickerState
    ) {
        update { current ->
            val state = transform(current)
            val pagination = state.pagination
            state.copy(
                pagination = PaginationState.Restartable(
                    page = ExamplePage.Chunk.number,
                    limit = pagination.limit,
                    isLoadingNextPage = false,
                    isEndReached = false
                )
            )
        }
    }
}
