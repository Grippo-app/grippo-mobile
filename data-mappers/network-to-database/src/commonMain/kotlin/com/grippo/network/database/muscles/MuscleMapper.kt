package com.grippo.network.database.muscles

import com.grippo.backend.dto.muscle.MuscleResponse
import com.grippo.database.entity.MuscleEntity
import com.grippo.logger.AppLogger

public fun List<MuscleResponse>.toEntities(): List<MuscleEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun MuscleResponse.toEntityOrNull(): MuscleEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "MuscleResponse.id is null"
    } ?: return null

    val entityMuscleGroupId = AppLogger.Mapping.log(muscleGroupId) {
        "MuscleResponse.muscleGroupId is null"
    } ?: return null

    val entityName = AppLogger.Mapping.log(name) {
        "MuscleResponse.name is null"
    } ?: return null

    val entityType = AppLogger.Mapping.log(type) {
        "MuscleResponse.type is null"
    } ?: return null

    val entityCreatedAt = AppLogger.Mapping.log(createdAt) {
        "MuscleResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) {
        "MuscleResponse.updatedAt is null"
    } ?: return null

    val entityRecoveryTimeHours = AppLogger.Mapping.log(recoveryTimeHours) {
        "MuscleResponse.recoveryTimeHours is null"
    } ?: return null

    return MuscleEntity(
        id = entityId,
        muscleGroupId = entityMuscleGroupId,
        name = entityName,
        type = entityType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
        recoveryTimeHours = entityRecoveryTimeHours
    )
}