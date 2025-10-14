package com.grippo.domain.state.training

import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.trainings.IterationState
import com.grippo.data.features.api.training.models.SetIteration
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlin.uuid.Uuid

public fun List<SetIteration>.toState(): PersistentList<IterationState> {
    return map { it.toState() }.toPersistentList()
}

public fun SetIteration.toState(): IterationState {
    return IterationState(
        id = Uuid.random().toString(),
        volume = VolumeFormatState.of(volume),
        repetitions = RepetitionsFormatState.of(repetitions)
    )
}