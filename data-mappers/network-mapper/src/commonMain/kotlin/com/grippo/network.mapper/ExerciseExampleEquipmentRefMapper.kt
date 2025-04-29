package com.grippo.network.mapper

import com.grippo.database.entity.ExerciseEquipmentEntity
import com.grippo.network.dto.ExerciseExampleEquipmentRefDto

public fun List<ExerciseExampleEquipmentRefDto>.toEntities(): List<ExerciseEquipmentEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleEquipmentRefDto.toEntityOrNull(): ExerciseEquipmentEntity? {
    return ExerciseEquipmentEntity(
        id = id ?: return null,
        equipmentId = equipmentId ?: return null,
        exerciseExampleId = exerciseExampleId ?: return null,
        createdAt = createdAt ?: return null,
        updatedAt = updatedAt ?: return null,
    )
}