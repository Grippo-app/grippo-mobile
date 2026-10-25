package com.grippo.data.features.api.metrics.volume.models

import com.grippo.toolkit.date.utils.DateRange

public data class WeeklyVolume(
    val weekRange: DateRange,
    val currentVolume: Float,
    val trainingsThisWeek: Int,
    val typicalVolume: Float?,
)
