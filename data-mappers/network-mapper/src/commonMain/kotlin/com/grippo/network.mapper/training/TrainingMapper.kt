package com.grippo.network.mapper.training

import com.grippo.database.entity.TrainingEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.TrainingResponse

public fun List<TrainingResponse>.toEntities(): List<TrainingEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun TrainingResponse.toEntityOrNull(): TrainingEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "TrainingDto.id is null"
    } ?: return null

    val entityDuration = AppLogger.checkOrLog(duration) {
        "TrainingDto.duration is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "TrainingDto.createdAt is null"
    } ?: return null

    val entityVolume = AppLogger.checkOrLog(volume) {
        "TrainingDto.volume is null"
    } ?: return null

    val entityRepetitions = AppLogger.checkOrLog(repetitions) {
        "TrainingDto.repetitions is null"
    } ?: return null

    val entityIntensity = AppLogger.checkOrLog(intensity) {
        "TrainingDto.intensity is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "TrainingDto.updatedAt is null"
    } ?: return null

    return TrainingEntity(
        id = entityId,
        duration = entityDuration,
        createdAt = entityCreatedAt,
        volume = entityVolume,
        repetitions = entityRepetitions,
        intensity = entityIntensity,
        updatedAt = entityUpdatedAt,
    )
}