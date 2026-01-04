package com.grippo.domain.state.metrics

import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.MonthlyDigestState
import com.grippo.data.features.api.metrics.models.MonthlyDigest

public fun MonthlyDigest.toState(): MonthlyDigestState {
    return MonthlyDigestState(
        month = this.month,
        exercisesCount = this.exercisesCount,
        trainingsCount = this.trainingsCount,
        duration = this.duration,
        total = VolumeFormatState.of(this.totalVolume),
        totalSets = this.totalSets,
    )
}
