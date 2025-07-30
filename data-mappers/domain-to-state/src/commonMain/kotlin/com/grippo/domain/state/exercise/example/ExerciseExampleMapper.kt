package com.grippo.domain.state.exercise.example

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.domain.state.equipment.toState
import com.grippo.state.exercise.examples.ExerciseExampleState

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