package com.grippo.network.mapper.exercise.example

import com.grippo.database.entity.ExerciseExampleTutorialEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.TutorialResponse

public fun List<TutorialResponse>.toEntities(): List<ExerciseExampleTutorialEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun TutorialResponse.toEntityOrNull(): ExerciseExampleTutorialEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "TutorialResponse.id is null"
    } ?: return null

    val entityExerciseExampleId = AppLogger.checkOrLog(exerciseExampleId) {
        "TutorialResponse.exerciseExampleId is null"
    } ?: return null

    val entityTitle = AppLogger.checkOrLog(title) {
        "TutorialResponse.title is null"
    } ?: return null

    val entityLanguage = AppLogger.checkOrLog(language) {
        "TutorialResponse.language is null"
    } ?: return null

    val entityValue = AppLogger.checkOrLog(value) {
        "TutorialResponse.value is null"
    } ?: return null

    val entityResourceType = AppLogger.checkOrLog(resourceType) {
        "TutorialResponse.resourceType is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "TutorialResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "TutorialResponse.updatedAt is null"
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