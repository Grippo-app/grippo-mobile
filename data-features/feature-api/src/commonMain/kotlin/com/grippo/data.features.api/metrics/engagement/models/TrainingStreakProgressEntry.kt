package com.grippo.data.features.api.metrics.engagement.models

import kotlinx.datetime.LocalDateTime

/** Progress counts distinct training dates, regardless of sessions on the same date. */
public data class TrainingStreakProgressEntry(
    val achievedTrainingDays: Int,
    val targetTrainingDays: Int,
    val isPartial: Boolean,
    val from: LocalDateTime,
    val to: LocalDateTime,
)
