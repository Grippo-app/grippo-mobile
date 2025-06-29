package com.grippo.network.mapper.exercise.example

import com.grippo.database.entity.ExerciseExampleEquipmentEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.exercise.example.ExerciseExampleEquipmentRefResponse

public fun List<ExerciseExampleEquipmentRefResponse>.toEntities(): List<ExerciseExampleEquipmentEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleEquipmentRefResponse.toEntityOrNull(): ExerciseExampleEquipmentEntity? {
    val entityId = AppLogger.checkOrLog(id) {
        "ExerciseExampleEquipmentRefResponse.id is null"
    } ?: return null

    val entityEquipmentId = AppLogger.checkOrLog(equipmentId) {
        "ExerciseExampleEquipmentRefResponse.equipmentId is null"
    } ?: return null

    val entityExerciseExampleId = AppLogger.checkOrLog(exerciseExampleId) {
        "ExerciseExampleEquipmentRefResponse.exerciseExampleId is null"
    } ?: return null

    val entityCreatedAt = AppLogger.checkOrLog(createdAt) {
        "ExerciseExampleEquipmentRefResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.checkOrLog(updatedAt) {
        "ExerciseExampleEquipmentRefResponse.updatedAt is null"
    } ?: return null

    return ExerciseExampleEquipmentEntity(
        id = entityId,
        equipmentId = entityEquipmentId,
        exerciseExampleId = entityExerciseExampleId,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}