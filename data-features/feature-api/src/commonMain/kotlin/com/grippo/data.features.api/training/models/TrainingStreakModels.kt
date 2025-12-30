package com.grippo.data.features.api.training.models

public data class TrainingStreak(
    val totalActiveDays: Int,
    val featured: TrainingStreakFeatured,
    val timeline: List<TrainingStreakProgressEntry>,
)

public data class TrainingStreakFeatured(
    val type: TrainingStreakType,
    val length: Int,
    val targetSessionsPerPeriod: Int,
    val periodLengthDays: Int,
    val mood: TrainingStreakMood,
    val progressPercent: Int,
    val rhythm: TrainingStreakRhythm? = null,
)

public enum class TrainingStreakType {
    Daily,
    Weekly,
    Rhythm,
}

public enum class TrainingStreakMood {
    CrushingIt,
    OnTrack,
    Restart,
}

public data class TrainingStreakProgressEntry(
    val progressPercent: Int,
    val achievedSessions: Int,
    val targetSessions: Int,
)

public data class TrainingStreakRhythm(
    val workDays: Int,
    val restDays: Int,
)
