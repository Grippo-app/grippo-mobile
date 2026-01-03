package com.grippo.state.domain.training

import com.grippo.core.state.trainings.ExerciseState
import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.state.domain.example.toDomain
import kotlinx.collections.immutable.toPersistentList

public fun List<ExerciseState>.toDomain(): List<SetExercise> {
    return mapNotNull { it.toDomain() }.toPersistentList()
}

public fun ExerciseState.toDomain(): SetExercise? {
    return SetExercise(
        name = name,
        iterations = iterations.toDomain(),
        exerciseExample = exerciseExample.toDomain() ?: return null,
        repetitions = total.repetitions.value ?: return null,
        volume = total.volume.value ?: return null,
        intensity = total.intensity.value ?: return null,
        createdAt = createdAt
    )
}