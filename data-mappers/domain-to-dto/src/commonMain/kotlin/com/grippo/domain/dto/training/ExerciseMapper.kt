package com.grippo.domain.dto.training

import com.grippo.backend.dto.training.ExerciseBody
import com.grippo.data.features.api.training.models.SetExercise

public fun List<SetExercise>.toBody(): List<ExerciseBody> {
    return map { exercise -> exercise.toBody() }
}

public fun SetExercise.toBody(): ExerciseBody {
    return ExerciseBody(
        repetitions = repetitions,
        intensity = intensity,
        volume = volume,
        exerciseExampleId = exerciseExample.id,
        iterations = iterations.toBody(),
        name = name
    )
}