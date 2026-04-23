package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.core.state.examples.stubExerciseExampleValueState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.spotlight_good_frequency_action
import com.grippo.design.resources.provider.spotlight_good_frequency_context
import com.grippo.design.resources.provider.spotlight_good_frequency_metric
import com.grippo.design.resources.provider.spotlight_good_frequency_title
import com.grippo.design.resources.provider.spotlight_near_best_action
import com.grippo.design.resources.provider.spotlight_near_best_context
import com.grippo.design.resources.provider.spotlight_near_best_metric
import com.grippo.design.resources.provider.spotlight_near_best_title
import com.grippo.design.resources.provider.spotlight_needs_attention_action
import com.grippo.design.resources.provider.spotlight_needs_attention_context
import com.grippo.design.resources.provider.spotlight_needs_attention_metric
import com.grippo.design.resources.provider.spotlight_needs_attention_title
import com.grippo.design.resources.provider.spotlight_progress_win_action
import com.grippo.design.resources.provider.spotlight_progress_win_context
import com.grippo.design.resources.provider.spotlight_progress_win_metric
import com.grippo.design.resources.provider.spotlight_progress_win_title
import kotlin.math.max

@Immutable
public data class ExerciseSpotlightState(
    val kind: Kind,
    val severity: Severity,
    val example: ExerciseExampleValueState,
    val details: Details,
    val confidence: Float,
    val score: Float,
) {
    public enum class Kind {
        ProgressWin,
        NeedsAttention,
        GoodFrequency,
        NearBest,
    }

    public enum class Severity {
        Positive,
        Warning,
    }

    @Immutable
    public sealed interface Details {
        @Immutable
        public data class ProgressWin(
            val improvementPercent: Int,
            val comparedSessions: Int,
            val latestSessionVolume: Float,
            val baselineVolumeMedian: Float,
        ) : Details

        @Immutable
        public data class NeedsAttention(
            val currentGapDays: Int,
            val typicalGapDays: Int,
            val triggerGapDays: Int,
        ) : Details

        @Immutable
        public data class GoodFrequency(
            val avgWeeklyFrequency: Float,
            val activeWeeks: Int,
            val appearancesInWindow: Int,
            val recentWindowDays: Int,
        ) : Details

        @Immutable
        public data class NearBest(
            val gapPercent: Int,
            val latestSessionVolume: Float,
            val bestSessionVolume: Float,
        ) : Details
    }

    @Composable
    public fun color(): Color {
        return when (kind) {
            Kind.ProgressWin -> AppTokens.colors.semantic.success
            Kind.NeedsAttention -> AppTokens.colors.semantic.error
            Kind.GoodFrequency -> AppTokens.colors.semantic.info
            Kind.NearBest -> AppTokens.colors.semantic.success
        }
    }

    public fun chipLabel(): UiText {
        return when (kind) {
            Kind.ProgressWin -> UiText.Res(Res.string.spotlight_progress_win_title)
            Kind.NeedsAttention -> UiText.Res(Res.string.spotlight_needs_attention_title)
            Kind.GoodFrequency -> UiText.Res(Res.string.spotlight_good_frequency_title)
            Kind.NearBest -> UiText.Res(Res.string.spotlight_near_best_title)
        }
    }

    public fun metricText(): UiText {
        return when (val value = details) {
            is Details.ProgressWin -> UiText.Res(
                Res.string.spotlight_progress_win_metric,
                listOf(value.improvementPercent, value.comparedSessions),
            )

            is Details.NeedsAttention -> UiText.Res(
                Res.string.spotlight_needs_attention_metric,
                listOf(value.currentGapDays),
            )

            is Details.GoodFrequency -> UiText.Res(
                Res.string.spotlight_good_frequency_metric,
                listOf(decimalText(value.avgWeeklyFrequency), windowWeeks(value.recentWindowDays)),
            )

            is Details.NearBest -> UiText.Res(
                Res.string.spotlight_near_best_metric,
                listOf(value.gapPercent),
            )
        }
    }

    public fun contextText(): UiText {
        return when (val value = details) {
            is Details.ProgressWin -> UiText.Res(
                Res.string.spotlight_progress_win_context,
                listOf(
                    volumeText(value.latestSessionVolume),
                    volumeText(value.baselineVolumeMedian),
                ),
            )

            is Details.NeedsAttention -> UiText.Res(
                Res.string.spotlight_needs_attention_context,
                listOf(value.typicalGapDays, value.triggerGapDays),
            )

            is Details.GoodFrequency -> UiText.Res(
                Res.string.spotlight_good_frequency_context,
                listOf(
                    value.activeWeeks,
                    windowWeeks(value.recentWindowDays),
                    value.appearancesInWindow,
                ),
            )

            is Details.NearBest -> UiText.Res(
                Res.string.spotlight_near_best_context,
                listOf(
                    volumeText(value.latestSessionVolume),
                    volumeText(value.bestSessionVolume),
                ),
            )
        }
    }

    public fun actionText(): UiText {
        return when (kind) {
            Kind.ProgressWin -> UiText.Res(Res.string.spotlight_progress_win_action)
            Kind.NeedsAttention -> UiText.Res(Res.string.spotlight_needs_attention_action)
            Kind.GoodFrequency -> UiText.Res(Res.string.spotlight_good_frequency_action)
            Kind.NearBest -> UiText.Res(Res.string.spotlight_near_best_action)
        }
    }

    private fun volumeText(value: Float): String {
        return VolumeFormatState.of(value).display
    }

    private fun decimalText(value: Float): String {
        return VolumeFormatState.of(value).display
    }

    private fun windowWeeks(recentWindowDays: Int): Int {
        return max(recentWindowDays / 7, 1)
    }
}

