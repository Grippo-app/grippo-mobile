package com.grippo.data.features.api.weight.history.models

import kotlinx.datetime.LocalDateTime

public data class WeightHistory(
    val id: String,
    val weight: Float,
    val createdAt: LocalDateTime
)