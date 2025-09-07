package com.grippo.domain.network.user.training

import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.network.dto.training.ExerciseBody

public fun List<SetExercise>.toBody(): List<ExerciseBody> {
    return map { exercise -> exercise.toBody() }
}

public fun SetExercise.toBody(): ExerciseBody {
    return ExerciseBody(
        repetitions = repetitions,
        intensity = intensity,
        volume = volume,
        exerciseExampleId = exerciseExample?.id,
        iterations = iterations.toBody(),
        name = name
    )
}