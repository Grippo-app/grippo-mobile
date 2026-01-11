package com.grippo.domain.dto.training

import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.services.backend.dto.training.TrainingBody

public fun SetTraining.toBody(): TrainingBody {
    return TrainingBody(
        repetitions = repetitions,
        duration = duration.inWholeMinutes,
        intensity = intensity,
        volume = volume,
        exercises = exercises.toBody()
    )
}