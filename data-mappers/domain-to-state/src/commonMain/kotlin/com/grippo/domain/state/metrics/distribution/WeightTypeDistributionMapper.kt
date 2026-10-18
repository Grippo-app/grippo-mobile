package com.grippo.domain.state.metrics.distribution

import com.grippo.core.state.examples.WeightTypeEnumState
import com.grippo.core.state.metrics.distribution.ExerciseDistributionEntryState
import com.grippo.core.state.metrics.distribution.ExerciseDistributionState
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.data.features.api.metrics.distribution.models.ExerciseDistribution
import com.grippo.domain.state.exercise.example.toState
import kotlinx.collections.immutable.toPersistentList

public fun ExerciseDistribution<WeightTypeEnum>.toState(): ExerciseDistributionState<WeightTypeEnumState> {
    val entries = entries.map { entry ->
        ExerciseDistributionEntryState(
            key = entry.key.toState(),
            value = entry.value,
        )
    }.toPersistentList()

    return ExerciseDistributionState(
        weighting = weighting.toState(),
        entries = entries,
        excludedVolumeSetCount = excludedVolumeSetCount,
    )
}
