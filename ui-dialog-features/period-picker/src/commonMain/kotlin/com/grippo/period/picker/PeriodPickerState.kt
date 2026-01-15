package com.grippo.period.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.PeriodFormatState
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class PeriodPickerState(
    val title: String,
    val value: PeriodFormatState,
    val suggestions: ImmutableList<DateRange> = persistentListOf(
        DateTimeUtils.thisWeek(),
        DateTimeUtils.thisMonth(),
        DateTimeUtils.thisYear(),
    )
)