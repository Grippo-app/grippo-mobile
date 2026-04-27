package com.grippo.core.state.metrics.performance

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateTimeFormatState

@Immutable
public data class PerformanceTrendHistoryEntry(
    val endDate: DateTimeFormatState,
    val metric: PerformanceMetricState,
)
