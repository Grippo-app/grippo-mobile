package com.grippo.core.state.metrics.engagement

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
public sealed interface TrainingStreakFeaturedState {
    public val length: Int
    public val targetSessionsPerPeriod: Int
    public val periodLengthDays: Int
    public val mood: TrainingStreakMood
    public val progressPercent: Int
    public val confidence: Float

    @Immutable
    public data class Daily(
        override val length: Int,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeaturedState {
        override val targetSessionsPerPeriod: Int = 1
        override val periodLengthDays: Int = 1
    }

    @Immutable
    public data class Weekly(
        override val length: Int,
        override val targetSessionsPerPeriod: Int,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeaturedState {
        override val periodLengthDays: Int = 7
    }

    @Immutable
    public data class Rhythm(
        override val length: Int,
        val workDays: Int,
        val restDays: Int,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeaturedState {
        override val targetSessionsPerPeriod: Int = workDays
        override val periodLengthDays: Int = workDays + restDays
    }

    @Immutable
    public data class Pattern(
        override val length: Int,
        override val targetSessionsPerPeriod: Int,
        override val periodLengthDays: Int,
        val mask: ImmutableList<Boolean>,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeaturedState
}
