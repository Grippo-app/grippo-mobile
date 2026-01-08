package com.grippo.domain.dto.training

import com.grippo.data.features.api.training.models.SetExercise

public fun List<SetExercise>.toBody(): List<com.grippo.services.backend.dto.training.ExerciseBody> {
    return map { exercise -> exercise.toBody() }
}

public fun SetExercise.toBody(): com.grippo.services.backend.dto.training.ExerciseBody {
    return com.grippo.services.backend.dto.training.ExerciseBody(
        repetitions = repetitions,
        intensity = intensity,
        volume = volume,
        exerciseExampleId = exerciseExample.id,
        iterations = iterations.toBody(),
        name = name
    )
}