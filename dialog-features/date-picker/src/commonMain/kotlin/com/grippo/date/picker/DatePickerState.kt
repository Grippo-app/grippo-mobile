package com.grippo.date.picker

import androidx.compose.runtime.Immutable
import com.grippo.date.utils.DateRange
import com.grippo.state.formatters.DateFormatState

@Immutable
public data class DatePickerState(
    public val limitations: DateRange,
    val value: DateFormatState
)