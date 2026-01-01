package com.grippo.domain.state.metrics

import com.grippo.core.state.examples.ForceTypeEnumState
import com.grippo.core.state.metrics.ExerciseDistributionEntryState
import com.grippo.core.state.metrics.ExerciseDistributionState
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.metrics.models.ExerciseDistribution
import com.grippo.domain.state.exercise.example.toState
import kotlinx.collections.immutable.toPersistentList

public fun ExerciseDistribution<ForceTypeEnum>.toForceTypeDistributionState(): ExerciseDistributionState<ForceTypeEnumState> {
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
