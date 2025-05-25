package com.grippo.database.mapper.exercise.equipment

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.database.entity.ExerciseExampleEntity

public fun ExerciseExampleEntity.toDomain(): ExerciseExampleValue {
    return ExerciseExampleValue(
        id = id,
        name = name,
        description = description,
        imageUrl = imageUrl,
        experience = ExperienceEnum.of(experience),
        forceType = ForceTypeEnum.of(forceType),
        weightType = WeightTypeEnum.of(weightType),
        category = CategoryEnum.of(category)
    )
}