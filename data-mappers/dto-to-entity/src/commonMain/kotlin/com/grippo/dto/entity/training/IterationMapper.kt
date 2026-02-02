package com.grippo.dto.entity.training

import com.grippo.services.backend.dto.training.IterationResponse
import com.grippo.services.database.entity.IterationEntity
import com.grippo.toolkit.logger.AppLogger

public fun List<IterationResponse>.toEntities(): List<IterationEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun IterationResponse.toEntityOrNull(): IterationEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "IterationResponse.id is null"
    } ?: return null

    val entityExerciseId = AppLogger.Mapping.log(exerciseId) {
        "IterationResponse.exerciseId is null"
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

    return IterationEntity(
        id = entityId,
        exerciseId = entityExerciseId,
        externalWeight = externalWeight,
        extraWeight = extraWeight,
        assistWeight = assistWeight,
        bodyWeight = bodyWeight,
        repetitions = entityRepetitions,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}