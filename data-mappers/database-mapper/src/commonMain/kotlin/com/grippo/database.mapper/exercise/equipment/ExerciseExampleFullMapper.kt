package com.grippo.database.mapper.exercise.equipment

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.database.models.ExerciseExamplePack
import com.grippo.logger.AppLogger

public fun List<ExerciseExamplePack>.toDomain(): List<ExerciseExample> {
    return mapNotNull { it.toDomain() }
}

public fun ExerciseExamplePack.toDomain(): ExerciseExample? {
    val mappedValue = AppLogger.Mapping.log(example.toDomain()) {
        "ExerciseExamplePack has invalid example and cannot be mapped"
    } ?: return null

    return ExerciseExample(
        value = mappedValue,
        tutorials = tutorials.toDomain(),
        bundles = bundles.toDomain(),
        equipments = equipments.toDomain(),
    )
}