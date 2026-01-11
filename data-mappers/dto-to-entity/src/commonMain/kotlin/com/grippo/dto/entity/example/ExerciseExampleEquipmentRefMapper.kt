package com.grippo.dto.entity.example

import com.grippo.services.backend.dto.exercise.example.ExerciseExampleEquipmentRefResponse
import com.grippo.services.database.entity.ExerciseExampleEquipmentEntity
import com.grippo.toolkit.logger.AppLogger

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