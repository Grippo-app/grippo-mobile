package com.grippo.domain.state.metrics

import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.WeeklyDigestState
import com.grippo.data.features.api.metrics.models.WeeklyDigest

public fun WeeklyDigest?.toState(): WeeklyDigestState? {
    return this?.let { digest ->
        WeeklyDigestState(
            weekStart = digest.weekStart,
            weekEnd = digest.weekEnd,
            exercisesCount = digest.exercisesCount,
            trainingsCount = digest.trainingsCount,
            duration = digest.duration,
            total = VolumeFormatState.of(digest.totalVolume),
            totalSets = digest.totalSets,
        )
    }
}
