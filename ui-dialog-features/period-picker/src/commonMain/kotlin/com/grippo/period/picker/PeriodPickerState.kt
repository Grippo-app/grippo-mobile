package com.grippo.period.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class PeriodPickerState(
    val title: String,
    val value: DateRangeFormatState,
    val suggestions: ImmutableList<DateRangeFormatState> = persistentListOf(
        DateRangeFormatState.of(DateRange.Range.Last7Days()),
        DateRangeFormatState.of(DateRange.Range.Last14Days()),
        DateRangeFormatState.of(DateRange.Range.Last30Days()),
        DateRangeFormatState.of(DateRange.Range.Last60Days()),
    )
)
