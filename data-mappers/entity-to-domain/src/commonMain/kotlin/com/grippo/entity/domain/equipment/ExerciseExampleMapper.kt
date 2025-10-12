package com.grippo.entity.domain.equipment

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.database.entity.ExerciseExampleEntity
import com.grippo.date.utils.DateTimeUtils
import com.grippo.logger.AppLogger

public fun ExerciseExampleEntity.toDomain(): ExerciseExampleValue? {
    val mappedWeightType = AppLogger.Mapping.log(WeightTypeEnum.of(weightType)) {
        "ExerciseExampleEntity $id has unrecognized weightType: $weightType"
    } ?: return null

    val mappedExperience = AppLogger.Mapping.log(ExperienceEnum.of(experience)) {
        "ExerciseExampleEntity $id has unrecognized experience: $experience"
    } ?: return null

    val mappedForceType = AppLogger.Mapping.log(ForceTypeEnum.of(forceType)) {
        "ExerciseExampleEntity $id has unrecognized forceType: $forceType"
    } ?: return null

    val mappedCategory = AppLogger.Mapping.log(CategoryEnum.of(category)) {
        "ExerciseExampleEntity $id has unrecognized category: $category"
    } ?: return null

    return ExerciseExampleValue(
        id = id,
        name = name,
        description = description,
        imageUrl = imageUrl,
        experience = mappedExperience,
        forceType = mappedForceType,
        weightType = mappedWeightType,
        category = mappedCategory,
        usageCount = usageCount,
        lastUsed = lastUsed?.let { DateTimeUtils.toLocalDateTime(it) }
    )
}