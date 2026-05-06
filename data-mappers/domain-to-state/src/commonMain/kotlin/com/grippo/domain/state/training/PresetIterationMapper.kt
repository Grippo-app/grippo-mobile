package com.grippo.domain.state.training

import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.IterationState
import com.grippo.data.features.api.training.models.PresetIteration
import com.grippo.data.features.api.training.models.PresetWeight
import kotlin.uuid.Uuid

public fun PresetIteration.toState(): IterationState {
    val multiplier = when (val w = suggestedWeight) {
        is PresetWeight.BodyOnly -> w.multiplier
        is PresetWeight.BodyAndExtra -> w.multiplier
        is PresetWeight.BodyAndAssist -> w.multiplier
        is PresetWeight.External -> null
    }

    return IterationState(
        id = Uuid.random().toString(),
        externalWeight = VolumeFormatState.Empty(),
        extraWeight = VolumeFormatState.Empty(),
        assistWeight = VolumeFormatState.Empty(),
        bodyWeight = WeightFormatState.Empty(),
        bodyMultiplier = MultiplierFormatState.of(multiplier),
        repetitions = RepetitionsFormatState.of(repetitions),
    )
}