package com.grippo.core.state.metrics

import androidx.compose.runtime.Immutable

@Immutable
public data class TrainingStreakState(
    val totalActiveDays: Int,
    val featured: TrainingStreakFeaturedState,
    val timeline: List<TrainingStreakProgressState>,
)

@Immutable
public data class TrainingStreakFeaturedState(
    val type: TrainingStreakType,
    val length: Int,
    val targetSessionsPerPeriod: Int,
    val periodLengthDays: Int,
    val mood: TrainingStreakMood,
    val progressPercent: Int,
    val rhythm: TrainingStreakRhythmState? = null,
)

@Immutable
public enum class TrainingStreakType {
    Daily,
    Weekly,
    Rhythm,
}

@Immutable
public enum class TrainingStreakMood {
    CrushingIt,
    OnTrack,
    Restart,
}

@Immutable
public data class TrainingStreakProgressState(
    val progressPercent: Int,
    val achievedSessions: Int,
    val targetSessions: Int,
)

@Immutable
public data class TrainingStreakRhythmState(
    val workDays: Int,
    val restDays: Int,
)

public fun stubTrainingStreakStates(): List<TrainingStreakState> {
    return listOf(
        TrainingStreakState(
            totalActiveDays = 9,
            featured = TrainingStreakFeaturedState(
                type = TrainingStreakType.Daily,
                length = 5,
                targetSessionsPerPeriod = 1,
                periodLengthDays = 1,
                mood = TrainingStreakMood.OnTrack,
                progressPercent = 60,
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
            )
        ),
        TrainingStreakState(
            totalActiveDays = 14,
            featured = TrainingStreakFeaturedState(
                type = TrainingStreakType.Weekly,
                length = 3,
                targetSessionsPerPeriod = 4,
                periodLengthDays = 7,
                mood = TrainingStreakMood.Restart,
                progressPercent = 40,
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
            )
        ),
        TrainingStreakState(
            totalActiveDays = 16,
            featured = TrainingStreakFeaturedState(
                type = TrainingStreakType.Rhythm,
                length = 4,
                targetSessionsPerPeriod = 2,
                periodLengthDays = 3,
                mood = TrainingStreakMood.CrushingIt,
                progressPercent = 80,
                rhythm = TrainingStreakRhythmState(workDays = 2, restDays = 1),
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
            )
        )
    )
}