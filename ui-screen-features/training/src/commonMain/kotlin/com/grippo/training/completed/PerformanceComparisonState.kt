package com.grippo.training.completed

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PerformanceComparisonState(
    val metrics: ImmutableList<PerformanceMetricState>,
    val reference: PerformanceReferenceState,
    val period: DateRangeFormatState,
) {
    fun isOver(range: DateRange?): Boolean = range != null && period.value == range

    fun presetLabel(range: DateRangeFormatState): UiText? = if (isOver(range.value)) range.label() else null
}
