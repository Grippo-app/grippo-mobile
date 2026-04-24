package com.grippo.data.features.api.metrics.engagement.models

import kotlinx.datetime.LocalDate
import kotlin.time.Duration

public data class TrainingDigestResult(
    val start: LocalDate,
    val end: LocalDate,
    val trainingsCount: Int,
    val duration: Duration,
    val totalVolume: Float,
    val totalSets: Int,
    val activeDays: Int,
    val avgVolumePerTraining: Float,
)
