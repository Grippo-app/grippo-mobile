package com.grippo.exercise.example.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.filters.FilterValue
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.UserExerciseExamplesUseCase
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleParams
import com.grippo.data.features.api.exercise.example.models.ExampleQueries
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.suggestion.AiSuggestionFeature
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.muscles.toState
import com.grippo.state.domain.example.toDomain
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

public class ExerciseExamplePickerViewModel(
    targetMuscleGroupId: String?,
    userExerciseExamplesUseCase: UserExerciseExamplesUseCase,
    muscleFeature: MuscleFeature,
    private val exampleFeature: ExerciseExampleFeature,
    private val aiSuggestionFeature: AiSuggestionFeature,
    private val dialogController: DialogController,
) : BaseViewModel<ExerciseExamplePickerState, ExerciseExamplePickerDirection, ExerciseExamplePickerLoader>(
    ExerciseExamplePickerState(manual = ManualQueries(selectedMuscleGroupId = targetMuscleGroupId))
), ExerciseExamplePickerContract {

    init {
        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map { current ->
                val manual = current.manual
                val suggestion = current.suggestion

                val manualFilters = manual.filters
                val isManual = suggestion == null

                ExampleParams(
                    queries = ExampleQueries(
                        name = suggestion?.name ?: manual.name.trim(),
                        weightType = if (isManual) manualFilters
                            .filterIsInstance<FilterValue.WeightType>()
                            .firstOrNull()
                            ?.value?.toDomain() else null,
                        forceType = if (isManual) manualFilters
                            .filterIsInstance<FilterValue.ForceType>()
                            .firstOrNull()
                            ?.value?.toDomain() else null,
                        category = if (isManual) manualFilters
                            .filterIsInstance<FilterValue.Category>()
                            .firstOrNull()
                            ?.value?.toDomain() else null,
                        muscleGroupId = if (isManual) manual.selectedMuscleGroupId else null
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
        update { it.copy(manual = it.manual.copy(muscleGroups = suggestions)) }
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
                manual = it.manual.copy(name = value),
                suggestion = null
            )
        }
    }

    override fun onFiltersClick() {
        val dialog = DialogConfig.FilterPicker(
            initial = state.value.manual.filters,
            onResult = { value ->
                updateWithPaginationReset {
                    it.copy(
                        manual = it.manual.copy(filters = value.toPersistentList()),
                        suggestion = null
                    )
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onClearSuggestion() {
        updateWithPaginationReset { it.copy(suggestion = null) }
    }

    override fun onMuscleGroupClick(id: String) {
        updateWithPaginationReset {
            val value = if (it.manual.selectedMuscleGroupId == id) null else id
            it.copy(
                manual = it.manual.copy(selectedMuscleGroupId = value),
                suggestion = null
            )
        }
    }

    override fun onSuggestClick() {
        safeLaunch(loader = ExerciseExamplePickerLoader.SuggestExample) {
            val result = aiSuggestionFeature
                .predictExerciseExample()
                .getOrThrow() ?: return@safeLaunch

            val name = exampleFeature
                .observeExerciseExample(result.id)
                .firstOrNull()?.value?.name ?: return@safeLaunch

            updateWithPaginationReset {
                it.copy(
                    suggestion = AiSuggestionQueries(
                        id = result.id,
                        name = name,
                        reason = result.reason
                    )
                )
            }
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
