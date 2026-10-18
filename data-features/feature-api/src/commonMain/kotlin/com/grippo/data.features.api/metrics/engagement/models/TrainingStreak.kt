package com.grippo.data.features.api.metrics.engagement.models

public data class TrainingStreak(
    val totalActiveDays: Int,
    val featured: TrainingStreakFeatured,
    val timeline: List<TrainingStreakProgressEntry>,

    val historyDays: Int,
    val lastTrainingGapDays: Int,
) {
    public val kind: TrainingStreakKind
        get() = when (featured) {
            is TrainingStreakFeatured.Daily -> TrainingStreakKind.Daily
            is TrainingStreakFeatured.Weekly -> TrainingStreakKind.Weekly
            is TrainingStreakFeatured.Rhythm -> TrainingStreakKind.Rhythm
            is TrainingStreakFeatured.Pattern -> TrainingStreakKind.Pattern
        }

    public val score: Float
        get() = featured.confidence
}
