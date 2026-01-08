package com.grippo.entity.domain.equipment

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.toolkit.logger.AppLogger

public fun List<com.grippo.services.database.models.ExerciseExamplePack>.toDomain(): List<ExerciseExample> {
    return mapNotNull { it.toDomain() }
}

public fun com.grippo.services.database.models.ExerciseExamplePack.toDomain(): ExerciseExample? {
    val mappedValue = AppLogger.Mapping.log(example.toDomain()) {
        "ExerciseExamplePack has invalid example and cannot be mapped"
    } ?: return null

    return ExerciseExample(
        value = mappedValue,
        bundles = bundles.toDomain(),
        equipments = equipments.toDomain(),
    )
}