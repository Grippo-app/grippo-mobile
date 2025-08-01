package com.grippo.period.picker

import androidx.compose.runtime.Immutable
import com.grippo.state.datetime.PeriodState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class PeriodPickerState(
    val list: ImmutableList<PeriodState> = persistentListOf(),
    val initial: PeriodState
)