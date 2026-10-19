package com.grippo.exercises.library

import com.grippo.analytics.exercises.ExercisesAnalytics
import com.grippo.analytics.exercises.models.ExercisesEvent
import com.grippo.analytics.exercises.models.ExercisesScreen
import com.grippo.analytics.exercises.models.LibraryFilter
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class ExerciseLibraryViewModel(
    mode: ExerciseLibraryMode,
    private val analytics: ExercisesAnalytics,
    private val userExerciseExamplesUseCase: UserExerciseExamplesUseCase,
    exerciseExampleFeature: ExerciseExampleFeature,
    muscleFeature: MuscleFeature,
    initialState: ExerciseLibraryState?,
) : BaseViewModel<ExerciseLibraryState, ExerciseLibraryDirection, ExerciseLibraryLoader>(
    initialState?.copy(mode = mode) ?: ExerciseLibraryState(
        mode = mode,
        queries = Queries(
            filter = when (mode) {
                is ExerciseLibraryMode.SimilarTo -> QueryFilter.Suggestions
                is ExerciseLibraryMode.Default -> mode.preselectedMuscleGroupId
                    ?.let(QueryFilter::Group)
                    ?: QueryFilter.All
            },
        ),
    )
), ExerciseLibraryContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(ExercisesScreen.Library, reason)
    }

    init {
        if (mode is ExerciseLibraryMode.SimilarTo) {
            exerciseExampleFeature.observeExerciseExample(mode.targetExerciseExampleId)
                .onEach(::provideTarget)
                .safeLaunch()
        }

        muscleFeature.observeMuscles()
            .onEach(::provideMuscles)
            .safeLaunch()

        state
            .map(::buildParams)
            .distinctUntilChanged()
            .flatMapLatest(::observeExerciseExamples)
            .onEach(::provideExerciseExamples)
            .safeLaunch()
    }

    private fun observeExerciseExamples(params: ExampleParams): Flow<Pair<ExampleParams, List<ExerciseExample>>> {
        return userExerciseExamplesUseCase
            .execute(params)
            .map { results -> params to results }
    }

    private fun buildParams(current: ExerciseLibraryState): ExampleParams {
        val scope: ExampleScope = when (val filter = current.queries.filter) {
            QueryFilter.Suggestions -> when (val m = current.mode) {
                is ExerciseLibraryMode.SimilarTo -> ExampleScope.SimilarTo(m.targetExerciseExampleId)
                is ExerciseLibraryMode.Default -> ExampleScope.All()
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

    private fun provideTarget(value: ExerciseExample?) {
        val target = value?.toState()?.value

        update { current ->
            if (current.mode !is ExerciseLibraryMode.SimilarTo) return@update current

            current.copy(mode = current.mode.copy(target = target))
        }
    }

    private fun provideMuscles(list: List<MuscleGroup>) {
        val suggestions = list.toState()

        update { current ->
            val filter = current.queries.filter
            val groupVanished =
                filter is QueryFilter.Group && suggestions.isNotEmpty() && suggestions.none { it.id == filter.id }
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

    private fun provideExerciseExamples(value: Pair<ExampleParams, List<ExerciseExample>>) {
        val (emittedParams, results) = value

        val incoming = results.toState()

        update { current ->
            if (buildParams(current) != emittedParams) return@update current

            val pagination = current.pagination
            val isFirstPage = pagination is PaginationState.Restartable ||
                    pagination.page == ExamplePage.Chunk.number
            val isSuggestionsActive = current.queries.filter is QueryFilter.Suggestions &&
                    current.mode is ExerciseLibraryMode.SimilarTo

            // Auto-switch: empty Suggestions on first page → fall back to All, hide chip
            if (isSuggestionsActive && isFirstPage && results.isEmpty()) {
                return@update current.copy(
                    queries = current.queries.copy(filter = QueryFilter.All),
                    exerciseExamples = persistentListOf(),
                    hasSuggestionsResults = false,
                    pagination = PaginationState.Restartable(),
                )
            }

            val items = if (isFirstPage) {
                incoming.toPersistentList()
            } else {
                (current.exerciseExamples + incoming).distinctBy { it.value.id }.toPersistentList()
            }

            val nextHasSuggestionsResults =
                if (isSuggestionsActive && isFirstPage) results.isNotEmpty()
                else current.hasSuggestionsResults

            current.copy(
                exerciseExamples = items,
                hasSuggestionsResults = nextHasSuggestionsResults,
                pagination = PaginationState.Next(
                    page = pagination.page,
                    limit = pagination.limit,
                    isLoadingNextPage = false,
                    isEndReached = results.size < pagination.limit,
                ),
            )
        }
    }

    override fun onQueryChange(value: String) {
        updateWithPaginationReset { it.copy(queries = it.queries.copy(name = value)) }
    }

    override fun onSuggestionsClick() {
        if (state.value.mode !is ExerciseLibraryMode.SimilarTo) return
        analytics.trackEvent(ExercisesEvent.FilterClicked(LibraryFilter.Suggestions))
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
        analytics.trackEvent(ExercisesEvent.FilterClicked(LibraryFilter.MuscleGroup))
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
        val pagination = state.value.pagination
        if (pagination.isEndReached || pagination.isLoadingNextPage) return
        analytics.trackEvent(ExercisesEvent.LoadNextPage)
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
        analytics.trackEvent(ExercisesEvent.ExerciseSelected)
        navigateTo(ExerciseLibraryDirection.BackWithResult(example))
    }

    override fun onBack() {
        analytics.trackEvent(ExercisesEvent.DismissRequested)
        navigateTo(ExerciseLibraryDirection.Back)
    }

    private fun updateWithPaginationReset(
        transform: (ExerciseLibraryState) -> ExerciseLibraryState
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
