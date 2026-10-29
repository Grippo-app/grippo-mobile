package com.grippo.performance.trend.details

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.core.state.metrics.performance.PerformanceTrendHistoryEntry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class PerformanceTrendDetailsDialogState(
    val range: DateRangeFormatState,
    val metricType: PerformanceMetricTypeState,
    val reference: PerformanceReferenceState,
    val history: ImmutableList<PerformanceTrendHistoryEntry> = persistentListOf(),
)
