package com.grippo.data.features.api.metrics.distribution.models

public data class MuscleLoadSummary(
    val meta: MuscleLoadMeta,
    val perGroup: MuscleLoadBreakdown,
    val perMuscle: MuscleLoadBreakdown,
    val volumePerGroup: MuscleLoadBreakdown,
    val volumePerMuscle: MuscleLoadBreakdown,
    val dominance: MuscleLoadDominance,
    val groupDominance: MuscleLoadDominance,
)
