package com.grippo.date.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
public data class DateRange(
    val from: LocalDateTime,
    val to: LocalDateTime,
)