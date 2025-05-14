package com.grippo.network.mapper

import com.grippo.database.entity.ExerciseEntity
import com.grippo.database.entity.IterationEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.ExerciseDto

public fun ExerciseDto.toIterations(): List<IterationEntity> {
    return iterations.mapNotNull { it.toEntityOrNull() }
}

public fun List<ExerciseDto>.toEntities(): List<ExerciseEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseDto.toEntityOrNull(): ExerciseEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "ExerciseDto.id is null"
    } ?: return null

    val entityTrainingId = AppLogger.checkOrLog(trainingId) {
        "ExerciseDto.trainingId is null"
    } ?: return null

    val entityName = AppLogger.checkOrLog(name) {
        "ExerciseDto.name is null"
    } ?: return null

    val entityVolume = AppLogger.checkOrLog(volume) {
        "ExerciseDto.volume is null"
    } ?: return null

    val entityRepetitions = AppLogger.checkOrLog(repetitions) {
        "ExerciseDto.repetitions is null"
    } ?: return null

    val entityIntensity = AppLogger.checkOrLog(intensity) {
        "ExerciseDto.intensity is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "ExerciseDto.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "ExerciseDto.updatedAt is null"
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