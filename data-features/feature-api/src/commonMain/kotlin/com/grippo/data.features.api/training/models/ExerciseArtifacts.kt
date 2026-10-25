package com.grippo.data.features.api.training.models

public data class ExerciseArtifacts(
    val exerciseName: String,
    val iterations: List<IterationArtifacts>,
) {
    public data class IterationArtifacts(
        val iteration: SetIteration,
        val artifacts: List<Artifact>,
    )

    public data class Artifact(
        val field: Field,
        val reason: Reason,
        val actual: Float?,
        val reference: Float? = null,
        val referenceCount: Int = 0,
    )

    public enum class Field {
        ExternalWeight, ExtraWeight, AssistWeight, BodyWeight, BodyMultiplier, Repetitions, Load,
    }

    public enum class Reason {
        NonFinite, Negative, NonPositive, MissingRequired, UnexpectedComponent,
        ComponentMismatch, LocalOutlier, HistoryOutlier, BodyWeightMismatch,
    }
}
