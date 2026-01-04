package com.grippo.domain.state.metrics

import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.WeeklyDigestState
import com.grippo.data.features.api.metrics.models.WeeklyDigest

public fun WeeklyDigest.toState(): WeeklyDigestState {
    return WeeklyDigestState(
        weekStart = this.weekStart,
        weekEnd = this.weekEnd,
        exercisesCount = this.exercisesCount,
        trainingsCount = this.trainingsCount,
        duration = this.duration,
        total = VolumeFormatState.of(this.totalVolume),
        totalSets = this.totalSets,
    )
}
