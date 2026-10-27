package com.grippo.training.completed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_recap_period_as_of
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PerformanceComparisonState(
    val metrics: ImmutableList<PerformanceMetricState>,
    val reference: PerformanceReferenceState,
    val period: DateRangeFormatState,
) {
    fun isOver(range: DateRange?): Boolean = range != null && period.value == range

    @Composable
    fun periodLabel(range: DateRangeFormatState): String? = when {
        isOver(range.value) -> range.periodPhrase()
        else -> period.shortEnd()?.let { AppTokens.strings.res(Res.string.training_recap_period_as_of, it) }
    }
}
