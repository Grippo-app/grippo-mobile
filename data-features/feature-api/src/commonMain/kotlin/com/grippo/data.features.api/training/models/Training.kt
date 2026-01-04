package com.grippo.data.features.api.training.models

import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

public data class Training(
    val id: String,
    val exercises: List<Exercise>,
    val duration: Duration,
    val createdAt: LocalDateTime,
    val volume: Float,
    val repetitions: Int,
    val intensity: Float
)