package com.grippo.exercise.example.picker

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.exercise.example.UserExerciseExamplesUseCase
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleParams
import com.grippo.data.features.api.exercise.example.models.ExampleScope
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.domain.state.exercise.example.toState
import com.grippo.domain.state.muscles.toState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@OptIn(FlowPreview::class)
public class ExerciseExamplePickerViewModel(
    mode: ExerciseExamplePickerMode,
    userExerciseExamplesUseCase: UserExerciseExamplesUseCase,
    muscleFeature: MuscleFeature,
) : BaseViewModel<ExerciseExamplePickerState, ExerciseExamplePickerDirection, ExerciseExamplePickerLoader>(
    ExerciseExamplePickerState(
        mode = mode,
        queries = Queries(filter = initialFilter(mode)),
    )
), ExerciseExamplePickerContract {

    init {
        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map(::buildParams)
            .distinctUntilChanged()
            .flatMapLatest { params ->
                userExerciseExamplesUseCase.execute(params).map { results -> params to results }
            }
            .onEach { (params, results) -> provideExerciseExamples(params, results) }
            .safeLaunch()
    }

    private fun buildParams(current: ExerciseExamplePickerState): ExampleParams {
        val scope: ExampleScope = when (val filter = current.queries.filter) {
            QueryFilter.Suggestions -> when (val m = current.mode) {
                is ExerciseExamplePickerMode.SimilarTo -> ExampleScope.SimilarTo(m.targetExerciseExampleId)
                is ExerciseExamplePickerMode.Default -> ExampleScope.All()
            }

            is QueryFilter.Group -> ExampleScope.All(muscleGroupId = filter.id)
            QueryFilter.All -> ExampleScope.All()
        }

        return ExampleParams(
            name = current.queries.name.trim(),
            scope = scope,
            page = ExamplePage(
                limits = current.pagination.limit,
                number = current.pagination.page,
            ),
        )
    }

    private fun provideMuscles(list: List<MuscleGroup>) {
        val suggestions = list.toState()
        update { current ->
            val filter = current.queries.filter
            val groupVanished =
                filter is QueryFilter.Group && suggestions.none { it.id == filter.id }
            val nextFilter = if (groupVanished) QueryFilter.All else filter
            val nextPagination = if (groupVanished) {
                PaginationState.Restartable(
                    page = ExamplePage.Chunk.number,
                    limit = current.pagination.limit,
                    isLoadingNextPage = false,
                    isEndReached = false,
                )
            } else {
                current.pagination
            }
            current.copy(
                queries = current.queries.copy(
                    muscleGroups = suggestions,
                    filter = nextFilter,
                ),
                pagination = nextPagination,
            )
        }
    }

    private fun provideExerciseExamples(
        emittedParams: ExampleParams,
        value: List<ExerciseExample>
    ) {
        update { current ->
            if (buildParams(current) != emittedParams) return@update current

            val pagination = current.pagination
            val isFirstPage = pagination is PaginationState.Restartable ||
                    pagination.page == ExamplePage.Chunk.number
            val isSuggestionsActive = current.queries.filter is QueryFilter.Suggestions &&
                    current.mode is ExerciseExamplePickerMode.SimilarTo

            // Auto-switch: empty Suggestions on first page → fall back to All, hide chip
            if (isSuggestionsActive && isFirstPage && value.isEmpty()) {
                return@update current.copy(
                    queries = current.queries.copy(filter = QueryFilter.All),
                    exerciseExamples = persistentListOf(),
                    hasSuggestionsResults = false,
                    pagination = PaginationState.Restartable(),
                )
            }

            val incoming = value.toState()
            val items = if (isFirstPage) {
                incoming.toPersistentList()
            } else {
                (current.exerciseExamples + incoming).distinctBy { it.value.id }.toPersistentList()
            }

            val nextHasSuggestionsResults =
                if (isSuggestionsActive && isFirstPage) value.isNotEmpty()
                else current.hasSuggestionsResults

            current.copy(
                exerciseExamples = items,
                hasSuggestionsResults = nextHasSuggestionsResults,
                pagination = PaginationState.Next(
                    page = pagination.page,
                    limit = pagination.limit,
                    isLoadingNextPage = false,
                    isEndReached = value.size < pagination.limit,
                ),
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

    override fun onSuggestionsClick() {
        if (state.value.mode !is ExerciseExamplePickerMode.SimilarTo) return
        updateWithPaginationReset {
            val nextFilter = if (it.queries.filter is QueryFilter.Suggestions) {
                QueryFilter.All
            } else {
                QueryFilter.Suggestions
            }
            it.copy(queries = it.queries.copy(filter = nextFilter))
        }
    }

    override fun onMuscleGroupClick(id: String) {
        updateWithPaginationReset {
            val nextFilter =
                if (it.queries.filter is QueryFilter.Group && it.queries.filter.id == id) {
                    QueryFilter.All
                } else {
                    QueryFilter.Group(id)
                }
            it.copy(queries = it.queries.copy(filter = nextFilter))
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
        val example = state.value.exerciseExamples.find { it.value.id == id } ?: return
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

    private companion object {
        private fun initialFilter(mode: ExerciseExamplePickerMode): QueryFilter = when (mode) {
            is ExerciseExamplePickerMode.SimilarTo -> QueryFilter.Suggestions
            is ExerciseExamplePickerMode.Default -> mode.preselectedMuscleGroupId
                ?.let(QueryFilter::Group)
                ?: QueryFilter.All
        }
    }
}
