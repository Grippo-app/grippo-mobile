package com.grippo.core.state.examples

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class ExerciseExampleComponentsState {

    @Immutable
    @Serializable
    public data class External(
        val required: Boolean
    ) : ExerciseExampleComponentsState()

    @Immutable
    @Serializable
    public data class BodyOnly(
        val required: Boolean,
        val bodyMultiplier: Float
    ) : ExerciseExampleComponentsState()

    @Immutable
    @Serializable
    public data class BodyAndExtra(
        val bodyRequired: Boolean,
        val bodyMultiplier: Float,
        val extraRequired: Boolean
    ) : ExerciseExampleComponentsState()

    @Immutable
    @Serializable
    public data class BodyAndAssist(
        val bodyRequired: Boolean,
        val bodyMultiplier: Float,
        val assistRequired: Boolean
    ) : ExerciseExampleComponentsState()
}
