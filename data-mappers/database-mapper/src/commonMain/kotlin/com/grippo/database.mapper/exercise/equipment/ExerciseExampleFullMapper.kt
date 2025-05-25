package com.grippo.database.mapper.exercise.equipment

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.database.models.ExerciseExampleFull

public fun List<ExerciseExampleFull>.toDomain(): List<ExerciseExample> {
    return map { it.toDomain() }
}

public fun ExerciseExampleFull.toDomain(): ExerciseExample {
    return ExerciseExample(
        value = example.toDomain(),
        tutorials = tutorials.toDomain(),
        bundles = bundles.toDomain(),
        equipments = equipments.toDomain(),
    )
}