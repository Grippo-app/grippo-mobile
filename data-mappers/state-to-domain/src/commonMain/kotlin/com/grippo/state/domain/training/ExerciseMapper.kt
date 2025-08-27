package com.grippo.state.domain.training

import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.state.trainings.ExerciseState
import kotlinx.collections.immutable.toPersistentList

public fun List<ExerciseState>.toDomain(): List<SetExercise> {
    return mapNotNull { it.toDomain() }.toPersistentList()
}

public fun ExerciseState.toDomain(): SetExercise? {
    return SetExercise(
        name = name,
        iterations = iterations.toDomain(),
        exerciseExampleId = exerciseExample?.id,
        repetitions = metrics.repetitions.value ?: return null,
        volume = metrics.volume.value ?: return null,
        intensity = metrics.intensity.value ?: return null
    )
}