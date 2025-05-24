package com.grippo.network.mapper

import com.grippo.database.entity.ExerciseExampleEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.ExerciseExampleResponse

public fun ExerciseExampleResponse.toExerciseExample(): ExerciseExampleEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "ExerciseExampleDto.id is null"
    } ?: return null

    val entityName = AppLogger.checkOrLog(name) {
        "ExerciseExampleDto.name is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "ExerciseExampleDto.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "ExerciseExampleDto.updatedAt is null"
    } ?: return null

    val entityForceType = AppLogger.checkOrLog(forceType) {
        "ExerciseExampleDto.forceType is null"
    } ?: return null

    val entityWeightType = AppLogger.checkOrLog(weightType) {
        "ExerciseExampleDto.weightType is null"
    } ?: return null

    val entityCategory = AppLogger.checkOrLog(category) {
        "ExerciseExampleDto.category is null"
    } ?: return null

    val entityExperience = AppLogger.checkOrLog(experience) {
        "ExerciseExampleDto.experience is null"
    } ?: return null

    return ExerciseExampleEntity(
        id = entityId,
        name = entityName,
        description = description,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
        forceType = entityForceType,
        weightType = entityWeightType,
        category = entityCategory,
        experience = entityExperience,
        imageUrl = imageUrl,
    )
}