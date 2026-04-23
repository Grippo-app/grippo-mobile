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
    return DigestState(
        range = DateRangeFormatState.of(range),
        exercisesCount = this.exercisesCount,
        trainingsCount = this.trainingsCount,
        duration = DurationFormatState.of(this.duration),
        total = VolumeFormatState.of(this.totalVolume),
        totalSets = this.totalSets,
    )
}
