package com.grippo.network.mapper.training

import com.grippo.database.entity.ExerciseEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.ExerciseResponse

public fun List<ExerciseResponse>.toEntities(): List<ExerciseEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseResponse.toEntityOrNull(): ExerciseEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "ExerciseResponse.id is null"
    } ?: return null

    val entityTrainingId = AppLogger.checkOrLog(trainingId) {
        "ExerciseResponse.trainingId is null"
    } ?: return null

    val entityName = AppLogger.checkOrLog(name) {
        "ExerciseResponse.name is null"
    } ?: return null

    val entityVolume = AppLogger.checkOrLog(volume) {
        "ExerciseResponse.volume is null"
    } ?: return null

    val entityRepetitions = AppLogger.checkOrLog(repetitions) {
        "ExerciseResponse.repetitions is null"
    } ?: return null

    val entityIntensity = AppLogger.checkOrLog(intensity) {
        "ExerciseResponse.intensity is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "ExerciseResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "ExerciseResponse.updatedAt is null"
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