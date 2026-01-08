package com.grippo.dto.entity.example

import com.grippo.toolkit.logger.AppLogger

public fun List<com.grippo.services.backend.dto.exercise.example.ExerciseExampleEquipmentRefResponse>.toEntities(): List<com.grippo.services.database.entity.ExerciseExampleEquipmentEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun com.grippo.services.backend.dto.exercise.example.ExerciseExampleEquipmentRefResponse.toEntityOrNull(): com.grippo.services.database.entity.ExerciseExampleEquipmentEntity? {
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

    return _root_ide_package_.com.grippo.services.database.entity.ExerciseExampleEquipmentEntity(
        id = entityId,
        equipmentId = entityEquipmentId,
        exerciseExampleId = entityExerciseExampleId,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
    )
}