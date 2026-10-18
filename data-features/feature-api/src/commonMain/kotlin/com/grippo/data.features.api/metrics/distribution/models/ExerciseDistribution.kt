package com.grippo.data.features.api.metrics.distribution.models

/** Raw totals grouped in the corresponding enum's declaration order. */
public data class ExerciseDistribution<K>(
    val weighting: DistributionWeighting,
    val entries: List<ExerciseDistributionEntry<K>>,
    /**
     * Positive-repetition sets excluded from [DistributionWeighting.Volume] because their
     * components were incomplete, nonfinite, negative, or produced a nonpositive effective load.
     * Exclusion does not establish that a recorded set is invalid. Zero for other weightings.
     */
    val excludedVolumeSetCount: Long,
)
