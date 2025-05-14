package com.grippo.network.mapper

import com.grippo.database.entity.ExerciseExampleBundleEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.ExerciseExampleBundleDto

public fun List<ExerciseExampleBundleDto>.toEntities(): List<ExerciseExampleBundleEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleBundleDto.toEntityOrNull(): ExerciseExampleBundleEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "ExerciseExampleBundleDto.id is null"
    } ?: return null

    val entityExerciseExampleId = AppLogger.checkOrLog(exerciseExampleId) {
        "ExerciseExampleBundleDto.exerciseExampleId is null"
    } ?: return null

    val entityMuscleId = AppLogger.checkOrLog(muscleId) {
        "ExerciseExampleBundleDto.muscleId is null"
    } ?: return null

    val entityPercentage = AppLogger.checkOrLog(percentage) {
        "ExerciseExampleBundleDto.percentage is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "ExerciseExampleBundleDto.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "ExerciseExampleBundleDto.updatedAt is null"
    } ?: return null

    return ExerciseExampleBundleEntity(
        id = entityId,
        exerciseExampleId = entityExerciseExampleId,
        muscleId = entityMuscleId,
        percentage = entityPercentage,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}