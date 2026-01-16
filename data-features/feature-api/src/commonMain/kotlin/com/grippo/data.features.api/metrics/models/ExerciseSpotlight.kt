package com.grippo.data.features.api.metrics.models

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue

public sealed interface ExerciseSpotlight {
    public val example: ExerciseExampleValue
    public val totalVolume: Float
    public val sessionCount: Int

    public data class MostConsistent(
        override val example: ExerciseExampleValue,
        override val totalVolume: Float,
        override val sessionCount: Int,
        val trainingsCount: Int,
        val coverageRatio: Float,
    ) : ExerciseSpotlight

    public data class BestProgress(
        override val example: ExerciseExampleValue,
        override val totalVolume: Float,
        override val sessionCount: Int,
        val baselineVolumeMedian: Float,
        val lastSessionVolume: Float,
        val progressDelta: Float,
        val progressRatio: Float,
    ) : ExerciseSpotlight

    public data class ComebackMissing(
        override val example: ExerciseExampleValue,
        override val totalVolume: Float,
        override val sessionCount: Int,
        val typicalGap: Float,
        val currentGap: Int,
        val score: Float,
    ) : ExerciseSpotlight
}
