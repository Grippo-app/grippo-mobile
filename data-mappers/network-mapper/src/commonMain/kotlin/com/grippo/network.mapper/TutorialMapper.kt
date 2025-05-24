package com.grippo.network.mapper

import com.grippo.database.entity.ExerciseExampleTutorialEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.TutorialResponse

public fun List<TutorialResponse>.toEntities(): List<ExerciseExampleTutorialEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun TutorialResponse.toEntityOrNull(): ExerciseExampleTutorialEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "TutorialDto.id is null"
    } ?: return null

    val entityExerciseExampleId = AppLogger.checkOrLog(exerciseExampleId) {
        "TutorialDto.exerciseExampleId is null"
    } ?: return null

    val entityTitle = AppLogger.checkOrLog(title) {
        "TutorialDto.title is null"
    } ?: return null

    val entityLanguage = AppLogger.checkOrLog(language) {
        "TutorialDto.language is null"
    } ?: return null

    val entityValue = AppLogger.checkOrLog(value) {
        "TutorialDto.value is null"
    } ?: return null

    val entityResourceType = AppLogger.checkOrLog(resourceType) {
        "TutorialDto.resourceType is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "TutorialDto.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "TutorialDto.updatedAt is null"
    } ?: return null

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