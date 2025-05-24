package com.grippo.domain.mapper.training

import com.grippo.data.features.api.training.models.Exercise
import com.grippo.presentation.api.trainings.models.ExerciseState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

public fun List<Exercise>.toState(): PersistentList<ExerciseState> {
    return mapNotNull { it.toState() }.toPersistentList()
}

public fun Exercise.toState(): ExerciseState {
    return ExerciseState(
        id = id,
        name = name,
        iterations = iterations.toState(),
        volume = volume,
        repetitions = repetitions,
        intensity = intensity
    )
}