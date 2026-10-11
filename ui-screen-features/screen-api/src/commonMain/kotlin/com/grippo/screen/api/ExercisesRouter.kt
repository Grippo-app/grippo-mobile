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
        val mode: Mode = Mode.Default(),
        val resultKey: ResultKey<Result<ExerciseExampleState>>,
    ) : ExercisesRouter()

    @Serializable
    public sealed interface Mode {
        @Serializable
        public data class Default(val preselectedMuscleGroupId: String? = null) : Mode

        @Serializable
        public data class SimilarTo(val targetExerciseExampleId: String) : Mode
    }
}
