package com.grippo.dto.entity.muscles

import com.grippo.toolkit.logger.AppLogger

public fun List<com.grippo.services.backend.dto.muscle.MuscleGroupResponse>.toEntities(): List<com.grippo.services.database.entity.MuscleGroupEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun com.grippo.services.backend.dto.muscle.MuscleGroupResponse.toEntityOrNull(): com.grippo.services.database.entity.MuscleGroupEntity? {
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

    return _root_ide_package_.com.grippo.services.database.entity.MuscleGroupEntity(
        id = entityId,
        name = entityName,
        type = entityType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}