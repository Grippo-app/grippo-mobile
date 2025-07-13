package com.grippo.network.mapper.exercise.example

import com.grippo.database.entity.ExerciseExampleTutorialEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.exercise.example.TutorialResponse

public fun List<TutorialResponse>.toEntities(): List<ExerciseExampleTutorialEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun TutorialResponse.toEntityOrNull(): ExerciseExampleTutorialEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "TutorialResponse.id is null"
    } ?: return null

    val entityExerciseExampleId = AppLogger.Mapping.log(exerciseExampleId) {
        "TutorialResponse.exerciseExampleId is null"
    } ?: return null

    val entityTitle = AppLogger.Mapping.log(title) {
        "TutorialResponse.title is null"
    } ?: return null

    val entityLanguage = AppLogger.Mapping.log(language) {
        "TutorialResponse.language is null"
    } ?: return null

    val entityValue = AppLogger.Mapping.log(value) {
        "TutorialResponse.value is null"
    } ?: return null

    val entityResourceType = AppLogger.Mapping.log(resourceType) {
        "TutorialResponse.resourceType is null"
    } ?: return null

    val entityCreatedAt = AppLogger.Mapping.log(createdAt) {
        "TutorialResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) {
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