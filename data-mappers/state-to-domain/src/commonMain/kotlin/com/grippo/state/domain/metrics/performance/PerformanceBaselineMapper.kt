package com.grippo.state.domain.metrics.performance

import com.grippo.core.state.metrics.performance.PerformanceBaselineState
import com.grippo.data.features.api.metrics.performance.models.PerformanceBaseline

public fun PerformanceBaselineState.toDomain(): PerformanceBaseline {
    return when (this) {
        PerformanceBaselineState.Average -> PerformanceBaseline.Average
        PerformanceBaselineState.Previous -> PerformanceBaseline.Previous
    }
}
