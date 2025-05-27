package com.grippo.network.mapper.exercise.example

import com.grippo.database.entity.ExerciseExampleBundleEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.exercise.example.ExerciseExampleBundleResponse

public fun List<ExerciseExampleBundleResponse>.toEntities(): List<ExerciseExampleBundleEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleBundleResponse.toEntityOrNull(): ExerciseExampleBundleEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "ExerciseExampleBundleResponse.id is null"
    } ?: return null

    val entityExerciseExampleId = AppLogger.checkOrLog(exerciseExampleId) {
        "ExerciseExampleBundleResponse.exerciseExampleId is null"
    } ?: return null

    val entityMuscleId = AppLogger.checkOrLog(muscleId) {
        "ExerciseExampleBundleResponse.muscleId is null"
    } ?: return null

    val entityPercentage = AppLogger.checkOrLog(percentage) {
        "ExerciseExampleBundleResponse.percentage is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "ExerciseExampleBundleResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "ExerciseExampleBundleResponse.updatedAt is null"
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