package com.grippo.domain.state.training

import com.grippo.data.features.api.training.models.Exercise
import com.grippo.domain.state.exercise.example.toState
import com.grippo.state.trainings.ExerciseState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

public fun List<Exercise>.toState(): PersistentList<ExerciseState> {
    return map { it.toState() }.toPersistentList()
}

public fun Exercise.toState(): ExerciseState {
    return ExerciseState(
        id = id,
        name = name,
        iterations = iterations.toState(),
        volume = volume,
        repetitions = repetitions,
        intensity = intensity,
        exerciseExample = exerciseExample?.toState()
    )
}