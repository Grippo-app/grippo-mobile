package com.grippo.dto.entity.training

import com.grippo.backend.dto.training.ExerciseResponse
import com.grippo.database.entity.ExerciseEntity
import com.grippo.logger.AppLogger

public fun List<ExerciseResponse>.toEntities(): List<ExerciseEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseResponse.toEntityOrNull(): ExerciseEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "ExerciseResponse.id is null"
    } ?: return null

    val entityTrainingId = AppLogger.Mapping.log(trainingId) {
        "ExerciseResponse.trainingId is null"
    } ?: return null

    val entityName = AppLogger.Mapping.log(name) {
        "ExerciseResponse.name is null"
    } ?: return null

    val entityVolume = AppLogger.Mapping.log(volume) {
        "ExerciseResponse.volume is null"
    } ?: return null

    val entityRepetitions = AppLogger.Mapping.log(repetitions) {
        "ExerciseResponse.repetitions is null"
    } ?: return null

    val entityIntensity = AppLogger.Mapping.log(intensity) {
        "ExerciseResponse.intensity is null"
    } ?: return null

    val entityCreatedAt = AppLogger.Mapping.log(createdAt) {
        "ExerciseResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) {
        "ExerciseResponse.updatedAt is null"
    } ?: return null

    val exerciseExampleId = AppLogger.Mapping.log(exerciseExampleId) {
        "ExerciseResponse.exerciseExampleId is null"
    } ?: return null

    return ExerciseEntity(
        id = entityId,
        trainingId = entityTrainingId,
        exerciseExampleId = exerciseExampleId,
        name = entityName,
        volume = entityVolume,
        repetitions = entityRepetitions,
        intensity = entityIntensity,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}