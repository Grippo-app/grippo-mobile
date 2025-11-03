package com.grippo.toolkit.date.utils

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
@Immutable
public data class DateRange(
    val from: LocalDateTime,
    val to: LocalDateTime,
)

public operator fun DateRange.contains(ts: LocalDateTime): Boolean {
    return (ts >= from) && (ts <= to)
}
