package com.grippo.domain.mapper.exercise.example

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.domain.mapper.equipment.toState
import com.grippo.logger.AppLogger
import com.grippo.presentation.api.exercise.example.models.ExerciseExampleState

public fun List<ExerciseExample>.toState(): List<ExerciseExampleState> {
    return mapNotNull { it.toState() }
}

public fun ExerciseExample.toState(): ExerciseExampleState? {
    val mappedValue = AppLogger.checkOrLog(value.toState()) {
        "ExerciseExample ${value.id} has an unrecognized value: $value"
    } ?: return null

    return ExerciseExampleState(
        value = mappedValue,
        bundles = bundles.toState(),
        tutorials = tutorials.toState(),
        equipments = equipments.toState(),
    )
}