package com.grippo.core.state.metrics.engagement

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlin.time.Duration

@Immutable
public sealed interface DigestState {
    public val range: DateRangeFormatState

    @Immutable
    public data class Content(
        override val range: DateRangeFormatState,
        val trainingsCount: Int,
        val duration: DurationFormatState,
        val total: VolumeFormatState,
        val avgVolume: VolumeFormatState,
        val totalSets: Int,
        val activeDays: Int,
    ) : DigestState

    @Immutable
    public data class Empty(
        override val range: DateRangeFormatState,
    ) : DigestState
}

public fun stubDigest(): DigestState.Content {
    val today = LocalDate(2026, 10, 18)
    val start = today.minus(DatePeriod(days = 6))
    val range = DateRange(
        from = DateTimeUtils.startOfDay(start),
        to = DateTimeUtils.endOfDay(today),
    )
    return DigestState.Content(
        range = DateRangeFormatState.of(range),
        trainingsCount = 5,
        duration = DurationFormatState.ofTotal(Duration.parse("7h")),
        total = VolumeFormatState.ofTotal(540F),
        avgVolume = VolumeFormatState.ofTotal(108F),
        totalSets = 32,
        activeDays = 4,
    )
}
