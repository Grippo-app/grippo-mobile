package com.grippo.state.domain.metrics.performance

import com.grippo.core.state.metrics.performance.PerformanceReferenceState
import com.grippo.data.features.api.metrics.performance.models.PerformanceReference

public fun PerformanceReferenceState.toDomain(): PerformanceReference {
    return when (this) {
        PerformanceReferenceState.Average -> PerformanceReference.Average
        PerformanceReferenceState.Previous -> PerformanceReference.Previous
    }
}
