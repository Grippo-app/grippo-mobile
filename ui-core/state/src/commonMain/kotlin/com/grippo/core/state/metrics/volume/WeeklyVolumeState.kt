package com.grippo.core.state.metrics.volume

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.datetime.LocalDateTime

@Immutable
public data class WeeklyVolumeState(
    val weekRange: DateRangeFormatState,
    val currentVolume: VolumeFormatState,
    val trainingsThisWeek: Int,
    val typicalVolume: VolumeFormatState?,
) {
    public fun progress(): Float? {
        val typical = typicalVolume?.value?.takeIf { it > 0f } ?: return null
        val current = currentVolume.value ?: 0f
        return (current / typical).coerceIn(0f, 1f)
    }
}

public fun stubWeeklyVolume(): WeeklyVolumeState = WeeklyVolumeState(
    weekRange = DateRangeFormatState.of(
        range = DateRange(
            from = LocalDateTime(2026, 10, 19, 0, 0),
            to = LocalDateTime(2026, 10, 25, 23, 59),
        ),
    ),
    currentVolume = VolumeFormatState.ofTotal(6_450f),
    trainingsThisWeek = 2,
    typicalVolume = VolumeFormatState.ofTotal(9_800f),
)
