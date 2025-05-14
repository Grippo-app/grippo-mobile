package com.grippo.network.mapper

import com.grippo.database.entity.ExerciseExampleBundleEntity
import com.grippo.database.entity.ExerciseExampleEntity
import com.grippo.database.entity.ExerciseExampleEquipmentEntity
import com.grippo.database.entity.ExerciseExampleTutorialEntity
import com.grippo.logger.AppLogger
import com.grippo.network.dto.ExerciseExampleDto

public fun List<ExerciseExampleDto>.toEntities(): List<ExerciseExampleEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleDto.toExerciseExampleBundles(): List<ExerciseExampleBundleEntity> {
    return exerciseExampleBundles.mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleDto.toTutorials(): List<ExerciseExampleTutorialEntity> {
    return tutorials.mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleDto.toEquipmentRefs(): List<ExerciseExampleEquipmentEntity> {
    return equipmentRefs.mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleDto.toEntityOrNull(): ExerciseExampleEntity? {
    val entityId = AppLogger.mapping(id, { "ExerciseExampleDto.id is null" }) ?: return null
    val entityName = AppLogger.mapping(name, { "ExerciseExampleDto.name is null" }) ?: return null
    val entityCreatedAt =
        AppLogger.mapping(createdAt, { "ExerciseExampleDto.createdAt is null" }) ?: return null
    val entityUpdatedAt =
        AppLogger.mapping(updatedAt, { "ExerciseExampleDto.updatedAt is null" }) ?: return null
    val entityForceType =
        AppLogger.mapping(forceType, { "ExerciseExampleDto.forceType is null" }) ?: return null
    val entityWeightType =
        AppLogger.mapping(weightType, { "ExerciseExampleDto.weightType is null" }) ?: return null
    val entityCategory =
        AppLogger.mapping(category, { "ExerciseExampleDto.category is null" }) ?: return null
    val entityExperience =
        AppLogger.mapping(experience, { "ExerciseExampleDto.experience is null" }) ?: return null

    return ExerciseExampleEntity(
        id = entityId,
        name = entityName,
        description = description,
        createdAt = entityCreatedAt,
        updatedAt = entityUpdatedAt,
        forceType = entityForceType,
        weightType = entityWeightType,
        category = entityCategory,
        experience = entityExperience,
        imageUrl = imageUrl,
    )
}