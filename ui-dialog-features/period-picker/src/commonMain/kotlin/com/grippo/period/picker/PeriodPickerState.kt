package com.grippo.period.picker

import androidx.compose.runtime.Immutable
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class PeriodPickerState(
    val title: String,
    val value: DateRange.Range,
    val suggestions: ImmutableList<DateRange.Range> = persistentListOf(
        DateRange.Range.Last7Days(),
        DateRange.Range.Last14Days(),
        DateRange.Range.Last30Days(),
        DateRange.Range.Last60Days(),
    )
)