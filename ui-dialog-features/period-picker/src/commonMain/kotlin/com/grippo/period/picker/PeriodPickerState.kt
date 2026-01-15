package com.grippo.period.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.PeriodFormatState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class PeriodPickerState(
    val title: String,
    val value: PeriodFormatState,
    val suggestions: ImmutableList<DateRange> = persistentListOf(
        DateRange.Range.Weekly().range,
        DateRange.Range.Last7Days().range,
        DateRange.Range.Monthly().range,
        DateRange.Range.Last30Days().range,
    )
)