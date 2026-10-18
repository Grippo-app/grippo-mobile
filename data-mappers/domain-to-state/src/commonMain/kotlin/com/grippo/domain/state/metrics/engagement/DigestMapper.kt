package com.grippo.domain.state.metrics.engagement

import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.engagement.DigestState
import com.grippo.data.features.api.metrics.engagement.models.TrainingDigestResult

public fun TrainingDigestResult.toState(): DigestState {
    val rangeFormat = DateRangeFormatState.of(range)

    if (this.trainingsCount == 0) {
        return DigestState.Empty(range = rangeFormat)
    }

    return DigestState.Content(
        range = rangeFormat,
        trainingsCount = this.trainingsCount,
        duration = DurationFormatState.ofTotal(this.duration),
        total = VolumeFormatState.ofTotal(this.totalVolume),
        avgVolume = VolumeFormatState.ofTotal(this.avgVolumePerTraining),
        totalSets = this.totalSets,
        activeDays = this.activeDays,
    )
}
