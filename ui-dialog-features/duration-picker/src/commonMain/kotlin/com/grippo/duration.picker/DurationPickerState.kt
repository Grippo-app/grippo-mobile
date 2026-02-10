package com.grippo.duration.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DurationFormatState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal val DefaultDurationMinutes: PersistentList<Int> =
    (0..59).toPersistentList()

internal val DefaultDurationHours: PersistentList<Int> = run {
    val startHours = DurationFormatState.DurationLimitation.start.inWholeHours.toInt()
    val endHours = DurationFormatState.DurationLimitation.endInclusive.inWholeHours.toInt()
    (startHours..endHours).toPersistentList()
}

@Immutable
public data class DurationPickerState(
    val hours: PersistentList<Int> = DefaultDurationHours,
    val minutes: PersistentList<Int> = DefaultDurationMinutes,
    val value: DurationFormatState,
)
