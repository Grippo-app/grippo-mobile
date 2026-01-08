package com.grippo.dto.entity.example

import com.grippo.toolkit.logger.AppLogger

public fun List<com.grippo.services.backend.dto.exercise.example.GetExerciseExampleResponse>.toEntities(): List<com.grippo.services.database.entity.ExerciseExampleEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun com.grippo.services.backend.dto.exercise.example.GetExerciseExampleResponse.toEntityOrNull(): com.grippo.services.database.entity.ExerciseExampleEntity? {
    val entityId = AppLogger.Mapping.log(entity?.id) {
        "ExerciseExampleResponse.id is null"
    } ?: return null

    val entityName = AppLogger.Mapping.log(entity?.name) {
        "ExerciseExampleResponse.name is null"
    } ?: return null

    val entityDescription = AppLogger.Mapping.log(entity?.description) {
        "ExerciseExampleResponse.description is null"
    } ?: return null

    val entityCreatedAt = AppLogger.Mapping.log(entity?.createdAt) {
        "ExerciseExampleResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.Mapping.log(entity?.updatedAt) {
        "ExerciseExampleResponse.updatedAt is null"
    } ?: return null

    val entityForceType = AppLogger.Mapping.log(entity?.forceType) {
        "ExerciseExampleResponse.forceType is null"
    } ?: return null

    val entityWeightType = AppLogger.Mapping.log(entity?.weightType) {
        "ExerciseExampleResponse.weightType is null"
    } ?: return null

    val entityCategory = AppLogger.Mapping.log(entity?.category) {
        "ExerciseExampleResponse.category is null"
    } ?: return null

    val entityExperience = AppLogger.Mapping.log(entity?.experience) {
        "ExerciseExampleResponse.experience is null"
    } ?: return null

    val entityUsageCount = AppLogger.Mapping.log(usageCount) {
        "ExerciseExampleResponse.usageCount is null"
    } ?: return null

    return _root_ide_package_.com.grippo.services.database.entity.ExerciseExampleEntity(
        id = entityId,
        name = entityName,
        description = entityDescription,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
        forceType = entityForceType,
        weightType = entityWeightType,
        category = entityCategory,
        experience = entityExperience,
        imageUrl = entity?.imageUrl,
        usageCount = entityUsageCount,
        lastUsed = lastUsed
    )
}