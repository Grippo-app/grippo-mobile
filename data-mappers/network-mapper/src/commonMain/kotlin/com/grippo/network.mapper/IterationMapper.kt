package com.grippo.network.mapper

import com.grippo.database.entity.IterationEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.IterationDto

public fun List<IterationDto>.toEntities(): List<IterationEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun IterationDto.toEntityOrNull(): IterationEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "IterationDto.id is null"
    } ?: return null

    val entityExerciseId = AppLogger.checkOrLog(exerciseId) {
        "IterationDto.exerciseId is null"
    } ?: return null

    val entityWeight = AppLogger.checkOrLog(weight) {
        "IterationDto.weight is null"
    } ?: return null

    val entityRepetitions = AppLogger.checkOrLog(repetitions) {
        "IterationDto.repetitions is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "IterationDto.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "IterationDto.updatedAt is null"
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