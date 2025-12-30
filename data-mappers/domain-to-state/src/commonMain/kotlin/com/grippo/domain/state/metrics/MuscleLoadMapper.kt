package com.grippo.domain.state.metrics

import com.grippo.core.state.metrics.MuscleLoadSummary as StateMuscleLoadSummary
import com.grippo.data.features.api.metrics.models.MuscleLoadSummary as DomainMuscleLoadSummary

public fun DomainMuscleLoadSummary.toState(): StateMuscleLoadSummary {
    return StateMuscleLoadSummary(
        perGroup = perGroup.toState(),
        perMuscle = perMuscle.toState(),
    )
}