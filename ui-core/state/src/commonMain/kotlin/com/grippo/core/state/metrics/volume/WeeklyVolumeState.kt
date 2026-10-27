package com.grippo.core.state.metrics.volume

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.weekly_volume_no_workouts
import com.grippo.design.resources.provider.weekly_volume_workouts
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.datetime.LocalDateTime

@Immutable
public data class WeeklyVolumeState(
    val weekRange: DateRangeFormatState,
    val currentVolume: VolumeFormatState,
    val trainingsThisWeek: Int,
    val typicalVolume: VolumeFormatState?,
) {
    public fun usualVolume(): Float? = typicalVolume?.value?.takeIf { volume -> volume > 0f }

    public fun progress(): Float? {
        val typical = usualVolume() ?: return null
        val current = currentVolume.value ?: 0f
        return (current / typical).coerceIn(0f, 1f)
    }

    @Composable
    public fun workoutsLabel(): String = if (trainingsThisWeek == 0) {
        AppTokens.strings.res(Res.string.weekly_volume_no_workouts)
    } else {
        AppTokens.strings.res(Res.plurals.weekly_volume_workouts, trainingsThisWeek, trainingsThisWeek)
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
