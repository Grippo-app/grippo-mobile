package com.grippo.network.mapper

import com.grippo.database.entity.ExerciseExampleTutorialEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.TutorialDto

public fun TutorialDto.toEntityOrNull(): ExerciseExampleTutorialEntity? {
    val entityId = AppLogger.mapping(id, { "TutorialDto.id is null" }) ?: return null
    val entityExerciseExampleId =
        AppLogger.mapping(exerciseExampleId, { "TutorialDto.exerciseExampleId is null" })
            ?: return null
    val entityTitle = AppLogger.mapping(title, { "TutorialDto.title is null" }) ?: return null
    val entityLanguage =
        AppLogger.mapping(language, { "TutorialDto.language is null" }) ?: return null
    val entityValue = AppLogger.mapping(value, { "TutorialDto.value is null" }) ?: return null
    val entityResourceType =
        AppLogger.mapping(resourceType, { "TutorialDto.resourceType is null" }) ?: return null
    val entityCreatedAt =
        AppLogger.mapping(createdAt, { "TutorialDto.createdAt is null" }) ?: return null
    val entityUpdatedAt =
        AppLogger.mapping(updatedAt, { "TutorialDto.updatedAt is null" }) ?: return null

    return ExerciseExampleTutorialEntity(
        id = entityId,
        exerciseExampleId = entityExerciseExampleId,
        title = entityTitle,
        language = entityLanguage,
        author = author,
        value = entityValue,
        resourceType = entityResourceType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}