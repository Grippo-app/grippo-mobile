package com.grippo.network.mapper

import com.grippo.database.entity.MuscleEntity
import com.grippo.database.entity.MuscleGroupEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.MuscleGroupResponse

public fun MuscleGroupResponse.toMuscles(): List<MuscleEntity> {
    return muscles.orEmpty().mapNotNull { it.toEntityOrNull() }
}

public fun MuscleGroupResponse.toEntityOrNull(): MuscleGroupEntity? {
    val entityId = AppLogger.mapping(id, { "MuscleGroupDto.id is null" }) ?: return null
    val entityName = AppLogger.mapping(name, { "MuscleGroupDto.name is null" }) ?: return null
    val entityType = AppLogger.mapping(type, { "MuscleGroupDto.type is null" }) ?: return null
    val entityCreatedAt =
        AppLogger.mapping(createdAt, { "MuscleGroupDto.createdAt is null" }) ?: return null
    val entityUpdatedAt =
        AppLogger.mapping(updatedAt, { "MuscleGroupDto.updatedAt is null" }) ?: return null

    return MuscleGroupEntity(
        id = entityId,
        name = entityName,
        type = entityType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}