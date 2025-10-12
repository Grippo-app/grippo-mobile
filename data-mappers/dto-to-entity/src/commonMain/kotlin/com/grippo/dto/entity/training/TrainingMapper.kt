package com.grippo.dto.entity.training

import com.grippo.backend.dto.training.TrainingResponse
import com.grippo.database.entity.TrainingEntity
import com.grippo.logger.AppLogger

public fun List<TrainingResponse>.toEntities(): List<TrainingEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun TrainingResponse.toEntityOrNull(): TrainingEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "TrainingResponse.id is null"
    } ?: return null

    val entityDuration = AppLogger.Mapping.log(duration) {
        "TrainingResponse.duration is null"
    } ?: return null

    val entityCreatedAt = AppLogger.Mapping.log(createdAt) {
        "TrainingResponse.createdAt is null"
    } ?: return null

    val entityVolume = AppLogger.Mapping.log(volume) {
        "TrainingResponse.volume is null"
    } ?: return null

    val entityRepetitions = AppLogger.Mapping.log(repetitions) {
        "TrainingResponse.repetitions is null"
    } ?: return null

    val entityIntensity = AppLogger.Mapping.log(intensity) {
        "TrainingResponse.intensity is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) {
        "TrainingResponse.updatedAt is null"
    } ?: return null

    val entityUserId = AppLogger.Mapping.log(userId) {
        "TrainingResponse.userId is null"
    } ?: return null

    return TrainingEntity(
        id = entityId,
        duration = entityDuration,
        createdAt = entityCreatedAt,
        volume = entityVolume,
        repetitions = entityRepetitions,
        intensity = entityIntensity,
        updatedAt = entityUpdatedAt,
        userId = entityUserId
    )
}