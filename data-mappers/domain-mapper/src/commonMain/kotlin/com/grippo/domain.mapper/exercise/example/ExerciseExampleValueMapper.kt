package com.grippo.domain.mapper.exercise.example

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.domain.mapper.user.toState
import com.grippo.logger.AppLogger
import com.grippo.presentation.api.exercise.example.models.ExerciseExampleValueState

public fun ExerciseExampleValue.toState(): ExerciseExampleValueState? {
    val mappedCategory = AppLogger.checkOrLog(category.toState()) {
        "ExerciseExample $id has an unrecognized category: $category"
    } ?: return null

    return ExerciseExampleValueState(
        id = id,
        name = name,
        imageUrl = imageUrl,
        description = description,
        experience = experience.toState(),
        weightType = weightType.toState(),
        forceType = forceType.toState(),
        category = mappedCategory
    )
}