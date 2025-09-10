package com.grippo.domain.state.exercise.example

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.domain.state.equipment.toState
import com.grippo.state.exercise.examples.ExerciseExampleState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<ExerciseExample>.toState(): ImmutableList<ExerciseExampleState> {
    return map { it.toState() }.toPersistentList()
}

public fun ExerciseExample.toState(): ExerciseExampleState {
    return ExerciseExampleState(
        value = value.toState(),
        bundles = bundles.toState(),
        equipments = equipments.toState(),
    )
}