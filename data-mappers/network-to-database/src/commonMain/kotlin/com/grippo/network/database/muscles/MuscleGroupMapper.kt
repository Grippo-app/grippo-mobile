package com.grippo.network.database.muscles

import com.grippo.backend.dto.muscle.MuscleGroupResponse
import com.grippo.database.entity.MuscleGroupEntity
import com.grippo.logger.AppLogger

public fun List<MuscleGroupResponse>.toEntities(): List<MuscleGroupEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun MuscleGroupResponse.toEntityOrNull(): MuscleGroupEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "MuscleGroupResponse.id is null"
    } ?: return null

    val entityName = AppLogger.Mapping.log(name) {
        "MuscleGroupResponse.name is null"
    } ?: return null

    val entityType = AppLogger.Mapping.log(type) {
        "MuscleGroupResponse.type is null"
    } ?: return null

    val entityCreatedAt = AppLogger.Mapping.log(createdAt) {
        "MuscleGroupResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) {
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