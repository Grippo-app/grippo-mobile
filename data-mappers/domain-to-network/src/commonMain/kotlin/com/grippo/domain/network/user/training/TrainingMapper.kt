package com.grippo.domain.network.user.training

import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.network.dto.training.TrainingBody

public fun SetTraining.toBody(): TrainingBody {
    return TrainingBody(
        repetitions = 0,
        duration = 0L,
        intensity = 0F,
        volume = 0F,
        exercises = exercises.toBody()
    )
}