public fun stubExerciseSpotlightProgressWin(): ExerciseSpotlightState {
    return ExerciseSpotlightState(
        kind = ExerciseSpotlightState.Kind.ProgressWin,
        severity = ExerciseSpotlightState.Severity.Positive,
        example = stubExerciseExampleValueState(),
        details = ExerciseSpotlightState.Details.ProgressWin(
            improvementPercent = 12,
            comparedSessions = 4,
            latestSessionVolume = 28f,
            baselineVolumeMedian = 25f,
        ),
        confidence = 0.84f,
        score = 0.81f,
    )
}

public fun stubExerciseSpotlightNeedsAttention(): ExerciseSpotlightState {
    return ExerciseSpotlightState(
        kind = ExerciseSpotlightState.Kind.NeedsAttention,
        severity = ExerciseSpotlightState.Severity.Warning,
        example = stubExerciseExampleValueState(),
        details = ExerciseSpotlightState.Details.NeedsAttention(
            currentGapDays = 12,
            typicalGapDays = 7,
            triggerGapDays = 10,
        ),
        confidence = 0.76f,
        score = 0.79f,
    )
}

public fun stubExerciseSpotlightGoodFrequency(): ExerciseSpotlightState {
    return ExerciseSpotlightState(
        kind = ExerciseSpotlightState.Kind.GoodFrequency,
        severity = ExerciseSpotlightState.Severity.Positive,
        example = stubExerciseExampleValueState(),
        details = ExerciseSpotlightState.Details.GoodFrequency(
            avgWeeklyFrequency = 1.3f,
            activeWeeks = 3,
            appearancesInWindow = 5,
            recentWindowDays = 28,
        ),
        confidence = 0.73f,
        score = 0.68f,
    )
}

public fun stubExerciseSpotlightNearBest(): ExerciseSpotlightState {
    return ExerciseSpotlightState(
        kind = ExerciseSpotlightState.Kind.NearBest,
        severity = ExerciseSpotlightState.Severity.Positive,
        example = stubExerciseExampleValueState(),
        details = ExerciseSpotlightState.Details.NearBest(
            gapPercent = 4,
            latestSessionVolume = 96f,
            bestSessionVolume = 100f,
        ),
        confidence = 0.71f,
        score = 0.72f,
    )
}
