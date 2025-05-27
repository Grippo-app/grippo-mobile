package com.grippo.network.mapper.training

import com.grippo.database.entity.TrainingEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.training.TrainingResponse

public fun List<TrainingResponse>.toEntities(): List<TrainingEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun TrainingResponse.toEntityOrNull(): TrainingEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "TrainingResponse.id is null"
    } ?: return null

    val entityDuration = AppLogger.checkOrLog(duration) {
        "TrainingResponse.duration is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "TrainingResponse.createdAt is null"
    } ?: return null

    val entityVolume = AppLogger.checkOrLog(volume) {
        "TrainingResponse.volume is null"
    } ?: return null

    val entityRepetitions = AppLogger.checkOrLog(repetitions) {
        "TrainingResponse.repetitions is null"
    } ?: return null

    val entityIntensity = AppLogger.checkOrLog(intensity) {
        "TrainingResponse.intensity is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "TrainingResponse.updatedAt is null"
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