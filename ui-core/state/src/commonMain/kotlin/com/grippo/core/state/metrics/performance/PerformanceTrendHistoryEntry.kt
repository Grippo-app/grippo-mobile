package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

@Immutable
public data class PerformanceTrendHistoryEntry(
    val trainingId: String,
    val endDate: DateTimeFormatState,
    val metric: PerformanceMetricState,
)

public fun stubPerformanceTrendHistory(): ImmutableList<PerformanceTrendHistoryEntry> {
    val metric = stubPerformanceMetrics().filterIsInstance<PerformanceMetricState.Volume>().first()
    return persistentListOf(1_200f, 900f, 780f).mapIndexed { index, value ->
        PerformanceTrendHistoryEntry(
            trainingId = "preview-performance-history-$index",
            endDate = DateTimeFormatState.of(
                value = LocalDateTime(2026, 10, 15 - index, 10, 0),
                range = DateRangePresets.infinity(),
                format = DateFormat.DateOnly.DateDdMmmYyyy,
            ),
            metric = metric.copy(current = value),
        )
    }.toPersistentList()
}
