package com.grippo.database.mapper.exercise.equipment

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.database.models.ExerciseExampleFull

public fun ExerciseExampleFull.toDomain(): ExerciseExample {
    return ExerciseExample(
        id = example.id,
        name = example.name,
        description = example.description,
        imageUrl = example.imageUrl,
        experience = ExperienceEnum.of(example.experience),
        forceType = ForceTypeEnum.of(example.forceType),
        weightType = WeightTypeEnum.of(example.weightType),
        category = CategoryEnum.of(example.category),
        tutorials = tutorials.toDomain(),
        bundles = bundles.toDomain(),
        equipments = equipments.toDomain(),
    )
}