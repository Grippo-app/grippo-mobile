package com.grippo.domain.state.metrics

import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.MonthlyDigestState
import com.grippo.data.features.api.metrics.models.MonthlyDigest

public fun MonthlyDigest?.toState(): MonthlyDigestState? {
    return this?.let { digest ->
        MonthlyDigestState(
            month = digest.month,
            exercisesCount = digest.exercisesCount,
            trainingsCount = digest.trainingsCount,
            duration = digest.duration,
            total = VolumeFormatState.of(digest.totalVolume),
            totalSets = digest.totalSets,
        )
    }
}
