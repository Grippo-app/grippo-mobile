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
    val entityId = AppLogger.mapping(id, { "ExerciseDto.id is null" }) ?: return null
    val entityTrainingId = AppLogger.mapping(trainingId, { "ExerciseDto.trainingId is null" }) ?: return null
    val entityName = AppLogger.mapping(name, { "ExerciseDto.name is null" }) ?: return null
    val entityVolume = AppLogger.mapping(volume?.toFloat(), { "ExerciseDto.volume is null" }) ?: return null
    val entityRepetitions = AppLogger.mapping(repetitions, { "ExerciseDto.repetitions is null" }) ?: return null
    val entityIntensity = AppLogger.mapping(intensity?.toFloat(), { "ExerciseDto.intensity is null" }) ?: return null
    val entityCreatedAt = AppLogger.mapping(createdAt, { "ExerciseDto.createdAt is null" }) ?: return null
    val entityUpdatedAt = AppLogger.mapping(updatedAt, { "ExerciseDto.updatedAt is null" }) ?: return null

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