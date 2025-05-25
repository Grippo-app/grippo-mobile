package com.grippo.database.mapper.training

import com.grippo.data.features.api.training.models.Exercise
import com.grippo.database.mapper.exercise.equipment.toDomain
import com.grippo.database.models.ExerciseFull

public fun List<ExerciseFull>.toDomain(): List<Exercise> {
    return map { it.toDomain() }
}

public fun ExerciseFull.toDomain(): Exercise {
    return Exercise(
        id = exercise.id,
        name = exercise.name,
        iterations = iterations.toDomain(),
        volume = exercise.volume,
        repetitions = exercise.repetitions,
        intensity = exercise.intensity,
        exerciseExample = example?.toDomain()
    )
}