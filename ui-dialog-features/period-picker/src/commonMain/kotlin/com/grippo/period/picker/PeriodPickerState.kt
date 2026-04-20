package com.grippo.period.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class PeriodPickerState(
    val title: String,
    val value: DateRangeFormatState,
    val suggestions: ImmutableList<DateRangeFormatState> = persistentListOf(
        DateRangeFormatState.of(DateRangeKind.Last7Days),
        DateRangeFormatState.of(DateRangeKind.Last14Days),
        DateRangeFormatState.of(DateRangeKind.Last30Days),
        DateRangeFormatState.of(DateRangeKind.Last60Days),
    )
)
