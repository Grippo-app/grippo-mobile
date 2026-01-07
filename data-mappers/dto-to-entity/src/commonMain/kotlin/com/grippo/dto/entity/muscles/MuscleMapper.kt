package com.grippo.dto.entity.muscles

import com.grippo.backend.dto.muscle.MuscleResponse
import com.grippo.database.entity.MuscleEntity
import com.grippo.toolkit.logger.AppLogger

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

    val entityRecovery = AppLogger.Mapping.log(recovery) {
        "MuscleResponse.recovery is null"
    } ?: return null

    val entitySensitivity = AppLogger.Mapping.log(sensitivity) {
        "MuscleResponse.sensitivity is null"
    } ?: return null

    val entitySize = AppLogger.Mapping.log(size) {
        "MuscleResponse.size is null"
    } ?: return null

    return MuscleEntity(
        id = entityId,
        muscleGroupId = entityMuscleGroupId,
        name = entityName,
        type = entityType,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
        recovery = entityRecovery,
        sensitivity = entitySensitivity,
        size = entitySize
    )
}