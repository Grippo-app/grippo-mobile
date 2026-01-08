package com.grippo.domain.dto.training

import com.grippo.data.features.api.training.models.SetTraining

public fun SetTraining.toBody(): com.grippo.services.backend.dto.training.TrainingBody {
    return com.grippo.services.backend.dto.training.TrainingBody(
        repetitions = repetitions,
        duration = duration.inWholeMinutes,
        intensity = intensity,
        volume = volume,
        exercises = exercises.toBody()
    )
}