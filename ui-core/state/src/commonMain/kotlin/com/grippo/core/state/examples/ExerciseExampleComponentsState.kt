package com.grippo.core.state.examples

import androidx.compose.runtime.Composable
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
        val bodyMultiplier: Double
    ) : ExerciseExampleComponentsState() {
        @Composable
        public fun bodyPercent(): String {
            return "${(bodyMultiplier * 100)}%"
        }
    }

    @Immutable
    @Serializable
    public data class BodyAndExtra(
        val bodyRequired: Boolean,
        val bodyMultiplier: Double,
        val extraRequired: Boolean
    ) : ExerciseExampleComponentsState() {
        @Composable
        public fun bodyPercent(): String {
            return "${(bodyMultiplier * 100)}%"
        }
    }

    @Immutable
    @Serializable
    public data class BodyAndAssist(
        val bodyRequired: Boolean,
        val bodyMultiplier: Double,
        val assistRequired: Boolean
    ) : ExerciseExampleComponentsState() {
        @Composable
        public fun bodyPercent(): String {
            return "${(bodyMultiplier * 100)}%"
        }
    }
}
