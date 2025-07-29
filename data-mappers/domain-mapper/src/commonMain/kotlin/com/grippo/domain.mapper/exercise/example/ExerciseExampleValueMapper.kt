package com.grippo.domain.mapper.exercise.example

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.domain.mapper.user.toState
import com.grippo.state.exercise.examples.ExerciseExampleValueState

public fun ExerciseExampleValue.toState(): ExerciseExampleValueState {
    return ExerciseExampleValueState(
        id = id,
        name = name,
        imageUrl = imageUrl,
        description = description,
        experience = experience.toState(),
        weightType = weightType.toState(),
        forceType = forceType.toState(),
        category = category.toState()
    )
}