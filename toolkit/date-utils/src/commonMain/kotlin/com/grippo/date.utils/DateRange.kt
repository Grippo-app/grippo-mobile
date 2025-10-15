package com.grippo.date.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
public data class DateRange(
    val from: LocalDateTime,
    val to: LocalDateTime,
)

public operator fun DateRange.contains(ts: LocalDateTime): Boolean {
    return (ts >= from) && (ts <= to)
}
