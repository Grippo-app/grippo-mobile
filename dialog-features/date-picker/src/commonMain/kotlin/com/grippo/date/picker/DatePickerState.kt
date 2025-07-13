package com.grippo.date.picker

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDateTime

@Immutable
public data class DatePickerState(
    public val limitations: Pair<LocalDateTime, LocalDateTime> =
        LocalDateTime(2024, 7, 9, 14, 30) to LocalDateTime(2026, 7, 9, 14, 30),
    val initial: LocalDateTime
)