package com.grippo.database.mapper.exercise.equipment

import com.grippo.data.features.api.exercise.example.models.ExerciseExampleBundle
import com.grippo.database.mapper.muscles.toDomain
import com.grippo.database.models.ExerciseExampleBundleWithMuscle

public fun List<ExerciseExampleBundleWithMuscle>.toDomain(): List<ExerciseExampleBundle> {
    return map { it.toDomain() }
}

public fun ExerciseExampleBundleWithMuscle.toDomain(): ExerciseExampleBundle {
    return ExerciseExampleBundle(
        id = bundle.id,
        percentage = bundle.percentage,
        muscle = muscle.toDomain()
    )
}