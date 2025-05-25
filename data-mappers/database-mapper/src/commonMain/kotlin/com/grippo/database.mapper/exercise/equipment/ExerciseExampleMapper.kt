package com.grippo.database.mapper.exercise.equipment

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.database.entity.ExerciseExampleEntity
import com.grippo.logger.AppLogger

public fun ExerciseExampleEntity.toDomain(): ExerciseExampleValue? {
    val mappedWeightType = AppLogger.checkOrLog(WeightTypeEnum.of(weightType)) {
        "ExerciseExampleEntity $id has unrecognized weightType: $weightType"
    } ?: return null

    return ExerciseExampleValue(
        id = id,
        name = name,
        description = description,
        imageUrl = imageUrl,
        experience = ExperienceEnum.of(experience),
        forceType = ForceTypeEnum.of(forceType),
        weightType = mappedWeightType,
        category = CategoryEnum.of(category)
    )
}
