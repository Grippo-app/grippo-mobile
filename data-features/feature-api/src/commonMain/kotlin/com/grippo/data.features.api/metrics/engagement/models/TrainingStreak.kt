package com.grippo.data.features.api.metrics.engagement.models

public data class TrainingStreak(
    val totalActiveDays: Int,
    val featured: TrainingStreakFeatured,
    val timeline: List<TrainingStreakProgressEntry>,

    val kind: TrainingStreakKind,
    val score: Float,

    val historyDays: Int,
    val lastTrainingGapDays: Int,
)
