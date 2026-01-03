package com.grippo.data.features.api.metrics.models

import com.grippo.data.features.api.muscle.models.MuscleEnum

public data class MuscleLoadSummary(
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
)

public data class MuscleLoadBreakdown(
    val entries: List<MuscleLoadEntry>,
)

public data class MuscleLoadEntry(
    val label: String,
    val percentage: Float,
    val muscles: List<MuscleEnum>,
)

public data class MuscleLoadDominance(
    /**
     * Share of the top-1 muscle in total stimulus load (0..100).
     */
    val top1SharePercent: Float,
    /**
     * Share of the top-2 muscles together in total stimulus load (0..100).
     */
    val top2SharePercent: Float,
)
