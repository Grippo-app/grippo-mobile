package com.grippo.domain.state.training

import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.IterationState
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