package com.grippo.network.mapper.exercise.example

import com.grippo.database.entity.ExerciseExampleEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.exercise.example.ExerciseExampleResponse

public fun List<ExerciseExampleResponse>.toEntities(): List<ExerciseExampleEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleResponse.toEntityOrNull(): ExerciseExampleEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "ExerciseExampleResponse.id is null"
    } ?: return null

    val entityName = AppLogger.checkOrLog(name) {
        "ExerciseExampleResponse.name is null"
    } ?: return null

    val entityDescription = AppLogger.checkOrLog(description) {
        "ExerciseExampleResponse.description is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "ExerciseExampleResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "ExerciseExampleResponse.updatedAt is null"
    } ?: return null

    val entityForceType = AppLogger.checkOrLog(forceType) {
        "ExerciseExampleResponse.forceType is null"
    } ?: return null

    val entityWeightType = AppLogger.checkOrLog(weightType) {
        "ExerciseExampleResponse.weightType is null"
    } ?: return null

    val entityCategory = AppLogger.checkOrLog(category) {
        "ExerciseExampleResponse.category is null"
    } ?: return null

    val entityExperience = AppLogger.checkOrLog(experience) {
        "ExerciseExampleResponse.experience is null"
    } ?: return null

    return ExerciseExampleEntity(
        id = entityId,
        name = entityName,
        description = entityDescription,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
        forceType = entityForceType,
        weightType = entityWeightType,
        category = entityCategory,
        experience = entityExperience,
        imageUrl = imageUrl,
    )
}