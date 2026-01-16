package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.stubExerciseExampleValueState

@Immutable
public sealed interface ExerciseSpotlightState {
    public val example: ExerciseExampleValueState
    public val totalVolume: Float
    public val sessionCount: Int

    @Immutable
    public data class MostConsistentState(
        override val example: ExerciseExampleValueState,
        override val totalVolume: Float,
        override val sessionCount: Int,
        val trainingsCount: Int,
        val coverageRatio: Float,
    ) : ExerciseSpotlightState

    @Immutable
    public data class BestProgressState(
        override val example: ExerciseExampleValueState,
        override val totalVolume: Float,
        override val sessionCount: Int,
        val baselineVolumeMedian: Float,
        val lastSessionVolume: Float,
        val progressDelta: Float,
        val progressRatio: Float,
    ) : ExerciseSpotlightState

    @Immutable
    public data class ComebackMissingState(
        override val example: ExerciseExampleValueState,
        override val totalVolume: Float,
        override val sessionCount: Int,
        val typicalGap: Float,
        val currentGap: Int,
        val score: Float,
    ) : ExerciseSpotlightState
}

public fun stubExerciseSpotlightMostConsistent(): ExerciseSpotlightState.MostConsistentState {
    return ExerciseSpotlightState.MostConsistentState(
        example = stubExerciseExampleValueState(),
        totalVolume = 1240f,
        sessionCount = 12,
        trainingsCount = 9,
        coverageRatio = 0.75f,
    )
}

public fun stubExerciseSpotlightBestProgress(): ExerciseSpotlightState.BestProgressState {
    return ExerciseSpotlightState.BestProgressState(
        example = stubExerciseExampleValueState(),
        totalVolume = 1580f,
        sessionCount = 14,
        baselineVolumeMedian = 420f,
        lastSessionVolume = 560f,
        progressDelta = 140f,
        progressRatio = 0.33f,
    )
}

public fun stubExerciseSpotlightComebackMissing(): ExerciseSpotlightState.ComebackMissingState {
    return ExerciseSpotlightState.ComebackMissingState(
        example = stubExerciseExampleValueState(),
        totalVolume = 980f,
        sessionCount = 10,
        typicalGap = 4f,
        currentGap = 9,
        score = 0.62f,
    )
}
