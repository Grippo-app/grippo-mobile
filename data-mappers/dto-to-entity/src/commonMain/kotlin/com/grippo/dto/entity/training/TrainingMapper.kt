package com.grippo.dto.entity.training

import com.grippo.toolkit.logger.AppLogger

public fun List<com.grippo.services.backend.dto.training.TrainingResponse>.toEntities(): List<com.grippo.services.database.entity.TrainingEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun com.grippo.services.backend.dto.training.TrainingResponse.toEntityOrNull(): com.grippo.services.database.entity.TrainingEntity? {
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

    val entityProfileId = AppLogger.Mapping.log(profileId ?: userId) {
        "TrainingResponse.profileId is null"
    } ?: return null

    return _root_ide_package_.com.grippo.services.database.entity.TrainingEntity(
        id = entityId,
        duration = entityDuration,
        createdAt = entityCreatedAt,
        volume = entityVolume,
        repetitions = entityRepetitions,
        intensity = entityIntensity,
        updatedAt = entityUpdatedAt,
        profileId = entityProfileId
    )
}
