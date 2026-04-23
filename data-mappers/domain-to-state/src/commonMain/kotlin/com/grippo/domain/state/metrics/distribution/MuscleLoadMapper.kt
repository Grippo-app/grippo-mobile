package com.grippo.domain.state.metrics.distribution

import com.grippo.domain.state.muscles.toState
import com.grippo.core.state.metrics.distribution.MuscleLoadDominanceState as StateMuscleLoadDominance
import com.grippo.core.state.metrics.distribution.MuscleLoadMetaState as StateMuscleLoadMeta
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState as StateMuscleLoadSummary
import com.grippo.data.features.api.metrics.distribution.MuscleLoadMeta as DomainMuscleLoadMeta
import com.grippo.data.features.api.metrics.distribution.MuscleLoadSummary as DomainMuscleLoadSummary

public fun DomainMuscleLoadSummary.toState(): StateMuscleLoadSummary {
    return StateMuscleLoadSummary(
        meta = meta.toState(),
        perGroup = perGroup.toState(),
        perMuscle = perMuscle.toState(),
        volumePerGroup = volumePerGroup.toState(),
        volumePerMuscle = volumePerMuscle.toState(),
        dominance = StateMuscleLoadDominance(
            top1SharePercent = dominance.top1SharePercent,
            top2SharePercent = dominance.top2SharePercent,
        ),
        groupDominance = StateMuscleLoadDominance(
            top1SharePercent = groupDominance.top1SharePercent,
            top2SharePercent = groupDominance.top2SharePercent,
        ),
    )
}

private fun DomainMuscleLoadMeta.toState(): StateMuscleLoadMeta {
    return StateMuscleLoadMeta(
        trainingsCount = trainingsCount,
        totalExercises = totalExercises,
        totalSets = totalSets,
        totalRepetitions = totalRepetitions,
        totalVolume = totalVolume,
        dominantGroup = dominantGroup?.toState(),
    )
}
