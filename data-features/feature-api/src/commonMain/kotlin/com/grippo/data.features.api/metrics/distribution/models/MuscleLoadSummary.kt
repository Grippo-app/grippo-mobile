package com.grippo.data.features.api.metrics.distribution.models

public data class MuscleLoadSummary(
    val meta: MuscleLoadMeta,

    val perGroup: MuscleLoadBreakdown,
    val perMuscle: MuscleLoadBreakdown,

    /**
     * Same breakdowns, but computed using volume (tonnage = weight * reps).
     */
    val volumePerGroup: MuscleLoadBreakdown,
    val volumePerMuscle: MuscleLoadBreakdown,

    /**
     * Dominance indicators from stimulus-based per-muscle load.
     */
    val dominance: MuscleLoadDominance,

    /**
     * Dominance indicators from stimulus-based per-group load.
     */
    val groupDominance: MuscleLoadDominance,
)
