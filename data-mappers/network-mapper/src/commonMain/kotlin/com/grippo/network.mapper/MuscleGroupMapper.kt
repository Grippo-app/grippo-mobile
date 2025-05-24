package com.grippo.network.mapper

import com.grippo.database.entity.MuscleGroupEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.MuscleGroupResponse

public fun List<MuscleGroupResponse>.toEntities(): List<MuscleGroupEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun MuscleGroupResponse.toEntityOrNull(): MuscleGroupEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "MuscleGroupDto.id is null"
    } ?: return null

    val entityName = AppLogger.checkOrLog(name) {
        "MuscleGroupDto.name is null"
    } ?: return null

    val entityType = AppLogger.checkOrLog(type) {
        "MuscleGroupDto.type is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "MuscleGroupDto.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "MuscleGroupDto.updatedAt is null"
    } ?: return null

    return MuscleGroupEntity(
        id = entityId,
        name = entityName,
        type = entityType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}