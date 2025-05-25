package com.grippo.database.mapper.exercise.equipment

import com.grippo.data.features.api.exercise.example.models.ResourceTypeEnum
import com.grippo.data.features.api.exercise.example.models.Tutorial
import com.grippo.database.entity.ExerciseExampleTutorialEntity

public fun List<ExerciseExampleTutorialEntity>.toDomain(): List<Tutorial> {
    return map { it.toDomain() }
}

public fun ExerciseExampleTutorialEntity.toDomain(): Tutorial {
    return Tutorial(
        id = id,
        title = title,
        value = value,
        language = language,
        author = author,
        resourceType = ResourceTypeEnum.of(resourceType)
    )
}