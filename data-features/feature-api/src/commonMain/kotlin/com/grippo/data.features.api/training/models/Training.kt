package com.grippo.data.features.api.training.models

import kotlinx.datetime.LocalDateTime

public data class Training(
    val id: String,
    val exercises: List<Exercise> = emptyList(),
    val duration: Long,
    val createdAt: LocalDateTime,
    val volume: Float,
    val repetitions: Int,
    val intensity: Float
)