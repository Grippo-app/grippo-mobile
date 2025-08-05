package com.grippo.date.picker

import androidx.compose.runtime.Immutable
import com.grippo.date.utils.DateRange
import kotlinx.datetime.LocalDateTime

@Immutable
public data class DatePickerState(
    public val limitations: DateRange,
    val initial: LocalDateTime
)