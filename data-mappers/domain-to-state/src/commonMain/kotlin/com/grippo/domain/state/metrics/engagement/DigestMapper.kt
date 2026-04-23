package com.grippo.domain.state.metrics.engagement

import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.engagement.DigestState
import com.grippo.data.features.api.metrics.engagement.TrainingDigestResult

public fun TrainingDigestResult.toState(): DigestState {
    return DigestState(
        start = this.start,
        end = this.end,
        exercisesCount = this.exercisesCount,
        trainingsCount = this.trainingsCount,
        duration = this.duration,
        total = VolumeFormatState.of(this.totalVolume),
        totalSets = this.totalSets,
    )
}
