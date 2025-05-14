package com.grippo.network.mapper

import com.grippo.database.entity.ExerciseExampleEquipmentEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.ExerciseExampleEquipmentRefDto

public fun List<ExerciseExampleEquipmentRefDto>.toEntities(): List<ExerciseExampleEquipmentEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleEquipmentRefDto.toEntityOrNull(): ExerciseExampleEquipmentEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "ExerciseExampleEquipmentRefDto.id is null"
    } ?: return null

    val entityEquipmentId = AppLogger.checkOrLog(equipmentId) {
        "ExerciseExampleEquipmentRefDto.equipmentId is null"
    } ?: return null

    val entityExerciseExampleId = AppLogger.checkOrLog(exerciseExampleId) {
        "ExerciseExampleEquipmentRefDto.exerciseExampleId is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "ExerciseExampleEquipmentRefDto.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "ExerciseExampleEquipmentRefDto.updatedAt is null"
    } ?: return null

    return ExerciseExampleEquipmentEntity(
        id = entityId,
        equipmentId = entityEquipmentId,
        exerciseExampleId = entityExerciseExampleId,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}