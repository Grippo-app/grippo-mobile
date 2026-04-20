package com.grippo.performance.trend

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.PerformanceMetricTypeState
import com.grippo.core.state.metrics.PerformanceTrendHistoryEntry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class PerformanceTrendDialogState(
    val range: DateRangeFormatState,
    val metricType: PerformanceMetricTypeState,
    val history: ImmutableList<PerformanceTrendHistoryEntry> = persistentListOf(),
)
