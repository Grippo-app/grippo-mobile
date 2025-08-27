package com.grippo.domain.network.user.training

import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.network.dto.training.IterationBody

public fun List<SetIteration>.toBody(): List<IterationBody> {
    return map { iteration -> iteration.toBody() }
}

public fun SetIteration.toBody(): IterationBody {
    return IterationBody(
        repetitions = repetitions,
        weight = volume
    )
}