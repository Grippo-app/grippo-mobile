package com.grippo.domain.state.metrics.engagement

import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.engagement.DigestState
import com.grippo.data.features.api.metrics.engagement.models.TrainingDigestResult
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils

public fun TrainingDigestResult.toState(): DigestState {
    val range = DateRange(
        from = DateTimeUtils.startOfDay(this.start),
        to = DateTimeUtils.endOfDay(this.end),
    )
    val rangeFormat = DateRangeFormatState.of(range)

    if (this.trainingsCount == 0) {
        return DigestState.Empty(range = rangeFormat)
    }

    return DigestState.Content(
        range = rangeFormat,
        trainingsCount = this.trainingsCount,
        duration = DurationFormatState.of(this.duration),
        total = VolumeFormatState.of(this.totalVolume),
        avgVolume = VolumeFormatState.of(this.avgVolumePerTraining),
        totalSets = this.totalSets,
        activeDays = this.activeDays,
    )
}
