package com.grippo.domain.state.training

import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.IterationState
import com.grippo.data.features.api.training.models.SetIteration

public fun SetIteration.toState(id: String): IterationState {
    return IterationState(
        id = id,
        externalWeight = VolumeFormatState.of(externalWeight),
        extraWeight = VolumeFormatState.of(extraWeight),
        assistWeight = VolumeFormatState.of(value = assistWeight, allowZero = true),
        bodyWeight = WeightFormatState.of(bodyWeight),
        bodyMultiplier = MultiplierFormatState.of(bodyMultiplier),
        repetitions = RepetitionsFormatState.of(repetitions),
    )
}
