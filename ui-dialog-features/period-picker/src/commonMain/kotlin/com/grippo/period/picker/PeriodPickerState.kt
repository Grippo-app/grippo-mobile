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
        DateRangeFormatState.ofPreset(DateRangeKind.Last7Days),
        DateRangeFormatState.ofPreset(DateRangeKind.Last14Days),
        DateRangeFormatState.ofPreset(DateRangeKind.Last30Days),
        DateRangeFormatState.ofPreset(DateRangeKind.Last60Days),
    )
)
