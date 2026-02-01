package com.grippo.data.features.api.exercise.example.models

public sealed class ExerciseExampleComponents {

    public data class External(
        val required: Boolean
    ) : ExerciseExampleComponents()

    public data class BodyOnly(
        val required: Boolean,
        val multiplier: Double
    ) : ExerciseExampleComponents()

    public data class BodyAndExtra(
        val bodyRequired: Boolean,
        val bodyMultiplier: Double,
        val extraRequired: Boolean
    ) : ExerciseExampleComponents()

    public data class BodyAndAssist(
        val bodyRequired: Boolean,
        val bodyMultiplier: Double,
        val assistRequired: Boolean
    ) : ExerciseExampleComponents()
}
