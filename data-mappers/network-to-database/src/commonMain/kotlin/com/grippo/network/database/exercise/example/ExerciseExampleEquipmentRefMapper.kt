package com.grippo.network.database.exercise.example

import com.grippo.database.entity.ExerciseExampleEquipmentEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.exercise.example.ExerciseExampleEquipmentRefResponse

public fun List<ExerciseExampleEquipmentRefResponse>.toEntities(): List<ExerciseExampleEquipmentEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleEquipmentRefResponse.toEntityOrNull(): ExerciseExampleEquipmentEntity? {
    val entityId = AppLogger.Mapping.log(id) {
        "ExerciseExampleEquipmentRefResponse.id is null"
    } ?: return null

    val entityEquipmentId = AppLogger.Mapping.log(equipmentId) {
        "ExerciseExampleEquipmentRefResponse.equipmentId is null"
    } ?: return null

    val entityExerciseExampleId = AppLogger.Mapping.log(exerciseExampleId) {
        "ExerciseExampleEquipmentRefResponse.exerciseExampleId is null"
    } ?: return null

    val entityCreatedAt = AppLogger.Mapping.log(createdAt) {
        "ExerciseExampleEquipmentRefResponse.createdAt is null"
    } ?: return null

    val entityUpdatedAt = AppLogger.Mapping.log(updatedAt) {
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