package com.grippo.dto.entity.training

import com.grippo.toolkit.logger.AppLogger

public fun List<com.grippo.services.backend.dto.training.IterationResponse>.toEntities(): List<com.grippo.services.database.entity.IterationEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun com.grippo.services.backend.dto.training.IterationResponse.toEntityOrNull(): com.grippo.services.database.entity.IterationEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "IterationResponse.id is null"
    } ?: return null

    val entityExerciseId = AppLogger.Mapping.log(exerciseId) {
        "IterationResponse.exerciseId is null"
    } ?: return null

    val entityWeight = AppLogger.Mapping.log(weight) {
        "IterationResponse.weight is null"
    } ?: return null

    val entityRepetitions = AppLogger.Mapping.log(repetitions) {
        "IterationResponse.repetitions is null"
    } ?: return null

    val entityCreatedAt = AppLogger.Mapping.log(createdAt) {
        "IterationResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) {
        "IterationResponse.updatedAt is null"
    } ?: return null

    return _root_ide_package_.com.grippo.services.database.entity.IterationEntity(
        id = entityId,
        exerciseId = entityExerciseId,
        volume = entityWeight,
        repetitions = entityRepetitions,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}