package com.grippo.network.mapper.exercise.example

import com.grippo.database.entity.ExerciseExampleEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.exercise.example.ExerciseExampleResponse

public fun List<ExerciseExampleResponse>.toEntities(): List<ExerciseExampleEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleResponse.toEntityOrNull(): ExerciseExampleEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "ExerciseExampleResponse.id is null"
    } ?: return null

    val entityName = AppLogger.Mapping.log(name) {
        "ExerciseExampleResponse.name is null"
    } ?: return null

    val entityDescription = AppLogger.Mapping.log(description) {
        "ExerciseExampleResponse.description is null"
    } ?: return null

    val entityCreatedAt = AppLogger.Mapping.log(createdAt) {
        "ExerciseExampleResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) {
        "ExerciseExampleResponse.updatedAt is null"
    } ?: return null

    val entityForceType = AppLogger.Mapping.log(forceType) {
        "ExerciseExampleResponse.forceType is null"
    } ?: return null

    val entityWeightType = AppLogger.Mapping.log(weightType) {
        "ExerciseExampleResponse.weightType is null"
    } ?: return null

    val entityCategory = AppLogger.Mapping.log(category) {
        "ExerciseExampleResponse.category is null"
    } ?: return null

    val entityExperience = AppLogger.Mapping.log(experience) {
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