package com.grippo.network.mapper.muscles

import com.grippo.database.entity.MuscleGroupEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.muscle.MuscleGroupResponse

public fun List<MuscleGroupResponse>.toEntities(): List<MuscleGroupEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun MuscleGroupResponse.toEntityOrNull(): MuscleGroupEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "MuscleGroupResponse.id is null"
    } ?: return null

    val entityName = AppLogger.checkOrLog(name) {
        "MuscleGroupResponse.name is null"
    } ?: return null

    val entityType = AppLogger.checkOrLog(type) {
        "MuscleGroupResponse.type is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "MuscleGroupResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "MuscleGroupResponse.updatedAt is null"
    } ?: return null

    return MuscleGroupEntity(
        id = entityId,
        name = entityName,
        type = entityType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}