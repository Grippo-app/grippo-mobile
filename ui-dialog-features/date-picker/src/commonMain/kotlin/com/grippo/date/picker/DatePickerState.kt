package com.grippo.date.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.toolkit.date.utils.DateRange

@Immutable
public data class DatePickerState(
    val title: String,
    public val limitations: DateRange,
    val value: DateFormatState
)