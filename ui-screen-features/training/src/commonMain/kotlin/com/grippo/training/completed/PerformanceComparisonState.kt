package com.grippo.training.completed

import androidx.compose.runtime.Immutable
import com.grippo.core.state.metrics.performance.PerformanceBaselineState
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PerformanceComparisonState(
    val metrics: ImmutableList<PerformanceMetricState>,
    val baseline: PerformanceBaselineState,
)
