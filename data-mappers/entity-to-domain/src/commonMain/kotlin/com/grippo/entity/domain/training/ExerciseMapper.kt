package com.grippo.entity.domain.training

import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.entity.domain.equipment.toDomain
import com.grippo.toolkit.date.utils.DateTimeUtils

public fun List<com.grippo.services.database.models.ExercisePack>.toDomain(): List<Exercise> {
    return mapNotNull { it.toDomain() }
}

public fun com.grippo.services.database.models.ExercisePack.toDomain(): Exercise? {
    return Exercise(
        id = exercise.id,
        name = exercise.name,
        iterations = iterations.toDomain(),
        volume = exercise.volume,
        repetitions = exercise.repetitions,
        intensity = exercise.intensity,
        createdAt = DateTimeUtils.toLocalDateTime(exercise.createdAt),
        exerciseExample = example?.toDomain() ?: return null
    )
}

public fun List<com.grippo.services.database.models.DraftExercisePack>.toSetDomain(): List<SetExercise> {
    return mapNotNull { it.toSetDomain() }
}

public fun com.grippo.services.database.models.DraftExercisePack.toSetDomain(): SetExercise? {
    return SetExercise(
        name = exercise.name,
        iterations = iterations.toSetDomain(),
        volume = exercise.volume,
        repetitions = exercise.repetitions,
        intensity = exercise.intensity,
        createdAt = DateTimeUtils.toLocalDateTime(exercise.createdAt),
        exerciseExample = example?.toDomain() ?: return null
    )
}