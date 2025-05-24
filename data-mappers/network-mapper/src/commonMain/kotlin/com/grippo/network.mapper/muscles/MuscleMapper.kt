package com.grippo.network.mapper.muscles

import com.grippo.database.entity.MuscleEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.MuscleResponse

public fun List<MuscleResponse>.toEntities(): List<MuscleEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun MuscleResponse.toEntityOrNull(): MuscleEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "MuscleDto.id is null"
    } ?: return null

    val entityMuscleGroupId = AppLogger.checkOrLog(muscleGroupId) {
        "MuscleDto.muscleGroupId is null"
    } ?: return null

    val entityName = AppLogger.checkOrLog(name) {
        "MuscleDto.name is null"
    } ?: return null

    val entityType = AppLogger.checkOrLog(type) {
        "MuscleDto.type is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "MuscleDto.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "MuscleDto.updatedAt is null"
    } ?: return null

    return MuscleEntity(
        id = entityId,
        muscleGroupId = entityMuscleGroupId,
        name = entityName,
        type = entityType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}