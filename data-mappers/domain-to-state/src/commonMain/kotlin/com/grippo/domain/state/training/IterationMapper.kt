package com.grippo.domain.state.training

import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.trainings.IterationState
import com.grippo.data.features.api.training.models.Iteration
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

public fun List<Iteration>.toState(): PersistentList<IterationState> {
    return map { it.toState() }.toPersistentList()
}

public fun Iteration.toState(): IterationState {
    return IterationState(
        id = id,
        externalWeight = VolumeFormatState.of(externalWeight),
        extraWeight = VolumeFormatState.of(extraWeight),
        assistWeight = VolumeFormatState.of(assistWeight),
        bodyWeight = VolumeFormatState.of(bodyWeight),
        repetitions = RepetitionsFormatState.of(repetitions),
    )
}