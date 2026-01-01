package com.grippo.domain.state.metrics

import com.grippo.core.state.examples.CategoryEnumState
import com.grippo.core.state.metrics.ExerciseDistributionEntryState
import com.grippo.core.state.metrics.ExerciseDistributionState
import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.metrics.models.ExerciseDistribution
import com.grippo.domain.state.exercise.example.toState
import kotlinx.collections.immutable.toPersistentList

public fun ExerciseDistribution<CategoryEnum>.toCategoryDistributionState(): ExerciseDistributionState<CategoryEnumState> {
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
