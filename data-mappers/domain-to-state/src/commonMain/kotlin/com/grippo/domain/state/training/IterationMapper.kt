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
        external = VolumeFormatState.of(volume),
        extra = VolumeFormatState.of(0f),
        assist = VolumeFormatState.of(0f),
        body = VolumeFormatState.of(0f),
        repetitions = RepetitionsFormatState.of(repetitions),
    )
}