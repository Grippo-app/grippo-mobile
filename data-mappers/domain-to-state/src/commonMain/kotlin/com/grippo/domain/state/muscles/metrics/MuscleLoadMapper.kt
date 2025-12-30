package com.grippo.domain.state.muscles.metrics

import com.grippo.core.state.muscles.metrics.MuscleLoadSummary as StateMuscleLoadSummary
import com.grippo.data.features.api.muscle.models.MuscleLoadSummary as DomainMuscleLoadSummary

public fun DomainMuscleLoadSummary.toState(): StateMuscleLoadSummary {
    return StateMuscleLoadSummary(
        perGroup = perGroup.toState(),
        perMuscle = perMuscle.toState(),
    )
}