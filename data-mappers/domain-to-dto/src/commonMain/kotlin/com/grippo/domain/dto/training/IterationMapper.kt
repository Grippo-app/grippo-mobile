package com.grippo.domain.dto.training

import com.grippo.data.features.api.training.models.SetIteration

public fun List<SetIteration>.toBody(): List<com.grippo.services.backend.dto.training.IterationBody> {
    return map { iteration -> iteration.toBody() }
}

public fun SetIteration.toBody(): com.grippo.services.backend.dto.training.IterationBody {
    return com.grippo.services.backend.dto.training.IterationBody(
        repetitions = repetitions,
        weight = volume
    )
}