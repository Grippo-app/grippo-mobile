package com.grippo.network.mapper

import com.grippo.database.entity.ExerciseEquipmentEntity
import com.grippo.database.entity.ExerciseExampleBundleEntity
import com.grippo.database.entity.ExerciseExampleEntity
import com.grippo.database.entity.ExerciseTutorialEntity
import com.grippo.network.dto.ExerciseExampleDto

public fun List<ExerciseExampleDto>.toEntities(): List<ExerciseExampleEntity> {
    return mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleDto.toExerciseExampleBundles(): List<ExerciseExampleBundleEntity> {
    return exerciseExampleBundles.mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleDto.toTutorials(): List<ExerciseTutorialEntity> {
    return tutorials.mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleDto.toEquipmentRefs(): List<ExerciseEquipmentEntity> {
    return equipmentRefs.mapNotNull { it.toEntityOrNull() }
}

public fun ExerciseExampleDto.toEntityOrNull(): ExerciseExampleEntity? {
    return ExerciseExampleEntity(
        id = id ?: return null,
        name = name ?: return null,
        description = description,
        createdAt = createdAt ?: return null,
        updatedAt = updatedAt ?: return null,
        forceType = forceType ?: return null,
        weightType = weightType ?: return null,
        category = category ?: return null,
        experience = experience ?: return null,
        imageUrl = imageUrl,
    )
}