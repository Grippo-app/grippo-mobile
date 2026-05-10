package com.grippo.exercise.example.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class ExerciseExamplePickerState(
    val mode: ExerciseExamplePickerMode,
    val exerciseExamples: ImmutableList<ExerciseExampleState> = persistentListOf(),
    val pagination: PaginationState = PaginationState.Next(),
    val queries: Queries = Queries(),
) {
    val canShowSuggestions: Boolean get() = mode is ExerciseExamplePickerMode.SimilarTo
}

@Immutable
public sealed interface ExerciseExamplePickerMode {

    @Immutable
    public data class Default(
        public val preselectedMuscleGroupId: String?,
    ) : ExerciseExamplePickerMode

    @Immutable
    public data class SimilarTo(
        public val targetExerciseExampleId: String,
    ) : ExerciseExamplePickerMode
}


@Immutable
public data class Queries(
    val name: String = "",
    val filter: QueryFilter = QueryFilter.All,
    val muscleGroups: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>> = persistentListOf(),
)

@Immutable
public sealed interface QueryFilter {

    @Immutable
    public data object All : QueryFilter

    @Immutable
    public data object Suggestions : QueryFilter

    @Immutable
    public data class Group(val id: String) : QueryFilter
}

@Immutable
public sealed class PaginationState {
    public abstract val page: Int
    public abstract val limit: Int
    public abstract val isLoadingNextPage: Boolean
    public abstract val isEndReached: Boolean

    @Immutable
    public data class Next(
        override val page: Int = ExamplePage.Chunk.number,
        override val limit: Int = ExamplePage.Chunk.limits,
        override val isLoadingNextPage: Boolean = false,
        override val isEndReached: Boolean = false,
    ) : PaginationState()

    @Immutable
    public data class Restartable(
        override val page: Int = ExamplePage.Chunk.number,
        override val limit: Int = ExamplePage.Chunk.limits,
        override val isLoadingNextPage: Boolean = false,
        override val isEndReached: Boolean = false,
    ) : PaginationState()
}
