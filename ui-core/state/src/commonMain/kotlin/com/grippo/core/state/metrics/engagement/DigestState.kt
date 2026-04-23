package com.grippo.core.state.metrics.engagement

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlin.time.Duration

@Immutable
public data class DigestState(
    val range: DateRangeFormatState,
    val exercisesCount: Int,
    val trainingsCount: Int,
    val duration: DurationFormatState,
    val total: VolumeFormatState,
    val totalSets: Int,
)

public fun stubDigest(): DigestState {
    val today = DateTimeUtils.now().date
    val start = today.minus(DatePeriod(days = 6))
    val range = DateRange(
        from = DateTimeUtils.startOfDay(start),
        to = DateTimeUtils.endOfDay(today),
    )
    return DigestState(
        range = DateRangeFormatState.of(range),
        exercisesCount = 28,
        duration = DurationFormatState.of(Duration.parse("7h")),
        total = VolumeFormatState.of(540F),
        totalSets = 32,
        trainingsCount = 5,
    )
}
