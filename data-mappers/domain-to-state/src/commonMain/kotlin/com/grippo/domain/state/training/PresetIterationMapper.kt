package com.grippo.domain.state.training

import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.IterationState
import com.grippo.data.features.api.training.models.PresetIteration
import kotlin.uuid.Uuid

public fun PresetIteration.toState(): IterationState {
    return IterationState(
        id = Uuid.random().toString(),
        externalWeight = VolumeFormatState.Empty(),
        extraWeight = VolumeFormatState.Empty(),
        assistWeight = VolumeFormatState.Empty(),
        bodyWeight = WeightFormatState.Empty(),
        bodyMultiplier = MultiplierFormatState.of(bodyMultiplier),
        repetitions = RepetitionsFormatState.of(repetitions),
    )
}
