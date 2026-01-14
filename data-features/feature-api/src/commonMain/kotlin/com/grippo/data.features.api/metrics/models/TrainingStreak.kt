package com.grippo.data.features.api.metrics.models

public enum class TrainingStreakKind {
    Daily,
    Weekly,
    Rhythm,
    Pattern,
}

public enum class TrainingStreakMood {
    CrushingIt,
    OnTrack,
    Paused,
    Restart,
}

public data class TrainingStreak(
    val totalActiveDays: Int,
    val featured: TrainingStreakFeatured,
    val timeline: List<TrainingStreakProgressEntry>,

    val kind: TrainingStreakKind,
    val score: Float,

    val historyDays: Int,
    val lastTrainingGapDays: Int,
)

public sealed interface TrainingStreakFeatured {
    public val length: Int
    public val mood: TrainingStreakMood
    public val progressPercent: Int

    /**
     * 0..1 confidence of the featured streak.
     */
    public val confidence: Float

    public data class Daily(
        override val length: Int,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeatured

    public data class Weekly(
        override val length: Int,
        val targetSessionsPerWeek: Int,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeatured

    public data class Rhythm(
        override val length: Int,
        val workDays: Int,
        val restDays: Int,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeatured

    public data class Pattern(
        override val length: Int,
        val targetSessionsPerPeriod: Int,
        val periodLengthDays: Int,
        val mask: List<Boolean>,
        override val mood: TrainingStreakMood,
        override val progressPercent: Int,
        override val confidence: Float,
    ) : TrainingStreakFeatured
}

public data class TrainingStreakProgressEntry(
    val progressPercent: Int,
    val achievedSessions: Int,
    val targetSessions: Int,
)
