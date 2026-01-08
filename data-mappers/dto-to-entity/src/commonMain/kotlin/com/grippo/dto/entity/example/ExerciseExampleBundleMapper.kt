package com.grippo.dto.entity.example

import com.grippo.toolkit.logger.AppLogger

public fun List<com.grippo.services.backend.dto.exercise.example.ExerciseExampleBundleResponse>.toEntities(): List<com.grippo.services.database.entity.ExerciseExampleBundleEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun com.grippo.services.backend.dto.exercise.example.ExerciseExampleBundleResponse.toEntityOrNull(): com.grippo.services.database.entity.ExerciseExampleBundleEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "ExerciseExampleBundleResponse.id is null"
    } ?: return null

    val entityExerciseExampleId = AppLogger.Mapping.log(exerciseExampleId) {
        "ExerciseExampleBundleResponse.exerciseExampleId is null"
    } ?: return null

    val entityMuscleId = AppLogger.Mapping.log(muscleId) {
        "ExerciseExampleBundleResponse.muscleId is null"
    } ?: return null

    val entityPercentage = AppLogger.Mapping.log(percentage) {
        "ExerciseExampleBundleResponse.percentage is null"
    } ?: return null

    val entityCreatedAt = AppLogger.Mapping.log(createdAt) {
        "ExerciseExampleBundleResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) {
        "ExerciseExampleBundleResponse.updatedAt is null"
    } ?: return null

    return _root_ide_package_.com.grippo.services.database.entity.ExerciseExampleBundleEntity(
        id = entityId,
        exerciseExampleId = entityExerciseExampleId,
        muscleId = entityMuscleId,
        percentage = entityPercentage,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}