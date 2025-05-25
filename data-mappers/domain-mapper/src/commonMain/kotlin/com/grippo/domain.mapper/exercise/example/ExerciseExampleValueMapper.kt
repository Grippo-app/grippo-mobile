package com.grippo.domain.mapper.exercise.example

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.domain.mapper.user.toState
import com.grippo.logger.AppLogger
import com.grippo.presentation.api.exercise.example.models.ExerciseExampleValueState

public fun ExerciseExampleValue.toState(): ExerciseExampleValueState? {
    val mappedCategory = AppLogger.checkOrLog(category.toState()) {
        "ExerciseExample $id has an unrecognized category: $category"
    } ?: return null

    val mappedForceType = AppLogger.checkOrLog(forceType.toState()) {
        "ExerciseExample $id has an unrecognized forceType: $forceType"
    } ?: return null

    val mappedWeightType = AppLogger.checkOrLog(weightType.toState()) {
        "ExerciseExample $id has an unrecognized weightType: $weightType"
    } ?: return null

    val mappedExperience = AppLogger.checkOrLog(experience.toState()) {
        "ExerciseExample $id has an unrecognized experience: $experience"
    } ?: return null

    return ExerciseExampleValueState(
        id = id,
        name = name,
        imageUrl = imageUrl,
        description = description,
        experience = mappedExperience,
        weightType = mappedWeightType,
        forceType = mappedForceType,
        category = mappedCategory
    )
}