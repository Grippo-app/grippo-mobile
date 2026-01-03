package com.grippo.data.features.api.metrics.models

public data class ExerciseDistributionEntry<K>(
    val key: K,
    val value: Float,
)

public data class ExerciseDistribution<K>(
    val entries: List<ExerciseDistributionEntry<K>>,
)

public enum class DistributionWeighting {
    Count,
    Sets,
    Repetitions,
    Volume,
}
