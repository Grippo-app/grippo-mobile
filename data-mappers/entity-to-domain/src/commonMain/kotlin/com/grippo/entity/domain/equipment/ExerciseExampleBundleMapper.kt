package com.grippo.entity.domain.equipment

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleBundle
import com.grippo.entity.domain.muscles.toDomain
import com.grippo.toolkit.logger.AppLogger

public fun List<com.grippo.services.database.models.ExerciseExampleBundleWithMuscle>.toDomain(): List<ExerciseExampleBundle> {
    return mapNotNull { it.toDomain() }
}

public fun com.grippo.services.database.models.ExerciseExampleBundleWithMuscle.toDomain(): ExerciseExampleBundle? {
    val mappedMuscle = AppLogger.Mapping.log(muscle.toDomain()) {
        "ExerciseExampleBundleWithMuscle ${bundle.id} has invalid muscle (id=${muscle.id})"
    } ?: return null

    return ExerciseExampleBundle(
        id = bundle.id,
        percentage = bundle.percentage,
        muscle = mappedMuscle
    )
}