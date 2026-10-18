package com.grippo.domain.state.metrics.distribution

import com.grippo.core.state.metrics.distribution.DistributionWeightingState
import com.grippo.data.features.api.metrics.distribution.models.DistributionWeighting

public fun DistributionWeighting.toState(): DistributionWeightingState {
    return when (this) {
        DistributionWeighting.Count -> DistributionWeightingState.Count
        DistributionWeighting.Sets -> DistributionWeightingState.Sets
        DistributionWeighting.Repetitions -> DistributionWeightingState.Repetitions
        DistributionWeighting.Volume -> DistributionWeightingState.Volume
    }
}
