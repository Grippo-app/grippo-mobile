package com.grippo.domain.state.metrics.distribution

import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.core.state.metrics.distribution.ExerciseDistributionEntryState
import com.grippo.core.state.metrics.distribution.ExerciseDistributionState
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.metrics.distribution.ExerciseDistribution
import com.grippo.domain.state.exercise.example.toState
import kotlinx.collections.immutable.toPersistentList

public fun ExerciseDistribution<ForceTypeEnum>.toState(): ExerciseDistributionState<ForceTypeEnumState> {
    val entries = entries.mapNotNull { entry ->
        val value = entry.value
        if (value <= 0f) {
            null
        } else {
            ExerciseDistributionEntryState(
                key = entry.key.toState(),
                value = value
            )
        }
    }.toPersistentList()

    return ExerciseDistributionState(entries = entries)
}
