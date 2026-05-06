package com.grippo.entity.domain.training

import com.grippo.data.features.api.training.models.Exercise
import com.grippo.entity.domain.equipment.toDomain
import com.grippo.services.database.models.ExercisePack
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.toolkit.logger.AppLogger

public fun List<ExercisePack>.toDomain(): List<Exercise> {
    return mapNotNull { it.toDomain() }
}

public fun ExercisePack.toDomain(): Exercise? {
    val mappedExerciseExample = AppLogger.Mapping.log(example?.toDomain()) {
        "ExercisePack exercise by ${exercise.exerciseExampleId} is null"
    } ?: return null

    return Exercise(
        id = exercise.id,
        name = exercise.name,
        iterations = iterations.toDomain(),
        volume = exercise.volume,
        repetitions = exercise.repetitions,
        intensity = exercise.intensity,
        createdAt = DateTimeUtils.toLocalDateTime(exercise.createdAt),
        exerciseExample = mappedExerciseExample
    )
}