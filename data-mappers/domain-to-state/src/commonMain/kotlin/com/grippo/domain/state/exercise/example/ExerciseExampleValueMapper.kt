package com.grippo.domain.state.exercise.example

import com.grippo.core.state.examples.ExerciseExampleValueState
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.domain.state.user.toState

public fun ExerciseExampleValue.toState(): ExerciseExampleValueState {
    return ExerciseExampleValueState(
        id = id,
        name = name,
        imageUrl = imageUrl,
        description = description,
        experience = experience.toState(),
        weightType = weightType.toState(),
        forceType = forceType.toState(),
        category = category.toState(),
        usageCount = usageCount,
        lastUsed = lastUsed
    )
}
