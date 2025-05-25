package com.grippo.domain.mapper.exercise.example

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.domain.mapper.equipment.toState
import com.grippo.domain.mapper.user.toState
import com.grippo.logger.AppLogger
import com.grippo.presentation.api.exercise.example.models.ExerciseExampleState

public fun List<ExerciseExample>.toState(): List<ExerciseExampleState> {
    return mapNotNull { it.toState() }
}

public fun ExerciseExample.toState(): ExerciseExampleState? {
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

    return ExerciseExampleState(
        id = id,
        name = name,
        imageUrl = imageUrl,
        description = description,
        bundles = bundles.toState(),
        tutorials = tutorials.toState(),
        equipments = equipments.toState(),
        experience = mappedExperience,
        weightType = mappedWeightType,
        forceType = mappedForceType,
        category = mappedCategory
    )
}