package com.grippo.data.features.api.user.models

import kotlin.time.Duration

public data class UserStats(
    val trainingsCount: Int,
    val totalDuration: Duration,
    val totalVolume: Float,
    val totalRepetitions: Int,
)
