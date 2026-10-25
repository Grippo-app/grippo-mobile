package com.grippo.domain.state.metrics.volume

import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.volume.WeeklyVolumeState
import com.grippo.data.features.api.metrics.volume.models.WeeklyVolume

public fun WeeklyVolume.toState(): WeeklyVolumeState {
    return WeeklyVolumeState(
        weekRange = DateRangeFormatState.of(weekRange),
        currentVolume = VolumeFormatState.ofTotal(currentVolume),
        trainingsThisWeek = trainingsThisWeek,
        typicalVolume = typicalVolume?.let(VolumeFormatState::ofTotal),
    )
}
