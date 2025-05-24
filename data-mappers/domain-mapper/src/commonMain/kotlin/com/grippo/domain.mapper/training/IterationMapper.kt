package com.grippo.domain.mapper.training

import com.grippo.data.features.api.training.models.Iteration
import com.grippo.presentation.api.trainings.models.IterationState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

public fun List<Iteration>.toState(): PersistentList<IterationState> {
    return mapNotNull { it.toState() }.toPersistentList()
}

public fun Iteration.toState(): IterationState {
    return IterationState(
        id = id,
        weight = weight,
        repetitions = repetitions
    )
}