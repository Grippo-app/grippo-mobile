package com.grippo.domain.mapper.exercise.example

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.domain.mapper.equipment.toState
import com.grippo.presentation.api.exercise.example.models.ExerciseExampleState

public fun List<ExerciseExample>.toState(): List<ExerciseExampleState> {
    return map { it.toState() }
}

public fun ExerciseExample.toState(): ExerciseExampleState {
    return ExerciseExampleState(
        value = value.toState(),
        bundles = bundles.toState(),
        tutorials = tutorials.toState(),
        equipments = equipments.toState(),
    )
}