package com.grippo.domain.dto.training

import com.grippo.backend.dto.training.TrainingBody
import com.grippo.data.features.api.training.models.SetTraining

public fun SetTraining.toBody(): TrainingBody {
    return TrainingBody(
        repetitions = repetitions,
        duration = duration.inWholeMinutes,
        intensity = intensity,
        volume = volume,
        exercises = exercises.toBody()
    )
}