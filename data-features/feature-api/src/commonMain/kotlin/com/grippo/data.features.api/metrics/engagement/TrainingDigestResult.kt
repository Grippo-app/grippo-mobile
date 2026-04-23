package com.grippo.data.features.api.metrics.engagement

import kotlinx.datetime.LocalDate
import kotlin.time.Duration

public data class TrainingDigestResult(
    val start: LocalDate,
    val end: LocalDate,
    val exercisesCount: Int,
    val trainingsCount: Int,
    val duration: Duration,
    val totalVolume: Float,
    val totalSets: Int,
)
