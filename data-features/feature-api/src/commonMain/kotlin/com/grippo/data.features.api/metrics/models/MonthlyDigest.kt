package com.grippo.data.features.api.metrics.models

import kotlinx.datetime.LocalDate
import kotlin.time.Duration

public data class MonthlyDigest(
    val month: LocalDate,
    val exercisesCount: Int,
    val trainingsCount: Int,
    val duration: Duration,
    val totalVolume: Float,
    val totalSets: Int,
)
