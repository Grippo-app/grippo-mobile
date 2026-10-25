package com.grippo.data.features.api.metrics.engagement.models

import kotlinx.datetime.LocalDateTime

public data class TrainingStreakProgressEntry(
    val achievedTrainingDays: Int,
    val targetTrainingDays: Int,
    val isPartial: Boolean,
    val from: LocalDateTime,
    val to: LocalDateTime,
)
