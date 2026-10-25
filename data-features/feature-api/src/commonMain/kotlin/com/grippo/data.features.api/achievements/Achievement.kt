package com.grippo.data.features.api.achievements

public sealed class Achievement(public open val exerciseExampleId: String) {
    public data class BestTonnage(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val tonnage: Float
    ) : Achievement(
        exerciseExampleId = exerciseExampleId,
    )

    public data class BestWeight(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val iterationId: String,
        val weight: Float
    ) : Achievement(
        exerciseExampleId = exerciseExampleId,
    )

    public data class LifetimeVolume(
        override val exerciseExampleId: String,
        val totalVolume: Float,
        val sessionsCount: Int
    ) : Achievement(
        exerciseExampleId = exerciseExampleId,
    )

    public data class MaxRepetitions(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val iterationId: String,
        val repetitions: Int
    ) : Achievement(
        exerciseExampleId = exerciseExampleId,
    )

    public data class PeakIntensity(
        override val exerciseExampleId: String,
        val exerciseId: String,
        val intensity: Double,
    ) : Achievement(
        exerciseExampleId = exerciseExampleId,
    )
}
