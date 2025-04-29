package com.grippo.network.mapper

import com.grippo.database.entity.ExerciseTutorialEntity
import com.grippo.network.dto.TutorialDto

public fun TutorialDto.toEntityOrNull(): ExerciseTutorialEntity? {
    return ExerciseTutorialEntity(
        id = id ?: return null,
        exerciseExampleId = exerciseExampleId ?: return null,
        title = title ?: return null,
        language = language ?: return null,
        author = author,
        value = value ?: return null,
        resourceType = resourceType ?: return null,
        createdAt = createdAt ?: return null,
        updatedAt = updatedAt ?: return null,
    )
}