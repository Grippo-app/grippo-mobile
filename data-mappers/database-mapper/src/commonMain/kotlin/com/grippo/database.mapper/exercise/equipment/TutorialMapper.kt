package com.grippo.database.mapper.exercise.equipment

import com.grippo.data.features.api.exercise.example.models.ResourceTypeEnum
import com.grippo.data.features.api.exercise.example.models.Tutorial
import com.grippo.database.entity.ExerciseExampleTutorialEntity
import com.grippo.logger.AppLogger

public fun List<ExerciseExampleTutorialEntity>.toDomain(): List<Tutorial> {
    return mapNotNull { it.toDomain() }
}

public fun ExerciseExampleTutorialEntity.toDomain(): Tutorial? {
    val mappedResourceType = AppLogger.checkOrLog(ResourceTypeEnum.of(resourceType)) {
        "Tutorial $id has unrecognized resourceType: $resourceType"
    } ?: return null

    return Tutorial(
        id = id,
        title = title,
        value = value,
        language = language,
        author = author,
        resourceType = mappedResourceType
    )
}