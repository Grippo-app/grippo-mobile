package com.grippo.network.mapper

import com.grippo.database.entity.ExerciseEquipmentEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.ExerciseExampleEquipmentRefDto

public fun List<ExerciseExampleEquipmentRefDto>.toEntities(): List<ExerciseEquipmentEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleEquipmentRefDto.toEntityOrNull(): ExerciseEquipmentEntity? {
    val entityId = AppLogger.mapping(id, { "ExerciseExampleEquipmentRefDto.id is null" }) ?: return null
    val entityEquipmentId = AppLogger.mapping(equipmentId, { "ExerciseExampleEquipmentRefDto.equipmentId is null" }) ?: return null
    val entityExerciseExampleId = AppLogger.mapping(exerciseExampleId, { "ExerciseExampleEquipmentRefDto.exerciseExampleId is null" }) ?: return null
    val entityCreatedAt = AppLogger.mapping(createdAt, { "ExerciseExampleEquipmentRefDto.createdAt is null" }) ?: return null
    val entityUpdatedAt = AppLogger.mapping(updatedAt, { "ExerciseExampleEquipmentRefDto.updatedAt is null" }) ?: return null

    return ExerciseEquipmentEntity(
        id = entityId,
        equipmentId = entityEquipmentId,
        exerciseExampleId = entityExerciseExampleId,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}