package com.grippo.state.domain.training

import com.grippo.core.state.trainings.IterationState
import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.toolkit.logger.AppLogger
import kotlinx.collections.immutable.toPersistentList

public fun List<IterationState>.toDomain(): List<SetIteration> {
    return mapNotNull { it.toDomain() }.toPersistentList()
}

public fun IterationState.toDomain(): SetIteration? {
    val mappedRepetitions = AppLogger.Mapping.log(repetitions.value) {
        "IterationState repetitions value is null (id=$id)"
    } ?: return null

    return SetIteration(
        externalWeight = externalWeight.value,
        extraWeight = extraWeight.value,
        assistWeight = assistWeight.value,
        bodyWeight = bodyWeight.value,
        bodyMultiplier = bodyMultiplier.value,
        repetitions = mappedRepetitions,
    )
}
