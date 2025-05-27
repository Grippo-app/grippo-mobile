package com.grippo.network.mapper.muscles

import com.grippo.database.entity.MuscleEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.muscle.MuscleResponse

public fun List<MuscleResponse>.toEntities(): List<MuscleEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun MuscleResponse.toEntityOrNull(): MuscleEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "MuscleResponse.id is null"
    } ?: return null

    val entityMuscleGroupId = AppLogger.checkOrLog(muscleGroupId) {
        "MuscleResponse.muscleGroupId is null"
    } ?: return null

    val entityName = AppLogger.checkOrLog(name) {
        "MuscleResponse.name is null"
    } ?: return null

    val entityType = AppLogger.checkOrLog(type) {
        "MuscleResponse.type is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "MuscleResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "MuscleResponse.updatedAt is null"
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