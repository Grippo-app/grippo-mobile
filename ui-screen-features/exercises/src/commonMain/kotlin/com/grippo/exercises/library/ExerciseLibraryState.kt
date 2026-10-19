package com.grippo.exercises.library

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Immutable
@Serializable
internal data class ExerciseLibraryState(
    @Transient
    val mode: ExerciseLibraryMode = ExerciseLibraryMode.Default(null),
    @Transient
    val exerciseExamples: ImmutableList<ExerciseExampleState> = persistentListOf(),
    @Transient
    val pagination: PaginationState = PaginationState.Next(),
    val queries: Queries = Queries(),
    @Transient
    val hasSuggestionsResults: Boolean = true,
) {
    val canShowSuggestions: Boolean =
        mode is ExerciseLibraryMode.SimilarTo && hasSuggestionsResults
}

@Immutable
internal sealed interface ExerciseLibraryMode {

    @Immutable
    data class Default(
        val preselectedMuscleGroupId: String?,
    ) : ExerciseLibraryMode

    @Immutable
    data class SimilarTo(
        val targetExerciseExampleId: String,
        val target: ExerciseExampleValueState? = null,
    ) : ExerciseLibraryMode
}

@Immutable
@Serializable
internal data class Queries(
    val name: String = "",
    val filter: QueryFilter = QueryFilter.All,
    @Transient
    val muscleGroups: ImmutableList<MuscleGroupState<MuscleRepresentationState.Plain>> = persistentListOf(),
)

@Immutable
@Serializable
internal sealed interface QueryFilter {

    @Immutable
    @Serializable
    data object All : QueryFilter

    @Immutable
    @Serializable
    data object Suggestions : QueryFilter

    @Immutable
    @Serializable
    data class Group(val id: String) : QueryFilter
}

@Immutable
internal sealed class PaginationState {
    abstract val page: Int
    abstract val limit: Int
    abstract val isLoadingNextPage: Boolean
    abstract val isEndReached: Boolean

    @Immutable
    data class Next(
        override val page: Int = ExamplePage.Chunk.number,
        override val limit: Int = ExamplePage.CHUNK_LIMITS,
        override val isLoadingNextPage: Boolean = false,
        override val isEndReached: Boolean = false,
    ) : PaginationState()

    @Immutable
    data class Restartable(
        override val page: Int = ExamplePage.Chunk.number,
        override val limit: Int = ExamplePage.CHUNK_LIMITS,
        override val isLoadingNextPage: Boolean = false,
        override val isEndReached: Boolean = false,
    ) : PaginationState()
}
