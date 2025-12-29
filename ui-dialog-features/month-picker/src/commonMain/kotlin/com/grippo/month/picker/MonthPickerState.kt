package com.grippo.month.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.toolkit.date.utils.DateRange

@Immutable
public data class MonthPickerState(
    val title: String,
    val limitations: DateRange,
    val value: DateFormatState
)
