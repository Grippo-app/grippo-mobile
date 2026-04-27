package com.grippo.data.features.api.metrics.engagement.models

import kotlinx.datetime.LocalDateTime

public data class TrainingStreakProgressEntry(
    val progressPercent: Int,
    val achievedSessions: Int,
    val targetSessions: Int,
    val from: LocalDateTime,
    val to: LocalDateTime,
)
