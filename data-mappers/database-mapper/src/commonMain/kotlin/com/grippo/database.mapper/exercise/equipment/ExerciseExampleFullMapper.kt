package com.grippo.database.mapper.exercise.equipment

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.database.models.ExerciseExamplePack

public fun List<ExerciseExamplePack>.toDomain(): List<ExerciseExample> {
    return map { it.toDomain() }
}

public fun ExerciseExamplePack.toDomain(): ExerciseExample {
    return ExerciseExample(
        value = example.toDomain(),
        tutorials = tutorials.toDomain(),
        bundles = bundles.toDomain(),
        equipments = equipments.toDomain(),
    )
}