package com.grippo.domain.network.user.training

import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.network.dto.training.TrainingBody

public fun SetTraining.toBody(): TrainingBody {
    return TrainingBody(
        repetitions = repetitions,
        duration = duration.inWholeMinutes,
        intensity = intensity,
        volume = volume,
        exercises = exercises.toBody()
    )
}