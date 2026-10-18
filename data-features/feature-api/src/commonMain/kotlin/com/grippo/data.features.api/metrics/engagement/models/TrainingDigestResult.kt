package com.grippo.data.features.api.metrics.engagement.models

import com.grippo.toolkit.date.utils.DateRange
import kotlin.time.Duration

public data class TrainingDigestResult(
    val range: DateRange,
    val trainingsCount: Int,
    val duration: Duration?,
    val totalVolume: Float?,
    val totalSets: Int,
    val activeDays: Int,
    val avgVolumePerTraining: Float?,
)
