package com.grippo.core.state.trainings.highlight

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
