package com.grippo.core.state.metrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.stubExerciseExampleValueState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.spotlight_anchor_title
import com.grippo.design.resources.provider.spotlight_best_progress_description
import com.grippo.design.resources.provider.spotlight_best_title
import com.grippo.design.resources.provider.spotlight_missing_description
import com.grippo.design.resources.provider.spotlight_missing_title
import com.grippo.design.resources.provider.spotlight_most_consistent_description

@Immutable
public sealed interface ExerciseSpotlightState {
    public val example: ExerciseExampleValueState
    public val totalVolume: VolumeFormatState
    public val sessionCount: Int

    @Immutable
    public data class MostConsistentState(
        override val example: ExerciseExampleValueState,
        override val totalVolume: VolumeFormatState,
        override val sessionCount: Int,
        val trainingsCount: Int,
        val coverageRatio: Float,
    ) : ExerciseSpotlightState

    @Immutable
    public data class BestProgressState(
        override val example: ExerciseExampleValueState,
        override val totalVolume: VolumeFormatState,
        override val sessionCount: Int,
        val baselineVolumeMedian: VolumeFormatState,
        val lastSessionVolume: VolumeFormatState,
        val progressDelta: Float,
        val progressRatio: Float,
    ) : ExerciseSpotlightState

    @Immutable
    public data class ComebackMissingState(
        override val example: ExerciseExampleValueState,
        override val totalVolume: VolumeFormatState,
        override val sessionCount: Int,
        val typicalGap: Float,
        val currentGap: Int,
        val score: Float,
    ) : ExerciseSpotlightState

    @Composable
    public fun title(): String {
        return when (this) {
            is BestProgressState -> AppTokens.strings.res(Res.string.spotlight_best_title)
            is ComebackMissingState -> AppTokens.strings.res(Res.string.spotlight_missing_title)
            is MostConsistentState -> AppTokens.strings.res(Res.string.spotlight_anchor_title)
        }
    }

    @Composable
    public fun description(): String {
        return when (this) {
            is MostConsistentState ->
                AppTokens.strings.res(
                    Res.string.spotlight_most_consistent_description,
                    sessionCount,
                    trainingsCount,
                )

            is BestProgressState ->
                AppTokens.strings.res(
                    Res.string.spotlight_best_progress_description,
                    progressDelta,
                )

            is ComebackMissingState ->
                AppTokens.strings.res(
                    Res.string.spotlight_missing_description,
                    currentGap,
                    typicalGap,
                )
        }
    }

    @Composable
    public fun color(): Color {
        return when (this) {
            is MostConsistentState -> AppTokens.colors.semantic.info
            is BestProgressState -> AppTokens.colors.semantic.success
            is ComebackMissingState -> AppTokens.colors.semantic.warning
        }
    }
}

public fun stubExerciseSpotlightMostConsistent(): ExerciseSpotlightState.MostConsistentState {
    return ExerciseSpotlightState.MostConsistentState(
        example = stubExerciseExampleValueState(),
        totalVolume = VolumeFormatState.of(1240f),
        sessionCount = 12,
        trainingsCount = 9,
        coverageRatio = 0.75f,
    )
}

public fun stubExerciseSpotlightBestProgress(): ExerciseSpotlightState.BestProgressState {
    return ExerciseSpotlightState.BestProgressState(
        example = stubExerciseExampleValueState(),
        totalVolume = VolumeFormatState.of(1580f),
        sessionCount = 14,
        baselineVolumeMedian = VolumeFormatState.of(420f),
        lastSessionVolume = VolumeFormatState.of(560f),
        progressDelta = 140f,
        progressRatio = 0.33f,
    )
}

public fun stubExerciseSpotlightComebackMissing(): ExerciseSpotlightState.ComebackMissingState {
    return ExerciseSpotlightState.ComebackMissingState(
        example = stubExerciseExampleValueState(),
        totalVolume = VolumeFormatState.of(980f),
        sessionCount = 10,
        typicalGap = 4f,
        currentGap = 9,
        score = 0.62f,
    )
}
