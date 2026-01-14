package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable

@Immutable
public data class TrainingStreakState(
    val totalActiveDays: Int,
    val featured: TrainingStreakFeaturedState,
    val timeline: List<TrainingStreakProgressState>,
    val kind: TrainingStreakKind,
    val score: Float,
    val historyDays: Int,
    val lastTrainingGapDays: Int,
)

@Immutable
public enum class TrainingStreakKind {
    Daily,
    Weekly,
    Rhythm,
    Pattern,
}

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
        val mask: List<Boolean>,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeaturedState
}

@Immutable
public enum class TrainingStreakMood {
    CrushingIt,
    OnTrack,
    Paused,
    Restart,
}

@Immutable
public data class TrainingStreakProgressState(
    val progressPercent: Int,
    val achievedSessions: Int,
    val targetSessions: Int,
)

public fun stubTrainingStreaks(): List<TrainingStreakState> {
    return listOf(
        TrainingStreakState(
            totalActiveDays = 9,
            featured = TrainingStreakFeaturedState.Daily(
                length = 5,
                mood = TrainingStreakMood.OnTrack,
                progressPercent = 60,
                confidence = 0.8f,
            ),
            timeline = listOf(
                TrainingStreakProgressState(
                    progressPercent = 100,
                    achievedSessions = 1,
                    targetSessions = 1
                ),
                TrainingStreakProgressState(
                    progressPercent = 100,
                    achievedSessions = 1,
                    targetSessions = 1
                ),
                TrainingStreakProgressState(
                    progressPercent = 50,
                    achievedSessions = 1,
                    targetSessions = 2
                ),
            ),
            kind = TrainingStreakKind.Daily,
            score = 0.72f,
            historyDays = 30,
            lastTrainingGapDays = 0,
        ),
        TrainingStreakState(
            totalActiveDays = 14,
            featured = TrainingStreakFeaturedState.Weekly(
                length = 3,
                targetSessionsPerPeriod = 4,
                mood = TrainingStreakMood.Paused,
                progressPercent = 40,
                confidence = 0.7f,
            ),
            timeline = listOf(
                TrainingStreakProgressState(
                    progressPercent = 75,
                    achievedSessions = 3,
                    targetSessions = 4
                ),
                TrainingStreakProgressState(
                    progressPercent = 40,
                    achievedSessions = 2,
                    targetSessions = 5
                ),
                TrainingStreakProgressState(
                    progressPercent = 20,
                    achievedSessions = 1,
                    targetSessions = 5
                ),
            ),
            kind = TrainingStreakKind.Weekly,
            score = 0.48f,
            historyDays = 60,
            lastTrainingGapDays = 4,
        ),
        TrainingStreakState(
            totalActiveDays = 16,
            featured = TrainingStreakFeaturedState.Rhythm(
                length = 4,
                workDays = 2,
                restDays = 1,
                mood = TrainingStreakMood.CrushingIt,
                progressPercent = 80,
                confidence = 0.9f,
            ),
            timeline = listOf(
                TrainingStreakProgressState(
                    progressPercent = 100,
                    achievedSessions = 2,
                    targetSessions = 2
                ),
                TrainingStreakProgressState(
                    progressPercent = 100,
                    achievedSessions = 2,
                    targetSessions = 2
                ),
                TrainingStreakProgressState(
                    progressPercent = 60,
                    achievedSessions = 1,
                    targetSessions = 2
                ),
                TrainingStreakProgressState(
                    progressPercent = 80,
                    achievedSessions = 2,
                    targetSessions = 2
                ),
            ),
            kind = TrainingStreakKind.Rhythm,
            score = 0.88f,
            historyDays = 45,
            lastTrainingGapDays = 1,
        )
    )
}
