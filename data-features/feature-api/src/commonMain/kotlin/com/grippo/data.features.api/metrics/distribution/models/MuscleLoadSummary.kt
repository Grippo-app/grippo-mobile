package com.grippo.data.features.api.metrics.distribution.models

public data class MuscleLoadSummary(
    val meta: MuscleLoadMeta,

    /** Positive anatomical muscle shares × performed set counts, normalized to percentages. */
    val perGroup: MuscleLoadBreakdown,
    val perMuscle: MuscleLoadBreakdown,

    /**
     * Same linear anatomy shares applied to fully measured recorded resistance × repetitions.
     * Weighted-set statistics and top examples retain their performed-set interpretation;
     * volume statistics include measured work only.
     */
    val volumePerGroup: MuscleLoadBreakdown,
    val volumePerMuscle: MuscleLoadBreakdown,

    /**
     * Dominance indicators from estimated weighted sets per muscle.
     */
    val dominance: MuscleLoadDominance,

    /**
     * Dominance indicators from estimated weighted sets per group.
     */
    val groupDominance: MuscleLoadDominance,
)
