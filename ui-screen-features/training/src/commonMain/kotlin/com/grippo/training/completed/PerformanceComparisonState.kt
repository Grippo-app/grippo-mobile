package com.grippo.training.completed

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.performance.PerformanceBaselineState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PerformanceComparisonState(
    val metrics: ImmutableList<PerformanceMetricState>,
    val baseline: PerformanceBaselineState,
    val period: DateRangeFormatState,
) {
    fun isOver(range: DateRange?): Boolean = range != null && period.value == range
}
