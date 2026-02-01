package com.grippo.entity.domain.training

import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.SetExercise
import com.grippo.entity.domain.equipment.toDomain
import com.grippo.services.database.models.DraftExercisePack
import com.grippo.services.database.models.ExercisePack
import com.grippo.toolkit.date.utils.DateTimeUtils

public fun List<ExercisePack>.toDomain(): List<Exercise> {
    return mapNotNull { it.toDomain() }
}

public fun ExercisePack.toDomain(): Exercise? {
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

public fun List<DraftExercisePack>.toSetDomain(): List<SetExercise> {
    return mapNotNull { it.toSetDomain() }
}

public fun DraftExercisePack.toSetDomain(): SetExercise? {
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
