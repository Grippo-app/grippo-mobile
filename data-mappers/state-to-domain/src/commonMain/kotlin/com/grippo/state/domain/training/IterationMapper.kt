package com.grippo.state.domain.training

import com.grippo.core.state.trainings.IterationState
import com.grippo.data.features.api.training.models.SetIteration
import kotlinx.collections.immutable.toPersistentList

public fun List<IterationState>.toDomain(): List<SetIteration> {
    return mapNotNull { it.toDomain() }.toPersistentList()
}

public fun IterationState.toDomain(): SetIteration? {
    return SetIteration(
        volume = volume.value ?: return null,
        repetitions = repetitions.value ?: return null
    )
}