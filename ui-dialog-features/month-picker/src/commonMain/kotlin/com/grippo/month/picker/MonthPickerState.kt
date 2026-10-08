package com.grippo.month.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.toolkit.date.utils.DateRange

@Immutable
public data class MonthPickerState(
    val limitations: DateRange,
    val value: DateTimeFormatState
)
