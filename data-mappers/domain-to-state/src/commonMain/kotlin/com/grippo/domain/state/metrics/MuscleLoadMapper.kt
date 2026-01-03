package com.grippo.domain.state.metrics

import com.grippo.core.state.metrics.MuscleLoadDominanceState as StateMuscleLoadDominance
import com.grippo.core.state.metrics.MuscleLoadSummaryState as StateMuscleLoadSummary
import com.grippo.data.features.api.metrics.models.MuscleLoadSummary as DomainMuscleLoadSummary

public fun DomainMuscleLoadSummary.toState(): StateMuscleLoadSummary {
    return StateMuscleLoadSummary(
        perGroup = perGroup.toState(),
        perMuscle = perMuscle.toState(),
        volumePerGroup = volumePerGroup.toState(),
        volumePerMuscle = volumePerMuscle.toState(),
        dominance = StateMuscleLoadDominance(
            top1SharePercent = dominance.top1SharePercent,
            top2SharePercent = dominance.top2SharePercent,
        ),
    )
}