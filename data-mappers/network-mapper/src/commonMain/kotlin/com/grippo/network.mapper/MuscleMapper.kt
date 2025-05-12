package com.grippo.network.mapper

import com.grippo.database.entity.MuscleEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.MuscleDto

public fun List<MuscleDto>.toEntities(): List<MuscleEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun MuscleDto.toEntityOrNull(): MuscleEntity? {
    val entityId = AppLogger.mapping(id, { "MuscleDto.id is null" }) ?: return null
    val entityMuscleGroupId = AppLogger.mapping(muscleGroupId, { "MuscleDto.muscleGroupId is null" }) ?: return null
    val entityName = AppLogger.mapping(name, { "MuscleDto.name is null" }) ?: return null
    val entityType = AppLogger.mapping(type, { "MuscleDto.type is null" }) ?: return null
    val entityCreatedAt = AppLogger.mapping(createdAt, { "MuscleDto.createdAt is null" }) ?: return null
    val entityUpdatedAt = AppLogger.mapping(updatedAt, { "MuscleDto.updatedAt is null" }) ?: return null

    return MuscleEntity(
        id = entityId,
        muscleGroupId = entityMuscleGroupId,
        name = entityName,
        type = entityType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}