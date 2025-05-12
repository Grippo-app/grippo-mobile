package com.grippo.network.mapper

import com.grippo.database.entity.ExerciseExampleBundleEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.ExerciseExampleBundleDto

public fun List<ExerciseExampleBundleDto>.toEntities(): List<ExerciseExampleBundleEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleBundleDto.toEntityOrNull(): ExerciseExampleBundleEntity? {
    val entityId = AppLogger.mapping(id, { "ExerciseExampleBundleDto.id is null" }) ?: return null
    val entityExerciseExampleId = AppLogger.mapping(exerciseExampleId, { "ExerciseExampleBundleDto.exerciseExampleId is null" }) ?: return null
    val entityMuscleId = AppLogger.mapping(muscleId, { "ExerciseExampleBundleDto.muscleId is null" }) ?: return null
    val entityPercentage = AppLogger.mapping(percentage, { "ExerciseExampleBundleDto.percentage is null" }) ?: return null
    val entityCreatedAt = AppLogger.mapping(createdAt, { "ExerciseExampleBundleDto.createdAt is null" }) ?: return null
    val entityUpdatedAt = AppLogger.mapping(updatedAt, { "ExerciseExampleBundleDto.updatedAt is null" }) ?: return null

    return ExerciseExampleBundleEntity(
        id = entityId,
        exerciseExampleId = entityExerciseExampleId,
        muscleId = entityMuscleId,
        percentage = entityPercentage,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}