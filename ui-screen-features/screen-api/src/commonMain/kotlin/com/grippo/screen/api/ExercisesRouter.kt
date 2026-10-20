package com.grippo.screen.api

import com.grippo.core.foundation.models.BaseRouter
import com.grippo.core.foundation.models.Result
import com.grippo.core.foundation.models.ResultKey
import com.grippo.core.state.examples.ExerciseExampleState
import kotlinx.serialization.Serializable

@Serializable
public sealed class ExercisesRouter : BaseRouter {
    @Serializable
    public data class Library(
        val mode: Mode,
    ) : ExercisesRouter()

    @Serializable
    public sealed interface Mode {
        /** The caller waits for one picked example on [resultKey]. */
        @Serializable
        public sealed interface Pick : Mode {
            public val resultKey: ResultKey<Result<ExerciseExampleState>>
        }

        @Serializable
        public data class Default(
            val preselectedMuscleGroupId: String? = null,
            override val resultKey: ResultKey<Result<ExerciseExampleState>>,
        ) : Pick

        @Serializable
        public data class SimilarTo(
            val targetExerciseExampleId: String,
            override val resultKey: ResultKey<Result<ExerciseExampleState>>,
        ) : Pick

        /** Embedded catalog (the Library tab): nothing is returned. */
        @Serializable
        public data object Browse : Mode
    }
}
