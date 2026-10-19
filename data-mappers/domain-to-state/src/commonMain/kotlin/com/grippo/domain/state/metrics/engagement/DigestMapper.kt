package com.grippo.domain.state.metrics.engagement

import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.engagement.DigestState
import com.grippo.data.features.api.metrics.engagement.models.TrainingDigestResult

public fun TrainingDigestResult.toState(): DigestState {
    if (this.trainingsCount == 0) {
        return DigestState.Empty
    }

    return DigestState.Content(
        trainingsCount = this.trainingsCount,
        duration = DurationFormatState.ofTotal(this.duration),
        total = VolumeFormatState.ofTotal(this.totalVolume),
        totalSets = this.totalSets,
    )
}
