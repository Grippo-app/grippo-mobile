package com.grippo.network.mapper.training

import com.grippo.database.entity.IterationEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.IterationResponse

public fun List<IterationResponse>.toEntities(): List<IterationEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun IterationResponse.toEntityOrNull(): IterationEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "IterationResponse.id is null"
    } ?: return null

    val entityExerciseId = AppLogger.checkOrLog(exerciseId) {
        "IterationResponse.exerciseId is null"
    } ?: return null

    val entityWeight = AppLogger.checkOrLog(weight) {
        "IterationResponse.weight is null"
    } ?: return null

    val entityRepetitions = AppLogger.checkOrLog(repetitions) {
        "IterationResponse.repetitions is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "IterationResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "IterationResponse.updatedAt is null"
    } ?: return null

    return IterationEntity(
        id = entityId,
        exerciseId = entityExerciseId,
        weight = entityWeight,
        repetitions = entityRepetitions,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}