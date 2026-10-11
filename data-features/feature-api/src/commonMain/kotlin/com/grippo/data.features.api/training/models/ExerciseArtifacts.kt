package com.grippo.data.features.api.training.models

/** Soft validation hints. No finding authorizes correcting or rejecting a user's set. */
public data class ExerciseArtifacts(
    val exerciseName: String,
    /** Every input iteration in its original order, including clean iterations. */
    val iterations: List<IterationArtifacts>,
) {
    public data class IterationArtifacts(
        val iteration: SetIteration,
        val artifacts: List<Artifact>,
    )

    public data class Artifact(
        val field: Field,
        val reason: Reason,
        /** Null for a missing required component. */
        val actual: Float?,
        /** A reference, not a proposed correction or a physiological limit. */
        val reference: Float? = null,
        /** Supporting peer sets for LocalOutlier; supporting days for HistoryOutlier. */
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
