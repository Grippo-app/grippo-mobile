package com.grippo.data.features.api.metrics.distribution.models

public data class ExerciseDistributionEntry<K>(
    val key: K,
    /** Raw quantity in the distribution's weighting unit, not a fraction or percentage. */
    val value: Double,
)
